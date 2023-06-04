package com.sd.lib.downloader.executor

import com.github.kevinsawicki.http.HttpRequest
import com.sd.lib.downloader.DownloadRequest
import com.sd.lib.downloader.exception.DownloadHttpExceptionResponseCode
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.util.Collections

class DefaultDownloadExecutor @JvmOverloads constructor(
    limitedParallelism: Int = 3,
    preferBreakpoint: Boolean = false,
) : IDownloadExecutor {

    private val _preferBreakpoint = preferBreakpoint
    private val _taskHolder: MutableMap<String, Job> = Collections.synchronizedMap(hashMapOf())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _scope by lazy {
        CoroutineScope(Dispatchers.IO.limitedParallelism(limitedParallelism))
    }

    private fun newHttpRequest(downloadRequest: DownloadRequest): HttpRequest {
        return HttpRequest.get(downloadRequest.url)
            .connectTimeout(15 * 1000)
            .readTimeout(15 * 1000)
            .trustAllHosts()
            .trustAllCerts()
    }

    override fun submit(request: DownloadRequest, file: File, updater: IDownloadExecutor.Updater) {
        val url = request.url
        _scope.launch(
            context = CoroutineExceptionHandler { _, _ -> },
            start = CoroutineStart.LAZY,
        ) {
            val length = file.length()
            val breakpoint = request.preferBreakpoint ?: _preferBreakpoint && length > 0

            if (breakpoint) {
                val httpRequest = newHttpRequest(request)
                val code = httpRequest.run {
                    this.header("Range", "bytes=$length-")
                    this.code()
                }

                ensureActive()
                if (code == HttpURLConnection.HTTP_PARTIAL) {
                    downloadBreakpoint(
                        httpRequest = httpRequest,
                        file = file,
                        updater = updater,
                    )
                    return@launch
                }
            }

            val httpRequest = newHttpRequest(request)
            val code = httpRequest.code()

            ensureActive()
            if (code == HttpURLConnection.HTTP_OK) {
                downloadNormal(
                    httpRequest = httpRequest,
                    file = file,
                    updater = updater,
                )
            } else {
                throw DownloadHttpExceptionResponseCode(code)
            }
        }.also { job ->
            _taskHolder[url] = job
            job.invokeOnCompletion { t ->
                _taskHolder.remove(url)
                if (t != null) {
                    updater.notifyError(t)
                } else {
                    updater.notifySuccess()
                }
            }
            job.start()
        }
    }

    override fun cancel(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        val job = _taskHolder[url] ?: return false
        job.cancel()
        return true
    }

    private suspend fun downloadNormal(
        httpRequest: HttpRequest,
        file: File,
        updater: IDownloadExecutor.Updater,
    ) {
        val total = httpRequest.contentLength().toLong()
        httpRequest.stream().use { input ->
            file.outputStream().use { output ->
                input.copyToOutput(
                    output = { buffer, offset, length ->
                        output.write(buffer, offset, length)
                    },
                    callback = { count ->
                        updater.notifyProgress(total, count)
                        false
                    },
                )
            }
        }
    }

    private suspend fun downloadBreakpoint(
        httpRequest: HttpRequest,
        file: File,
        updater: IDownloadExecutor.Updater,
    ) {
        val length = file.length()
        val randomAccessFile = RandomAccessFile(file, "rwd").apply {
            this.seek(length)
        }

        val total = httpRequest.contentLength() + length
        httpRequest.stream().use { input ->
            randomAccessFile.use { output ->
                input.copyToOutput(
                    output = { buffer, offset, length ->
                        output.write(buffer, offset, length)
                    },
                    callback = { count ->
                        updater.notifyProgress(total, count + length)
                        false
                    },
                )
            }
        }
    }
}

private suspend fun InputStream.copyToOutput(
    output: (buffer: ByteArray, offset: Int, length: Int) -> Unit,
    callback: ((count: Long) -> Boolean)? = null,
): Long {
    var bytesCopied: Long = 0
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

    currentCoroutineContext().ensureActive()
    var bytes = read(buffer)
    currentCoroutineContext().ensureActive()

    while (bytes >= 0) {
        output(buffer, 0, bytes)
        currentCoroutineContext().ensureActive()

        bytesCopied += bytes
        if (callback?.invoke(bytesCopied) == true) break

        currentCoroutineContext().ensureActive()
        bytes = read(buffer)
        currentCoroutineContext().ensureActive()
    }
    return bytesCopied
}