package com.sd.lib.downloader.executor

import com.sd.lib.downloader.DownloadRequest
import com.sd.lib.downloader.exception.DownloadHttpExceptionResponseCode
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection

class DefaultDownloadExecutor @JvmOverloads constructor(
    limitedParallelism: Int = 3,
    preferBreakpoint: Boolean = false,
) : IDownloadExecutor {

    private val _preferBreakpoint = preferBreakpoint
    private val _taskHolder: MutableMap<String, Job> = hashMapOf()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _scope by lazy {
        val job = SupervisorJob()
        val dispatcher = Dispatchers.IO.limitedParallelism(limitedParallelism)
        CoroutineScope(job + dispatcher)
    }

    override fun submit(
        request: DownloadRequest,
        file: File,
        updater: IDownloadExecutor.Updater,
    ) {
        _scope.launch(CoroutineExceptionHandler { _, _ -> }) {
            handleRequest(
                request = request,
                file = file,
                updater = updater,
            )
        }.also { job ->
            val url = request.url
            job.invokeOnCompletion { e ->
                _taskHolder.remove(url)
                if (e != null) {
                    updater.notifyError(e)
                } else {
                    updater.notifySuccess()
                }
            }
            _taskHolder[url] = job
        }
    }

    override fun cancel(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        val job = _taskHolder[url] ?: return false
        job.cancel()
        return true
    }

    private suspend fun handleRequest(
        request: DownloadRequest,
        file: File,
        updater: IDownloadExecutor.Updater,
    ) {
        val length = file.length()
        val breakpoint = request.preferBreakpoint ?: _preferBreakpoint && length > 0

        if (breakpoint) {
            val httpRequest = newHttpRequest(request)
            val code = httpRequest.run {
                this.header("Range", "bytes=$length-")
                this.code()
            }

            currentCoroutineContext().ensureActive()
            if (code == HttpURLConnection.HTTP_PARTIAL) {
                downloadBreakpoint(
                    httpRequest = httpRequest,
                    file = file,
                    updater = updater,
                )
                return
            }
        }

        val httpRequest = newHttpRequest(request)
        val code = httpRequest.code()

        currentCoroutineContext().ensureActive()
        if (code == HttpURLConnection.HTTP_OK) {
            downloadNormal(
                httpRequest = httpRequest,
                file = file,
                updater = updater,
            )
        } else {
            throw DownloadHttpExceptionResponseCode(code)
        }
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
                    },
                )
            }
        }
    }
}

private fun newHttpRequest(downloadRequest: DownloadRequest): HttpRequest {
    return HttpRequest.get(downloadRequest.url)
        .connectTimeout(15 * 1000)
        .readTimeout(15 * 1000)
        .trustAllHosts()
        .trustAllCerts()
}

private suspend inline fun InputStream.copyToOutput(
    output: (buffer: ByteArray, offset: Int, length: Int) -> Unit,
    callback: (count: Long) -> Unit,
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
        callback(bytesCopied)

        currentCoroutineContext().ensureActive()
        bytes = read(buffer)
        currentCoroutineContext().ensureActive()
    }
    return bytesCopied
}