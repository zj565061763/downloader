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
    val url = request.url
    return suspendCancellableCoroutine { continuation ->
        val awaitCallback = AwaitCallback(
            url = request.url,
            continuation = continuation,
            callback = callback,
        )

        continuation.invokeOnCancellation {
            FDownloader.removeCallback(awaitCallback)
        }

        if (continuation.isActive) {
            logMsg { "awaitTask url:${url}" }
            FDownloader.addCallback(awaitCallback)
            FDownloader.addTask(request)
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
                    logMsg { "awaitTask resume error url:${url} exception:${info.exception.javaClass.simpleName} ${info.exception}" }
                    FDownloader.removeCallback(this)
                    continuation.resume(Result.failure(info.exception))
                }
                else -> {}
            }
        }
    }
}