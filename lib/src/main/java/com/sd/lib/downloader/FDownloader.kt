package com.sd.lib.downloader

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.sd.lib.downloader.exception.DownloadException
import com.sd.lib.downloader.exception.DownloadExceptionCancellation
import com.sd.lib.downloader.exception.DownloadExceptionCompleteFile
import com.sd.lib.downloader.exception.DownloadExceptionPrepareFile
import com.sd.lib.downloader.exception.DownloadExceptionSubmitTask
import com.sd.lib.downloader.executor.IDownloadExecutor
import com.sd.lib.downloader.utils.IDir
import com.sd.lib.downloader.utils.fDir
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

object FDownloader : IDownloader {
    private val _mapTask: MutableMap<String, DownloadTaskWrapper> = hashMapOf()
    private val _mapTempFile: MutableMap<File, String> = hashMapOf()

    private val _callbackHolder: MutableMap<IDownloader.Callback, String> = ConcurrentHashMap()
    private val _continuationHolder: MutableMap<String, MutableSet<Continuation<Result<File>>>> = hashMapOf()

    private val config get() = DownloaderConfig.get()
    private val _downloadDirectory by lazy { config.downloadDirectory.fDir() }

    private val _handler by lazy { Handler(Looper.getMainLooper()) }

    override fun addCallback(callback: IDownloader.Callback) {
        val put = _callbackHolder.put(callback, "")
        if (put == null) {
            logMsg { "addCallback:${callback} size:${_callbackHolder.size}" }
        }
    }

    override fun removeCallback(callback: IDownloader.Callback) {
        if (_callbackHolder.remove(callback) != null) {
            logMsg { "removeCallback:${callback} size:${_callbackHolder.size}" }
        }
    }

    override fun getDownloadFile(url: String?): File? {
        return _downloadDirectory.getKeyFile(url).takeIf { it?.isFile == true }
    }

    override fun deleteDownloadFile(ext: String?) {
        _downloadDirectory.deleteFile(ext).let { count ->
            if (count > 0) {
                logMsg { "deleteDownloadFile ext:${ext} count:${count} " }
            }
        }
    }

    override fun deleteTempFile() {
        _downloadDirectory.deleteTempFile {
            _mapTempFile.containsKey(it)
        }.let { count ->
            if (count > 0) {
                logMsg { "deleteTempFile count:${count}" }
            }
        }
    }

    @Synchronized
    override fun hasTask(url: String?): Boolean {
        return _mapTask[url] != null
    }

    override fun addTask(url: String?): Boolean {
        return addTask(DownloadRequest.Builder().build(url))
    }

    @Synchronized
    override fun addTask(request: DownloadRequest): Boolean {
        val url = request.url
        if (_mapTask.containsKey(url)) return true

        val task = DownloadTask(url)

        val tempFile = _downloadDirectory.getKeyTempFile(url)
        if (tempFile == null) {
            logMsg { "addTask error create temp file failed:${url}" }
            notifyError(task, DownloadExceptionPrepareFile())
            return false
        }

        val downloadUpdater = DefaultDownloadUpdater(
            task = task,
            tempFile = tempFile,
            downloadDirectory = _downloadDirectory,
        )

        try {
            config.downloadExecutor.submit(
                request = request,
                file = tempFile,
                updater = downloadUpdater,
            )
        } catch (e: Exception) {
            check(e !is DownloadException)
            notifyError(task, DownloadExceptionSubmitTask(e))
            return false
        }

        _mapTask[url] = DownloadTaskWrapper(task, tempFile)
        _mapTempFile[tempFile] = url
        logMsg { "addTask url:${url} temp:${tempFile.absolutePath} size:${_mapTask.size} tempSize:${_mapTempFile.size}" }
        return true
    }

    @Synchronized
    override fun cancelTask(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        if (!hasTask(url)) return false

        logMsg { "cancelTask start url:${url}" }
        val result = config.downloadExecutor.cancel(url)
        logMsg { "cancelTask finish result:${result} url:${url}" }
        return result
    }

    override suspend fun awaitTask(url: String): Result<File> {
        return awaitTask(DownloadRequest.Builder().build(url))
    }

    override suspend fun awaitTask(request: DownloadRequest): Result<File> {
        val url = request.url
        return suspendCancellableCoroutine { cont ->
            synchronized(this@FDownloader) {
                val holder = _continuationHolder[url] ?: hashSetOf<Continuation<Result<File>>>().also {
                    _continuationHolder[url] = it
                }
                holder.add(cont)
                logMsg { "awaitTask url:${url} size:${holder.size} urlSize:${_continuationHolder.size}" }

                cont.invokeOnCancellation {
                    synchronized(this@FDownloader) {
                        _continuationHolder[url]?.let { holder ->
                            holder.remove(cont)
                            if (holder.isEmpty()) _continuationHolder.remove(url)
                            logMsg { "awaitTask cancel url:${url} size:${holder.size} urlSize:${_continuationHolder.size}" }
                        }
                        removeAwaitCallback()
                    }
                }
                addCallback(_awaitCallback)
                addTask(request)
            }
        }
    }

