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
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

object FDownloader : IDownloader {
    private val _mapTask: MutableMap<String, DownloadTaskInfo> = hashMapOf()
    private val _mapTempFile: MutableMap<File, String> = hashMapOf()

    private val _callbackHolder: MutableMap<IDownloader.Callback, String> = ConcurrentHashMap()

    private val _downloadDirectory by lazy { config.downloadDirectory.fDir() }
    private val config get() = DownloaderConfig.get()

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

        _mapTask[url] = DownloadTaskInfo(tempFile)
        _mapTempFile[tempFile] = url
        logMsg { "addTask url:${url} temp:${tempFile.absolutePath} size:${_mapTask.size} tempSize:${_mapTempFile.size}" }
        if (task.notifyInitialized()) {
            val info = IDownloadInfo.Initialized(task.url)
            notifyDownloadInfo(info) {
                logMsg { "notify callback Initialized" }
            }
        }

        val downloadUpdater = DefaultDownloadUpdater(
            task = task,
            tempFile = tempFile,
            downloadDirectory = _downloadDirectory,
        )

        return try {
            config.downloadExecutor.submit(
                request = request,
                file = tempFile,
                updater = downloadUpdater,
            )
            true
        } catch (e: Exception) {
            check(e !is DownloadException)
            notifyError(task, DownloadExceptionSubmitTask(e))
            false
        }
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

    override suspend fun awaitTask(
        url: String,
        callback: IDownloader.Callback?,
    ): Result<File> {
        return awaitTask(
            request = DownloadRequest.Builder().build(url),
            callback = callback,
        )
    }

    override suspend fun awaitTask(
        request: DownloadRequest,
        callback: IDownloader.Callback?,
    ): Result<File> {
        val url = request.url
        return suspendCancellableCoroutine { continuation ->
            val awaitCallback = AwaitCallback(
                url = request.url,
                continuation = continuation,
                callback = callback,
            )

            continuation.invokeOnCancellation {
                removeCallback(awaitCallback)
            }

            if (continuation.isActive) {
                logMsg { "awaitTask url:${url}" }
                addCallback(awaitCallback)
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

    private val _handler by lazy { Handler(Looper.getMainLooper()) }

    internal fun notifyProgress(task: DownloadTask, total: Long, current: Long) {
        task.notifyProgress(total, current)?.let { info ->
            notifyDownloadInfo(info)
        }
    }

    internal fun notifySuccess(task: DownloadTask, file: File) {
        if (task.notifySuccess()) {
            removeTask(task.url)
            val info = IDownloadInfo.Success(task.url, file)
            notifyDownloadInfo(info) {
                logMsg { "notify callback Success url:${task.url} file:${file.absolutePath}" }
            }
        }
    }

    internal fun notifyError(task: DownloadTask, exception: DownloadException) {
        if (task.notifyError()) {
            removeTask(task.url)
            val info = IDownloadInfo.Error(task.url, exception)
            notifyDownloadInfo(info) {
                logMsg { "notify callback Error url:${task.url} exception:${exception.javaClass.simpleName}" }
            }
        }
    }

    private fun notifyDownloadInfo(
        info: IDownloadInfo,
        block: (() -> Unit)? = null,
    ) {
        _handler.post {
            block?.invoke()
            for (item in _callbackHolder.keys) {
                item.onDownloadInfo(info)
            }
        }
    }

    private class DownloadTaskInfo(
        val tempFile: File,
    )
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

    private val _isFinish = AtomicBoolean(false)

    override fun notifyProgress(total: Long, current: Long) {
        if (_isFinish.get()) return
        FDownloader.notifyProgress(_task, total, current)
    }

    override fun notifySuccess() {
        if (_isFinish.compareAndSet(false, true)) {
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
    }

    override fun notifyError(e: Throwable) {
        if (_isFinish.compareAndSet(false, true)) {
            logMsg { "updater download error:${e} $_url" }

            if (e is CancellationException) {
                FDownloader.notifyError(_task, DownloadExceptionCancellation())
            } else {
                FDownloader.notifyError(_task, DownloadException.wrap(e))
            }
        }
    }
}

private class AwaitCallback(
    private val url: String,
    private val continuation: CancellableContinuation<Result<File>>,
    private val callback: IDownloader.Callback?,
) : IDownloader.Callback {

    override fun onDownloadInfo(info: IDownloadInfo) {
        if (info.url == url && continuation.isActive) {
            callback?.onDownloadInfo(info)
            when (info) {
                is IDownloadInfo.Success -> {
                    logMsg { "awaitTask resume success url:${url}" }
                    FDownloader.removeCallback(this)
                    continuation.resume(Result.success(info.file))
                }
                is IDownloadInfo.Error -> {
                    logMsg { "awaitTask resume error url:${url} exception:${info.exception}" }
                    FDownloader.removeCallback(this)
                    continuation.resume(Result.failure(info.exception))
                }
                else -> {}
            }
        }
    }
}

internal inline fun logMsg(block: () -> String) {
    if (DownloaderConfig.get().isDebug) {
        Log.i("FDownloader", block())
    }
}