package com.sd.lib.downloader

import com.sd.lib.downloader.exception.DownloadException
import java.io.File

sealed interface DownloadInfo {
  val url: String

  /** 初始化 */
  data class Initialized(override val url: String) : DownloadInfo, AccessibleDownloadInfo

  /** 取消中（底层实现是异步取消时才有这个状态） */
  data class Cancelling(override val url: String) : DownloadInfo, AccessibleDownloadInfo

  /** 下载中 */
  data class Progress(
    override val url: String,
    /** 总数量 */
    val total: Long,
    /** 已传输数量 */
    val current: Long,
    /** 传输进度[0-100] */
    val progress: Float,
  ) : DownloadInfo, AccessibleDownloadInfo

  /** 下载成功 */
  data class Success(
    override val url: String,
    val file: File,
  ) : DownloadInfo

  /** 下载失败 */
  data class Error(
    override val url: String,
    val exception: DownloadException,
  ) : DownloadInfo
}

sealed interface AccessibleDownloadInfo