package com.sd.lib.downloader

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.sd.lib.downloader.exception.DownloadException
import com.sd.lib.downloader.exception.DownloadExceptionCancellation
import com.sd.lib.downloader.exception.DownloadExceptionCreateDownloadFile
import com.sd.lib.downloader.exception.DownloadExceptionCreateTempFile
import com.sd.lib.downloader.exception.DownloadExceptionIllegalRequestEmptyUrl
import com.sd.lib.downloader.exception.DownloadExceptionRenameFile
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
  private val _tempFiles: MutableSet<File> = Collections.newSetFromMap(ConcurrentHashMap())

  /** 正在取消中的任务 */
  private val _cancellingTasks: MutableSet<String> = mutableSetOf()
  /** 等待中的请求 */
  private val _pendingRequests: MutableMap<String, DownloadRequest> = mutableMapOf()

  /** 下载目录 */
  private val _downloadDir: DownloadDir = DownloadDir.get(_config.downloadDirectory)
  private val _callbacks: MutableSet<Downloader.Callback> = Collections.newSetFromMap(ConcurrentHashMap())

  private val _config get() = DownloaderConfig.get()
  private val _handler = Handler(Looper.getMainLooper())

  override fun registerCallback(callback: Downloader.Callback) {
    if (_callbacks.add(callback)) {
      logMsg { "registerCallback:${callback} size:${_callbacks.size}" }
    }
  }

  override fun unregisterCallback(callback: Downloader.Callback) {
    if (_callbacks.remove(callback)) {
      logMsg { "unregisterCallback:${callback} size:${_callbacks.size}" }
    }
  }

  override fun getDownloadFile(url: String): File? {
    return _downloadDir.existOrNullFileForKey(url)
  }

  override fun deleteTempFile() {
    _downloadDir.tempFiles { files ->
      var count = 0
      files.forEach { file ->
        if (!_tempFiles.contains(file)) {
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
  override fun getDownloadInfo(url: String): AccessibleDownloadInfo? {
    return _mapTask[url]?.info
  }

  override fun addTask(url: String): Boolean {
    return addTask(DownloadRequest.Builder().build(url))
  }

  @Synchronized
  override fun addTask(request: DownloadRequest): Boolean {
    val url = request.url

    if (_mapTask.containsKey(url)) {
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
      notifyError(task, DownloadExceptionIllegalRequestEmptyUrl())
      return false
    }

    val tempFile = _downloadDir.tempFileForKey(url)
    if (tempFile == null) {
      logMsg { "addTask $url error create temp file failed" }
      notifyError(task, DownloadExceptionCreateTempFile())
      return false
    }

    val downloadFile = _downloadDir.fileForKey(url)
    if (downloadFile == null) {
      logMsg { "addTask $url error create download file failed" }
      notifyError(task, DownloadExceptionCreateDownloadFile())
      return false
    }

    _mapTask[url] = DownloadTaskInfo(tempFile, task)
    _tempFiles.add(tempFile)
    logMsg { "addTask $url temp:${tempFile.absolutePath} size:${_mapTask.size} tempSize:${_tempFiles.size}" }

    if (task.notifyInitialized()) {
      val initializedInfo = DownloadInfo.Initialized(task.url)
      _mapTask[url]?.info = initializedInfo
      notifyCallbacks(initializedInfo)
    }

    return try {
      _config.downloadExecutor.submit(
        request = request,
        file = tempFile,
        updater = DefaultDownloadUpdater(
          task = task,
          tempFile = tempFile,
          downloadFile = downloadFile,
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
    if (_mapTask.containsKey(url)) {
      logMsg { "cancelTask $url start" }

      removePendingRequest(url)
      _config.downloadExecutor.cancel(url)

      val taskInfo = _mapTask[url]
      if (taskInfo != null) {
        /**
         * 如果[DownloadExecutor.cancel]之后任务依然存在，
         * 说明没有同步回调[DownloadExecutor.Updater.notifyError]移除任务。
         */
        logMsg { "cancelTask $url was not removed synchronously" }
        _cancellingTasks.add(url)

        val task = taskInfo.task
        if (task.notifyCancelling()) {
          val cancellingInfo = DownloadInfo.Cancelling(task.url)
          taskInfo.info = cancellingInfo
          notifyCallbacks(cancellingInfo)
        }
      }

      logMsg { "cancelTask $url finish" }
    }
  }

  /** 移除等待中的请求 */
  @Synchronized
  private fun removePendingRequest(url: String): DownloadRequest? {
    return _pendingRequests.remove(url)?.also { request ->
      logMsg { "removePendingRequest $url request:${request} pendingSize:${_pendingRequests.size}" }
    }
  }

  /** 任务结束，移除下载任务 */
  @Synchronized
  private fun removeTask(url: String) {
    val taskInfo = _mapTask.remove(url)
    if (taskInfo != null) {
      _tempFiles.remove(taskInfo.tempFile)
      _cancellingTasks.remove(url)
      logMsg { "removeTask $url size:${_mapTask.size} tempSize:${_tempFiles.size} cancelingSize:${_cancellingTasks.size}" }
    }
  }

  internal fun notifyProgress(task: DownloadTask, total: Long, current: Long) {
    task.notifyProgress(total, current)?.also { progressInfo ->
      notifyCallbacks(progressInfo)
    }
  }

  internal fun notifySuccess(task: DownloadTask, file: File) {
    if (task.notifySuccess()) {
      removeTask(task.url)
      notifyCallbacks(DownloadInfo.Success(task.url, file))
    }
  }

  internal fun notifyError(task: DownloadTask, exception: DownloadException) {
    if (task.notifyError()) {
      removeTask(task.url)
      notifyCallbacks(DownloadInfo.Error(task.url, exception))
      // 检查是否有正在等待中的请求
      removePendingRequest(task.url)?.also { request ->
        addTask(request)
      }
    }
  }

  private fun notifyCallbacks(info: DownloadInfo) {
    _handler.post {
      logDownloadInfoNotify(info)
      for (item in _callbacks) {
        item.onDownloadInfo(info)
      }
    }
  }

  private class DownloadTaskInfo(
    val tempFile: File,
    val task: DownloadTask,
    var info: AccessibleDownloadInfo? = null,
  )
}

private class DefaultDownloadUpdater(
  private val task: DownloadTask,
  private val tempFile: File,
  private val downloadFile: File,
  private val downloadDir: DownloadDir,
) : DownloadExecutor.Updater {
  private val _isFinish = AtomicBoolean(false)

  override fun notifyProgress(total: Long, current: Long) {
    if (_isFinish.get()) return
    FDownloader.notifyProgress(task, total, current)
  }

  override fun notifySuccess() {
    if (_isFinish.compareAndSet(false, true)) {
      if (!tempFile.exists()) {
        logMsg { "updater notifySuccess $${task.url} error temp file not found" }
        FDownloader.notifyError(task, DownloadExceptionTempFileNotFound())
        return
      }

      val renamed = synchronized(downloadDir) {
        downloadFile.deleteRecursively()
        tempFile.renameTo(downloadFile)
      }

      if (renamed) {
        logMsg { "updater notifySuccess ${task.url}" }
        FDownloader.notifySuccess(task, downloadFile)
      } else {
        logMsg { "updater notifySuccess ${task.url} error rename temp file to download file" }
        FDownloader.notifyError(task, DownloadExceptionRenameFile())
      }
    }
  }

  override fun notifyError(e: Throwable) {
    if (_isFinish.compareAndSet(false, true)) {
      logMsg { "updater notifyError ${task.url} $e" }
      if (e is CancellationException) {
        FDownloader.notifyError(task, DownloadExceptionCancellation())
      } else {
        FDownloader.notifyError(task, DownloadException.wrap(e))
      }
    }
  }
}

private fun logDownloadInfoNotify(info: DownloadInfo) {
  logMsg {
    buildString {
      append("notifyCallbacks ${info.url} ")
      when (info) {
        is DownloadInfo.Initialized -> "Initialized"
        is DownloadInfo.Progress -> "Progress ${info.progress} ${info.current}/${info.total} "
        is DownloadInfo.Cancelling -> "Cancelling"
        is DownloadInfo.Success -> "Success file:${info.file}"
        is DownloadInfo.Error -> "Error exception:${info.exception}"
      }.also {
        append(it)
      }
    }
  }
}

internal inline fun logMsg(block: () -> String) {
  if (DownloaderConfig.get().isDebug) {
    Log.i("FDownloader", block())
  }
}