    /**
     * 任务结束，移除下载任务
     */
    @Synchronized
    private fun removeTask(url: String) {
        val wrapper = _mapTask.remove(url)
        if (wrapper != null) {
            _mapTempFile.remove(wrapper.tempFile)
            logMsg { "removeTask url:${url} size:${_mapTask.size} tempSize:${_mapTempFile.size}" }
        }
    }

    internal fun notifyProgress(info: DownloadTask, total: Long, current: Long) {
        info.notifyProgress(total, current)?.let { progress ->
            _handler.post {
                for (item in _callbackHolder.keys) {
                    item.onProgress(info.url, progress)
                }
            }
        }
    }

    internal fun notifySuccess(info: DownloadTask, file: File) {
        if (info.notifySuccess()) {
            removeTask(info.url)
            _handler.post {
                logMsg { "notify callback onSuccess url:${info.url} file:${file.absolutePath}" }
                for (item in _callbackHolder.keys) {
                    item.onSuccess(info.url, file)
                }
            }
        }
    }

    internal fun notifyError(info: DownloadTask, exception: DownloadException) {
        if (info.notifyError()) {
            removeTask(info.url)
            _handler.post {
                logMsg { "notify callback onError url:${info.url} exception:${exception}" }
                for (item in _callbackHolder.keys) {
                    item.onError(info.url, exception)
                }
            }
        }
    }

    private val _awaitCallback = object : IDownloader.Callback {
        override fun onProgress(url: String, progress: DownloadProgress) {
        }

        override fun onSuccess(url: String, file: File) {
            resumeTask(url, Result.success(file))
        }

        override fun onError(url: String, exception: DownloadException) {
            resumeTask(url, Result.failure(exception))
        }
    }

    @Synchronized
    private fun resumeTask(url: String, result: Result<File>) {
        _continuationHolder.remove(url)?.let { holder ->
            logMsg { "resumeTask ${if (result.isSuccess) "success" else "failure"} url:${url} size:${holder.size} urlSize:${_continuationHolder.size}" }
            holder.forEach {
                it.resume(result)
            }
        }
        removeAwaitCallback()
    }

    private fun removeAwaitCallback() {
        if (_continuationHolder.isEmpty()) {
            removeCallback(_awaitCallback)
        }
    }
}

private class DefaultDownloadUpdater(
    task: DownloadTask,
    tempFile: File,
    downloadDirectory: IDir,
) : IDownloadExecutor.Updater {

    private val _url = task.url
    private val _task = task
    private val _tempFile = tempFile
    private val _downloadDirectory = downloadDirectory

    @Volatile
    private var _isFinish = false
        set(value) {
            require(value) { "Require true value." }
            field = value
        }

    override fun notifyProgress(total: Long, current: Long) {
        if (_isFinish) return
        FDownloader.notifyProgress(_task, total, current)
    }

    override fun notifySuccess() {
        if (_isFinish) return
        _isFinish = true
        logMsg { "updater download success $_url" }

        if (!_tempFile.exists()) {
            logMsg { "updater download success error temp file not exists $_url" }
            FDownloader.notifyError(_task, DownloadExceptionCompleteFile())
            return
        }

        val downloadFile = _downloadDirectory.getKeyFile(_url)
        if (downloadFile == null) {
            logMsg { "updater download success error create download file $_url" }
            FDownloader.notifyError(_task, DownloadExceptionCompleteFile())
            return
        }

        if (_tempFile.renameTo(downloadFile)) {
            FDownloader.notifySuccess(_task, downloadFile)
        } else {
            logMsg { "updater download success error rename temp file to download file $_url" }
            FDownloader.notifyError(_task, DownloadExceptionCompleteFile())
        }
    }

    override fun notifyError(t: Throwable) {
        if (_isFinish) return
        _isFinish = true
        logMsg { "updater download error:${t} $_url" }

        if (t is CancellationException) {
            FDownloader.notifyError(_task, DownloadExceptionCancellation())
        } else {
            FDownloader.notifyError(_task, DownloadException.wrap(t))
        }
    }
}

private class DownloadTaskWrapper(
    val task: DownloadTask,
    val tempFile: File,
)

internal inline fun logMsg(block: () -> String) {
    if (DownloaderConfig.get().isDebug) {
        Log.i("FDownload", block())
    }
}