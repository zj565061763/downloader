package com.sd.lib.downloader

/**
 * 注册
 */
fun Downloader.Callback.register() {
  FDownloader.registerCallback(this)
}

/**
 * 取消注册
 */
fun Downloader.Callback.unregister() {
  FDownloader.unregisterCallback(this)
}

/**
 * 下载回调
 */
abstract class DownloadCallback : Downloader.Callback {
  final override fun onDownloadInfo(info: IDownloadInfo) {
    when (info) {
      is IDownloadInfo.Initialized -> onInitialized(info)
      is IDownloadInfo.Progress -> onProgress(info)
      is IDownloadInfo.Success -> onSuccess(info)
      is IDownloadInfo.Error -> onError(info)
    }
  }

  protected open fun onInitialized(info: IDownloadInfo.Initialized) = Unit
  protected open fun onProgress(info: IDownloadInfo.Progress) = Unit
  protected open fun onSuccess(info: IDownloadInfo.Success) = Unit
  protected open fun onError(info: IDownloadInfo.Error) = Unit
}

/**
 * 把当前回调对象转为监听[url]的回调对象，回调对象会在[url]任务结束后自动被移除
 */
fun Downloader.Callback.withUrl(url: String): Downloader.Callback {
  val callback = this
  return if (callback is UrlCallback) {
    callback.takeIf { it.url == url } ?: UrlCallback(url, callback.callback)
  } else {
    UrlCallback(url, callback)
  }
}

private class UrlCallback(
  val url: String,
  val callback: Downloader.Callback,
) : Downloader.Callback {

  init {
    require(callback !is UrlCallback)
  }

  override fun onDownloadInfo(info: IDownloadInfo) {
    if (info.url == url) {
      callback.onDownloadInfo(info)
      when (info) {
        is IDownloadInfo.Success,
        is IDownloadInfo.Error,
          -> this@UrlCallback.unregister()
        else -> {}
      }
    }
  }
}