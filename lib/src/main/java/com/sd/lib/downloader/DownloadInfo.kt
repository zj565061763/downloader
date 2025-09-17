package com.sd.lib.downloader

import com.sd.lib.downloader.exception.DownloadException
import java.io.File

sealed interface IDownloadInfo {
  val url: String

  /**
   * 初始化
   */
  data class Initialized(override val url: String) : IDownloadInfo

  /**
   * 下载中
   */
  data class Progress(
    override val url: String,

    /** 总数量 */
    val total: Long,

    /** 已传输数量 */
    val current: Long,

    /** 传输进度[0-100] */
    val progress: Int,

    /** 传输速率（B/S） */
    val speedBps: Int,
  ) : IDownloadInfo

  /**
   * 下载成功
   */
  data class Success(
    override val url: String,
    val file: File,
  ) : IDownloadInfo

  /**
   * 下载失败
   */
  data class Error(
    override val url: String,
    val exception: DownloadException,
  ) : IDownloadInfo
}