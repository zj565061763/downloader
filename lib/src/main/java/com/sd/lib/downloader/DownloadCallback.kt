package com.sd.lib.downloader

/**
 * 注册
 */
fun IDownloader.Callback.register() {
    FDownloader.registerCallback(this)
}

/**
 * 取消注册
 */
fun IDownloader.Callback.unregister() {
    FDownloader.unregisterCallback(this)
}

fun IDownloader.Callback.withUrl(url: String): IDownloader.Callback {
    val callback = this
    return if (callback is UrlCallback) {
        callback.takeIf { it.url == url } ?: UrlCallback(url, callback.callback)
    } else {
        UrlCallback(url, callback)
    }
}

private class UrlCallback(
    val url: String,
    val callback: IDownloader.Callback,
) : IDownloader.Callback {
    override fun onDownloadInfo(info: IDownloadInfo) {
        if (info.url == url) {
            callback.onDownloadInfo(info)
            when (info) {
                is IDownloadInfo.Success,
                is IDownloadInfo.Error -> FDownloader.unregisterCallback(this)
                else -> {}
            }
        }
    }
}