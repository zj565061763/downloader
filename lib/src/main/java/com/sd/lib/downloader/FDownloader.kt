package com.sd.lib.downloader

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.sd.lib.downloader.exception.DownloadException
import com.sd.lib.downloader.exception.DownloadExceptionCancellation
import com.sd.lib.downloader.exception.DownloadExceptionCompleteFile
import com.sd.lib.downloader.exception.DownloadExceptionCreateDownloadFile
import com.sd.lib.downloader.exception.DownloadExceptionCreateTempFile
import com.sd.lib.downloader.exception.DownloadExceptionIllegalRequest
import com.sd.lib.downloader.exception.DownloadExceptionSubmitTask
import com.sd.lib.downloader.exception.DownloadExceptionTempFileNotFound
import com.sd.lib.downloader.executor.DownloadExecutor
import java.io.File
import java.util.Collections
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object FDownloader : Downloader {
  /** 所有任务 */
  private val _mapTask: MutableMap<String, DownloadTaskInfo> = mutableMapOf()
  /** 下载中的临时文件 */
  private val _mapTempFile: MutableMap<File, String> = Collections.synchronizedMap(mutableMapOf())

  /** 正在取消中的任务 */
  private val _cancellingTasks: MutableSet<String> = mutableSetOf()
  /** 等待中的请求 */
  private val _pendingRequests: MutableMap<String, DownloadRequest> = mutableMapOf()

  /** 下载目录 */
  private val _downloadDir: DownloadDir = DownloadDir.get(_config.downloadDirectory)
  private val _callbacks: MutableMap<Downloader.Callback, String> = ConcurrentHashMap()

  private val _config get() = DownloaderConfig.get()
  private val _handler by lazy { Handler(Looper.getMainLooper()) }

  override fun registerCallback(callback: Downloader.Callback) {
    if (_callbacks.put(callback, "") == null) {
      logMsg { "registerCallback:${callback} size:${_callbacks.size}" }
    }
  }

  override fun unregisterCallback(callback: Downloader.Callback) {
    if (_callbacks.remove(callback) != null) {
      logMsg { "unregisterCallback:${callback} size:${_callbacks.size}" }
    }
  }

  override fun deleteTempFile() {
    _downloadDir.tempFiles { files ->
      var count = 0
      files.forEach { file ->
        if (!_mapTempFile.containsKey(file)) {
          if (file.deleteRecursively()) count++
        }
      }
      count
    }.also { count ->
      if (count > 0) {
        logMsg { "deleteTempFile count:${count}" }
      }
    }
  }

  override fun deleteDownloadFile(block: (File) -> Boolean) {
    _downloadDir.files { files ->
      var count = 0
      files.forEach { file ->
        if (block(file)) {
          if (file.deleteRecursively()) count++
        }
      }
      count
    }.also { count ->
      if (count > 0) {
        logMsg { "deleteDownloadFile count:${count}" }
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
      if (_cancellingTasks.contains(url)) {
        // url对应的任务正在取消中，把请求添加到等待列表
        _pendingRequests[url] = request
        logMsg { "addTask $url addPendingRequest request:${request} pendingSize:${_pendingRequests.size}" }
      }
      return true
    }

    val progressNotifyStrategy = request.progressNotifyStrategy ?: _config.progressNotifyStrategy
    val task = DownloadTask(url, progressNotifyStrategy)

    if (url.isEmpty()) {
      logMsg { "addTask error url is empty" }
      notifyError(task, DownloadExceptionIllegalRequest("url is empty"))
      return false
    }

    val tempFile = _downloadDir.tempFileForKey(url)
    if (tempFile == null) {
      logMsg { "addTask $url error create temp file failed" }
      notifyError(task, DownloadExceptionCreateTempFile())
      return false
    }

    _mapTask[url] = DownloadTaskInfo(tempFile)
    _mapTempFile[tempFile] = url
    logMsg { "addTask $url temp:${tempFile.absolutePath} size:${_mapTask.size} tempSize:${_mapTempFile.size}" }
    notifyInitialized(task)

    return try {
      _config.downloadExecutor.submit(
        request = request,
        file = tempFile,
        updater = DefaultDownloadUpdater(
          task = task,
          tempFile = tempFile,
          downloadDir = _downloadDir,
        ),
      )
      true
    } catch (e: Throwable) {
      check(e !is DownloadException)
      logMsg { "addTask $url submit error $e" }
      notifyError(task, DownloadExceptionSubmitTask(e))
      false
    }
  }

  @Synchronized
  override fun cancelTask(url: String) {
    if (hasTask(url)) {
      logMsg { "cancelTask $url start" }

      removePendingRequest(url)
      _config.downloadExecutor.cancel(url)

      if (hasTask(url)) {
        /**
         * 如果[DownloadExecutor.cancel]之后任务依然存在，
         * 说明没有同步回调[DownloadExecutor.Updater.notifyError]移除任务。
         */
        logMsg { "cancelTask $url was not removed synchronously" }
        _cancellingTasks.add(url)
      }

      logMsg { "cancelTask $url finish" }
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
      _cancellingTasks.remove(url)
      logMsg { "removeTask $url size:${_mapTask.size} tempSize:${_mapTempFile.size} cancelingSize:${_cancellingTasks.size}" }
    }
  }

  /**
   * 移除等待中的请求
   */
  @Synchronized
  private fun removePendingRequest(url: String): DownloadRequest? {
    return _pendingRequests.remove(url)?.also { request ->
      logMsg { "removePendingRequest $url request:${request} pendingSize:${_pendingRequests.size}" }
    }
  }

  private fun notifyInitialized(task: DownloadTask) {
    if (task.notifyInitialized()) {
      DownloadInfo.Initialized(task.url).notifyCallbacks {
        logMsg { "notifyCallbacks ${it.url} Initialized" }
      }
    }
  }

  internal fun notifyProgress(task: DownloadTask, total: Long, current: Long) {
    task.notifyProgress(total, current)?.notifyCallbacks {
      logMsg { "notifyCallbacks ${it.url} Progress ${it.progress}" }
    }
  }

  internal fun notifySuccess(task: DownloadTask, file: File) {
    val url = task.url
    if (task.notifySuccess()) {
      removeTask(url)
      DownloadInfo.Success(url, file).notifyCallbacks {
        logMsg { "notifyCallbacks ${it.url} Success file:${file.absolutePath}" }
      }
    }
  }

  internal fun notifyError(task: DownloadTask, exception: DownloadException) {
    val url = task.url
    if (task.notifyError()) {
      removeTask(url)
      DownloadInfo.Error(url, exception).notifyCallbacks {
        logMsg { "notifyCallbacks ${it.url} Error exception:${exception}" }
      }
      removePendingRequest(url)?.also { request ->
        addTask(request)
      }
    }
  }

  private fun <T : DownloadInfo> T.notifyCallbacks(block: ((T) -> Unit)? = null) {
    val info = this
    _handler.post {
      block?.invoke(info)
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
  downloadDir: DownloadDir,
) : DownloadExecutor.Updater {
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
      if (!_tempFile.exists()) {
        logMsg { "updater notifySuccess error temp file not found $_url" }
        FDownloader.notifyError(_task, DownloadExceptionTempFileNotFound())
        return
      }

      val downloadFile = _downloadDir.fileForKey(_url)
      if (downloadFile == null) {
        logMsg { "updater notifySuccess error create download file $_url" }
        FDownloader.notifyError(_task, DownloadExceptionCreateDownloadFile())
        return
      }

      if (_tempFile.renameTo(downloadFile)) {
        logMsg { "updater notifySuccess $_url" }
        FDownloader.notifySuccess(_task, downloadFile)
      } else {
        logMsg { "updater notifySuccess error rename temp file to download file $_url" }
        FDownloader.notifyError(_task, DownloadExceptionCompleteFile())
      }
    }
  }

  override fun notifyError(e: Throwable) {
    if (_isFinish.compareAndSet(false, true)) {
      logMsg { "updater notifyError ${e} $_url" }
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