package com.sd.lib.downloader

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * 添加下载任务
 */
suspend fun IDownloader.awaitTask(
    url: String,
    callback: IDownloader.Callback? = null,
): Result<File> {
    return awaitTask(
        request = DownloadRequest.Builder().build(url),
        callback = callback,
    )
}

/**
 * 添加下载任务
 */
suspend fun IDownloader.awaitTask(
    request: DownloadRequest,
    callback: IDownloader.Callback? = null,
): Result<File> {
    return suspendCancellableCoroutine { continuation ->
        val awaitCallback = AwaitCallback(
            url = request.url,
            continuation = continuation,
            callback = callback,
        )

        continuation.invokeOnCancellation {
            awaitCallback.unregister()
        }

        awaitCallback.register()
        FDownloader.addTask(request)
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
                    FDownloader.unregisterCallback(this)
                    continuation.resume(Result.success(info.file))
                }
                is IDownloadInfo.Error -> {
                    FDownloader.unregisterCallback(this)
                    continuation.resume(Result.failure(info.exception))
                }
                else -> {}
            }
        }
    }
}