package com.sd.lib.downloader.executor

import com.sd.lib.downloader.DownloadRequest
import com.sd.lib.downloader.exception.DownloadExceptionHttp
import com.sd.lib.downloader.exception.DownloadExceptionHttpResponseCode
import com.sd.lib.downloader.logMsg
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap

interface DownloadExecutor {
  /**
   * 提交下载任务
   * @param request 下载请求
   * @param file    要保存的下载文件
   * @param updater 下载信息更新对象
   */
  @Throws(Throwable::class)
  fun submit(
    request: DownloadRequest,
    file: File,
    updater: Updater,
  )

  /**
   * 取消[url]下载任务
   */
  fun cancel(url: String)

  interface Updater {
    /**
     * 通知下载进度
     * @param total   总数量
     * @param current 已传输数量
     */
    fun notifyProgress(total: Long, current: Long)

    /**
     * 通知下载成功
     */
    fun notifySuccess()

    /**
     * 通知下载错误，如果[e]是[CancellationException]，表示取消下载
     */
    fun notifyError(e: Throwable)
  }

  companion object {
    @JvmOverloads
    @JvmStatic
    fun getDefault(
      /** 同时下载的任务数量 */
      limitedParallelism: Int = 3,
    ): DownloadExecutor {
      return DefaultDownloadExecutor(limitedParallelism)
    }
  }
}

private class DefaultDownloadExecutor(
  /** 同时下载的任务数量 */
  limitedParallelism: Int,
) : DownloadExecutor {
  private val _mapJob: MutableMap<String, Job> = ConcurrentHashMap()

  @OptIn(ExperimentalCoroutinesApi::class)
  private val _scope by lazy {
    val dispatcher = Dispatchers.IO.limitedParallelism(limitedParallelism)
    CoroutineScope(SupervisorJob() + dispatcher)
  }

  override fun submit(
    request: DownloadRequest,
    file: File,
    updater: DownloadExecutor.Updater,
  ) {
    val url = request.url
    _scope.launch(CoroutineExceptionHandler { _, _ -> }) {
      handleRequest(request = request, file = file, updater = updater)
    }.also { job ->
      _mapJob[url] = job
      logMsg { "executor $url submit size:${_mapJob.size}" }
      job.invokeOnCompletion { e ->
        _mapJob.remove(url)
        logMsg { "executor $url finish $e size:${_mapJob.size}" }
        if (e != null) {
          val cause = e.findCause()
          if (cause is IOException) {
            updater.notifyError(DownloadExceptionHttp(cause = cause))
          } else {
            updater.notifyError(cause)
          }
        } else {
          updater.notifySuccess()
        }
      }
    }
  }

  override fun cancel(url: String) {
    _mapJob[url]?.cancel()
  }

  private suspend fun handleRequest(
    request: DownloadRequest,
    file: File,
    updater: DownloadExecutor.Updater,
  ) {
    val length = file.length()
    val breakpoint = request.preferBreakpoint && length > 0

    if (breakpoint) {
      val httpRequest = newHttpRequest(request).apply {
        this.header("Range", "bytes=$length-")
      }

      logMsg { "executor ${request.url} breakpoint start" }
      val code = withTimeout(request.connectTimeout) { httpRequest.code() }
      logMsg { "executor ${request.url} breakpoint finish code:$code" }

      currentCoroutineContext().ensureActive()
      when (code) {
        HttpURLConnection.HTTP_PARTIAL -> {
          downloadBreakpoint(httpRequest = httpRequest, file = file, updater = updater)
          return
        }
        HttpURLConnection.HTTP_OK -> {
          downloadNormal(httpRequest = httpRequest, file = file, updater = updater)
          return
        }
        416 -> {
          // 不处理，回退到正常下载
        }
        else -> {
          throw DownloadExceptionHttpResponseCode(code)
        }
      }
    }

    val httpRequest = newHttpRequest(request)
    logMsg { "executor ${request.url} normal start" }
    val code = withTimeout(request.connectTimeout) { httpRequest.code() }
    logMsg { "executor ${request.url} normal finish code:$code" }

    currentCoroutineContext().ensureActive()
    if (code == HttpURLConnection.HTTP_OK) {
      downloadNormal(httpRequest = httpRequest, file = file, updater = updater)
    } else {
      throw DownloadExceptionHttpResponseCode(code)
    }
  }

  /** 正常下载 */
  private suspend fun downloadNormal(
    httpRequest: HttpRequest,
    file: File,
    updater: DownloadExecutor.Updater,
  ) {
    file.deleteRecursively()
    val total = httpRequest.contentLength().toLong()
    httpRequest.stream().use { input ->
      file.outputStream().use { output ->
        input.copyToOutput(
          output = { buffer, offset, length -> output.write(buffer, offset, length) },
          callback = { count -> updater.notifyProgress(total, count) },
        )
        output.flush()
        runCatching { output.fd.sync() }
          .onFailure { logMsg { "output.fd.sync() onFailure:${it.stackTraceToString()}" } }
      }
    }
  }

  /** 断点下载 */
  private suspend fun downloadBreakpoint(
    httpRequest: HttpRequest,
    file: File,
    updater: DownloadExecutor.Updater,
  ) {
    val length = file.length()
    val randomAccessFile = RandomAccessFile(file, "rw").apply {
      this.seek(length)
    }

    val total = httpRequest.contentLength() + length
    httpRequest.stream().use { input ->
      randomAccessFile.use { output ->
        input.copyToOutput(
          output = { buffer, offset, length -> output.write(buffer, offset, length) },
          callback = { count -> updater.notifyProgress(total, count + length) },
        )
        runCatching { output.fd.sync() }
          .onFailure { logMsg { "output.fd.sync() onFailure:${it.stackTraceToString()}" } }
      }
    }
  }
}

private fun newHttpRequest(downloadRequest: DownloadRequest): HttpRequest {
  return HttpRequest.get(downloadRequest.url)
    .connectTimeout(downloadRequest.connectTimeout.toInt())
    .readTimeout(downloadRequest.connectTimeout.toInt())
    .trustAllHosts()
    .trustAllCerts()
}

private fun Throwable.findCause(): Throwable {
  return when (val e = this) {
    is HttpRequest.HttpRequestException -> e.cause ?: e
    else -> e
  }
}

private suspend inline fun InputStream.copyToOutput(
  output: (buffer: ByteArray, offset: Int, length: Int) -> Unit,
  callback: (count: Long) -> Unit,
): Long {
  var bytesCopied: Long = 0
  val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

  currentCoroutineContext().ensureActive()
  var bytes = read(buffer)

  while (bytes >= 0) {
    currentCoroutineContext().ensureActive()
    output(buffer, 0, bytes)
    currentCoroutineContext().ensureActive()

    bytesCopied += bytes
    callback(bytesCopied)

    currentCoroutineContext().ensureActive()
    bytes = read(buffer)
  }

  currentCoroutineContext().ensureActive()
  return bytesCopied
}