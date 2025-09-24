package com.sd.lib.downloader

class DownloadRequest private constructor(builder: Builder) {
  /** 下载地址 */
  val url: String

  /** 是否需要断点下载 */
  val preferBreakpoint: Boolean?

  /** 连接超时时间（毫秒） */
  val connectTimeout: Long

  /** 下载进度通知策略 */
  val progressNotifyStrategy: DownloadProgressNotifyStrategy

  /** 下载文件要保存的目录 */
  val dirname: String

  init {
    this.url = builder.url
    this.preferBreakpoint = builder.preferBreakpoint
    this.connectTimeout = builder.connectTimeout ?: DownloaderConfig.get().connectionTimeout
    this.progressNotifyStrategy = builder.progressNotifyStrategy ?: DownloaderConfig.get().progressNotifyStrategy
    this.dirname = builder.dirname
  }

  class Builder {
    internal lateinit var url: String
      private set

    internal var preferBreakpoint: Boolean? = null
      private set

    internal var connectTimeout: Long? = null
      private set

    internal var progressNotifyStrategy: DownloadProgressNotifyStrategy? = null
      private set

    internal var dirname: String = ""
      private set

    /** 是否优先使用断点下载 */
    fun setPreferBreakpoint(preferBreakpoint: Boolean) = apply {
      this.preferBreakpoint = preferBreakpoint
    }

    /** 连接超时时间（毫秒） */
    fun setConnectTimeout(timeout: Long) = apply {
      require(timeout > 0)
      this.connectTimeout = timeout
    }

    /** 下载进度通知策略 */
    fun setProgressNotifyStrategy(strategy: DownloadProgressNotifyStrategy) = apply {
      this.progressNotifyStrategy = strategy
    }

    /** 下载文件要保存的目录 */
    fun setDirname(dirname: String) = apply {
      this.dirname = dirname.trim()
    }

    fun build(url: String): DownloadRequest {
      this.url = url
      return DownloadRequest(this)
    }
  }
}