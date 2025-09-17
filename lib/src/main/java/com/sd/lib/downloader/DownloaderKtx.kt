package com.sd.lib.downloader

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

fun Downloader.downloadInfoFlow(url: String? = null): Flow<IDownloadInfo> {
  return callbackFlow {
    val scope = MainScope()
    val callback = object : Downloader.Callback {
      override fun onDownloadInfo(info: IDownloadInfo) {
        if (url == null || url == info.url) {
          scope.launch { send(info) }
        }
      }
    }
    registerCallback(callback)
    awaitClose {
      unregisterCallback(callback)
      scope.cancel()
    }
  }
}

/**
 * 添加下载任务
 */
suspend fun Downloader.addTaskAwait(
  url: String,
  callback: Downloader.Callback? = null,
): Result<File> {
  return addTaskAwait(
    request = DownloadRequest.Builder().build(url),
    callback = callback,
  )
}

/**
 * 添加下载任务
 */
suspend fun Downloader.addTaskAwait(
  request: DownloadRequest,
  callback: Downloader.Callback? = null,
): Result<File> {
  return suspendCancellableCoroutine { continuation ->
    val realCallback = object : Downloader.Callback {
      override fun onDownloadInfo(info: IDownloadInfo) {
        if (!continuation.isActive) {
          unregisterCallback(this)
          return
        }
        if (info.url == request.url) {
          callback?.onDownloadInfo(info)
          when (info) {
            is IDownloadInfo.Success -> {
              unregisterCallback(this)
              continuation.resume(Result.success(info.file))
            }
            is IDownloadInfo.Error -> {
              unregisterCallback(this)
              continuation.resume(Result.failure(info.exception))
            }
            else -> {}
          }
        }
      }
    }

    registerCallback(realCallback)
    addTask(request)

    continuation.invokeOnCancellation {
      unregisterCallback(realCallback)
    }
  }
}