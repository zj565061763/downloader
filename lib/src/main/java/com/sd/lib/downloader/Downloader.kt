package com.sd.lib.downloader

import java.io.File

interface Downloader {
  /** 注册回调对象，监听所有下载任务 */
  fun registerCallback(callback: Callback)

  /** 取消注册 */
  fun unregisterCallback(callback: Callback)

  /** 获取[url]对应的下载文件，如果文件不存在则返回null */
  fun getDownloadFile(url: String): File?

  /** 获取所有下载文件（不含临时文件） */
  fun <T> downloadFiles(block: (List<File>) -> T): T

  /** 获取[url]对应的下载信息 */
  fun getDownloadInfo(url: String): AccessibleDownloadInfo?

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

  /** 取消下载任务 */
  fun cancelTask(url: String)

  interface Callback {
    /** 主线程回调 */
    fun onDownloadInfo(info: DownloadInfo)
  }
}