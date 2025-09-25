package com.sd.lib.downloader

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * 添加下载任务，注意：如果当前协程被取消，不会取消下载任务
 */
suspend fun Downloader.addTaskAwait(
  request: DownloadRequest,
  callback: DownloadInfoCallback? = null,
): Result<File> {
  return suspendCancellableCoroutine { continuation ->
    val realCallback = object : DownloadInfoCallback {
      override fun onDownloadInfo(info: DownloadInfo) {
        if (!continuation.isActive) {
          unregisterCallback(this)
          return
        }
        if (request.url == info.url) {
          callback?.onDownloadInfo(info)
          when (info) {
            is DownloadInfo.Success -> {
              unregisterCallback(this)
              continuation.resume(Result.success(info.file))
            }
            is DownloadInfo.Error -> {
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

/** 监听所有下载信息 */
fun Downloader.Companion.downloadInfoFlow(): Flow<DownloadInfo> {
  return downloadInfoFlow { info ->
    trySend(info)
  }
}

/** 监听指定[url]的下载信息 */
fun Downloader.Companion.downloadInfoFlowOf(url: String): Flow<DownloadInfo> {
  return downloadInfoFlow { info ->
    if (url == info.url) {
      trySend(info)
    }
  }
}

private fun Downloader.Companion.downloadInfoFlow(
  block: ProducerScope<DownloadInfo>.(DownloadInfo) -> Unit,
): Flow<DownloadInfo> {
  return callbackFlow {
    val callback = object : DownloadInfoCallback {
      override fun onDownloadInfo(info: DownloadInfo) {
        block(info)
      }
    }
    FDownloader.registerCallback(callback)
    awaitClose {
      FDownloader.unregisterCallback(callback)
    }
  }.buffer(Channel.UNLIMITED)
}