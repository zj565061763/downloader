package com.sd.lib.downloader

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

fun IDownloader.downloadInfoFlow(url: String? = null): Flow<IDownloadInfo> {
  return callbackFlow {
    val scope = MainScope()

    val callback = object : IDownloader.Callback {
      override fun onDownloadInfo(info: IDownloadInfo) {
        if (url == null || url == info.url) {
          scope.launch {
            send(info)
          }
        }
      }
    }.also { it.register() }

    awaitClose {
      callback.unregister()
      scope.cancel()
    }
  }
}

/**
 * 添加下载任务
 */
suspend fun IDownloader.addTaskAwait(
  url: String,
  callback: IDownloader.Callback? = null,
): Result<File> {
  return addTaskAwait(
    request = DownloadRequest.Builder().build(url),
    callback = callback,
  )
}

/**
 * 添加下载任务
 */
suspend fun IDownloader.addTaskAwait(
  request: DownloadRequest,
  callback: IDownloader.Callback? = null,
): Result<File> {
  return suspendCancellableCoroutine { continuation ->
    val awaitCallback = AwaitCallback(
      url = request.url,
      continuation = continuation,
      callback = callback,
    )

    awaitCallback.register()
    FDownloader.addTask(request)

    continuation.invokeOnCancellation {
      awaitCallback.unregister()
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
          this@AwaitCallback.unregister()
          continuation.resume(Result.success(info.file))
        }
        is IDownloadInfo.Error -> {
          this@AwaitCallback.unregister()
          continuation.resume(Result.failure(info.exception))
        }
        else -> {}
      }
    }
  }
}