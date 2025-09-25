package com.sd.lib.downloader

import java.io.File

interface Downloader {
  /** 注册回调对象，监听所有下载任务 */
  fun registerCallback(callback: DownloadInfoCallback)

  /** 取消注册 */
  fun unregisterCallback(callback: DownloadInfoCallback)

  /** 获取[url]对应的下载文件，如果文件不存在则返回null */
  fun getDownloadFile(url: String): File?

  /** 访问下载目录 */
  fun <T> downloadDir(block: DownloadDirScope.() -> T): T

  /** 获取[url]对应的下载信息 */
  fun getDownloadInfo(url: String): AccessibleDownloadInfo?

  /**
   * 添加下载任务
   * @return true-任务添加成功或者已经添加
   */
  fun addTask(request: DownloadRequest): Boolean

  /** 取消下载任务 */
  fun cancelTask(url: String)

  companion object {
    /**
     * 获取子目录名称为[name]的下载器，
     * 如果[name]为空，则默认子目录为:default
     */
    @JvmStatic
    fun dirname(name: String): Downloader {
      return DownloaderImpl(dirname = name.ifEmpty { "default" })
    }
  }
}

fun interface DownloadInfoCallback {
  /** 主线程回调 */
  fun onDownloadInfo(info: DownloadInfo)
}

private class DownloaderImpl(
  private val dirname: String,
) : Downloader {
  override fun registerCallback(callback: DownloadInfoCallback) {
    FDownloader.registerCallback(callback)
  }

  override fun unregisterCallback(callback: DownloadInfoCallback) {
    FDownloader.unregisterCallback(callback)
  }

  override fun getDownloadFile(url: String): File? {
    return FDownloader.getDownloadFile(dirname = dirname, url = url)
  }

  override fun <T> downloadDir(block: DownloadDirScope.() -> T): T {
    return FDownloader.downloadDir {
      DownloadDirScopeImpl(dirname = dirname, dir = this).block()
    }
  }

  override fun getDownloadInfo(url: String): AccessibleDownloadInfo? {
    return FDownloader.getDownloadInfo(url)
  }

  override fun addTask(request: DownloadRequest): Boolean {
    return FDownloader.addTask(dirname = dirname, request = request)
  }

  override fun cancelTask(url: String) {
    FDownloader.cancelTask(url)
  }

  init {
    require(dirname.isNotEmpty()) { "dirname is empty." }
  }
}