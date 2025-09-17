package com.sd.lib.downloader

interface IDownloader {
  /**
   * 注册回调对象，监听所有下载任务
   */
  fun registerCallback(callback: Callback)

  /**
   * 取消注册
   */
  fun unregisterCallback(callback: Callback)

  /**
   * 删除所有临时文件（下载中的临时文件不会被删除）
   */
  fun deleteTempFile()

  /**
   * 删除所有下载文件（临时文件不会被删除）
   */
  fun deleteDownloadFile()

  /**
   * 根据扩展名[extension]删除下载文件（临时文件不会被删除）
   * @param extension 文件扩展名(例如mp3)，如果为空-删除没有扩展名的文件
   */
  fun deleteDownloadFileWithExtension(extension: String)

  /**
   * 是否有[url]对应的下载任务
   */
  fun hasTask(url: String): Boolean

  /**
   * 添加下载任务
   * @return true-任务添加成功或者已经添加
   */
  fun addTask(url: String): Boolean

  /**
   * 添加下载任务
   * @return true-任务添加成功或者已经添加
   */
  fun addTask(request: DownloadRequest): Boolean

  /**
   * 取消下载任务
   */
  fun cancelTask(url: String)

  interface Callback {
    /** 主线程回调 */
    fun onDownloadInfo(info: IDownloadInfo)
  }
}