package com.sd.lib.downloader

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.sd.lib.downloader.exception.DownloadException
import com.sd.lib.downloader.exception.DownloadExceptionCancellation
import com.sd.lib.downloader.exception.DownloadExceptionCompleteFile
import com.sd.lib.downloader.exception.DownloadExceptionIllegalRequest
import com.sd.lib.downloader.exception.DownloadExceptionPrepareFile
import com.sd.lib.downloader.exception.DownloadExceptionSubmitTask
import com.sd.lib.downloader.exception.DownloadExceptionTempFileNotFound
import com.sd.lib.downloader.executor.IDownloadExecutor
import java.io.File
import java.util.Collections
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object FDownloader : IDownloader {
    /** 所有任务 */
    private val _mapTask: MutableMap<String, DownloadTaskInfo> = hashMapOf()
    /** 下载中的临时文件 */
    private val _mapTempFile: MutableMap<File, String> = Collections.synchronizedMap(hashMapOf())

    /** 正在取消中的任务 */
    private val _cancelingTasks: MutableSet<String> = hashSetOf()
    /** 等待中的请求 */
    private val _pendingRequests: MutableMap<String, DownloadRequest> = hashMapOf()

    /** 下载目录 */
    private val _downloadDir: IDownloadDir = DownloadDir(config.downloadDirectory)
    private val _callbacks: MutableMap<IDownloader.Callback, String> = ConcurrentHashMap()

    private val config get() = DownloaderConfig.get()
    private val _handler by lazy { Handler(Looper.getMainLooper()) }

    override fun registerCallback(callback: IDownloader.Callback) {
        if (_callbacks.put(callback, "") == null) {
            logMsg { "registerCallback:${callback} size:${_callbacks.size}" }
        }
    }

    override fun unregisterCallback(callback: IDownloader.Callback) {
        if (_callbacks.remove(callback) != null) {
            logMsg { "unregisterCallback:${callback} size:${_callbacks.size}" }
        }
    }

    override fun deleteDownloadFile(ext: String?) {
        _downloadDir.deleteFile {
            ext == null || ext == it.extension
        }.let { count ->
            if (count > 0) {
                logMsg { "deleteDownloadFile ext:${ext} count:${count} " }
            }
        }
    }

    override fun deleteTempFile() {
        _downloadDir.deleteTempFile {
            !_mapTempFile.containsKey(it)
        }.let { count ->
            if (count > 0) {
                logMsg { "deleteTempFile count:${count}" }
            }
        }
    }

    @Synchronized
    override fun hasTask(url: String): Boolean {
        return _mapTask.containsKey(url)
    }

    override fun addTask(url: String): Boolean {
        return addTask(DownloadRequest.Builder().build(url))
    }

    @Synchronized
    override fun addTask(request: DownloadRequest): Boolean {
        val url = request.url

        if (hasTask(url)) {
            if (_cancelingTasks.contains(url)) {
                // url对应的任务正在取消中，把请求添加到等待列表
                _pendingRequests[url] = request
                logMsg { "addTask addPendingRequest url:${url} request:${request} pendingSize:${_pendingRequests.size}" }
            }
            return true
        }

        val task = DownloadTask(url)

        if (url.isEmpty()) {
            logMsg { "addTask error url is empty" }
            notifyError(task, DownloadExceptionIllegalRequest("url is empty"))
            return false
        }

        val tempFile = _downloadDir.getKeyTempFile(url)
        if (tempFile == null) {
            logMsg { "addTask error create temp file failed:${url}" }
            notifyError(task, DownloadExceptionPrepareFile())
            return false
        }

        _mapTask[url] = DownloadTaskInfo(tempFile)
        _mapTempFile[tempFile] = url
        logMsg { "addTask url:${url} temp:${tempFile.absolutePath} size:${_mapTask.size} tempSize:${_mapTempFile.size}" }
        notifyInitialized(task)

        val downloadUpdater = DefaultDownloadUpdater(
            task = task,
            tempFile = tempFile,
            downloadDir = _downloadDir,
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
    override fun cancelTask(url: String) {
        if (hasTask(url)) {
            logMsg { "cancelTask start url:${url}" }
            config.downloadExecutor.cancel(url)

            removePendingRequest(url)
            if (hasTask(url)) {
                _cancelingTasks.add(url)
            }

            logMsg { "cancelTask finish url:${url}" }
        }
    }

    /**
     * 任务结束，移除下载任务
     */
    @Synchronized
    private fun removeTask(url: String) {
        val info = _mapTask.remove(url)
        if (info != null) {
            _mapTempFile.remove(info.tempFile)
            _cancelingTasks.remove(url)
            logMsg { "removeTask url:${url} size:${_mapTask.size} tempSize:${_mapTempFile.size} cancelingSize:${_cancelingTasks.size}" }
        }
    }

    /**
     * 移除等待中的请求
     */
    @Synchronized
    private fun removePendingRequest(url: String): DownloadRequest? {
        return _pendingRequests.remove(url)?.also { request ->
            logMsg { "removePendingRequest url:${url} request:${request} pendingSize:${_pendingRequests.size}" }
        }
    }

    private fun notifyInitialized(task: DownloadTask) {
        if (task.notifyInitialized()) {
            val info = IDownloadInfo.Initialized(task.url)
            notifyDownloadInfo(info) {
                logMsg { "notify callback Initialized" }
            }
        }
    }

    internal fun notifyProgress(task: DownloadTask, total: Long, current: Long) {
        task.notifyProgress(total, current)?.let { info ->
            notifyDownloadInfo(info)
        }
    }

    internal fun notifySuccess(task: DownloadTask, file: File) {
        val url = task.url
        if (task.notifySuccess()) {
            removeTask(url)

            val info = IDownloadInfo.Success(url, file)
            notifyDownloadInfo(info) {
                logMsg { "notify callback Success url:${url} file:${file.absolutePath}" }
            }
        }
    }

    internal fun notifyError(task: DownloadTask, exception: DownloadException) {
        val url = task.url
        if (task.notifyError()) {
            removeTask(url)

            val info = IDownloadInfo.Error(url, exception)
            notifyDownloadInfo(info) {
                logMsg { "notify callback Error url:${url} exception:${exception.javaClass.simpleName} $exception" }
            }

            removePendingRequest(url)?.let { request ->
                addTask(request)
            }
        }
    }

    private fun notifyDownloadInfo(
        info: IDownloadInfo,
        block: (() -> Unit)? = null,
    ) {
        _handler.post {
            block?.invoke()
            for (item in _callbacks.keys) {
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
    downloadDir: IDownloadDir,
) : IDownloadExecutor.Updater {

    private val _url = task.url
    private val _task = task
    private val _tempFile = tempFile
    private val _downloadDir = downloadDir

    private val _isFinish = AtomicBoolean(false)

    override fun notifyProgress(total: Long, current: Long) {
        if (_isFinish.get()) return
        FDownloader.notifyProgress(_task, total, current)
    }

    override fun notifySuccess() {
        if (_isFinish.compareAndSet(false, true)) {
            logMsg { "updater download success $_url" }

            if (!_tempFile.exists()) {
                logMsg { "updater download success error temp file not found $_url" }
                FDownloader.notifyError(_task, DownloadExceptionTempFileNotFound())
                return
            }

            val downloadFile = _downloadDir.getKeyFile(_url)
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

internal inline fun logMsg(block: () -> String) {
    if (DownloaderConfig.get().isDebug) {
        Log.i("FDownloader", block())
    }
}