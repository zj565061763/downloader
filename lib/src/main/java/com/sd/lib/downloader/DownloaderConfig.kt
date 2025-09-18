package com.sd.lib.downloader

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import com.sd.lib.downloader.executor.DefaultDownloadExecutor
import com.sd.lib.downloader.executor.DownloadExecutor
import java.io.File

/**
 * 下载器配置
 */
class DownloaderConfig private constructor(builder: Builder) {
  internal val isDebug: Boolean
  internal val downloadDirectory: File
  internal val downloadExecutor: DownloadExecutor
  internal val progressNotifyStrategy: DownloadProgressNotifyStrategy

  init {
    this.isDebug = builder.isDebug
    this.downloadDirectory = builder.downloadDirectory ?: builder.context.defaultDownloadDir()
    this.downloadExecutor = builder.downloadExecutor ?: DefaultDownloadExecutor()
    this.progressNotifyStrategy = builder.progressNotifyStrategy ?: DownloadProgressNotifyStrategy.WhenProgressIncreased()
  }

  class Builder {
    internal lateinit var context: Context
      private set

    internal var isDebug = false
      private set

    internal var downloadDirectory: File? = null
      private set

    internal var downloadExecutor: DownloadExecutor? = null
      private set

    internal var progressNotifyStrategy: DownloadProgressNotifyStrategy? = null
      private set

    /**
     * 调试模式(tag：FDownloader)
     */
    fun setDebug(debug: Boolean) = apply {
      this.isDebug = debug
    }

    /**
     * 下载目录
     */
    fun setDownloadDirectory(directory: File?) = apply {
      this.downloadDirectory = directory
    }

    /**
     * 下载执行器
     */
    fun setDownloadExecutor(executor: DownloadExecutor?) = apply {
      this.downloadExecutor = executor
    }

    /**
     * 下载进度通知策略
     */
    fun setProgressNotifyStrategy(strategy: DownloadProgressNotifyStrategy?) = apply {
      this.progressNotifyStrategy = strategy
    }

    fun build(context: Context): DownloaderConfig {
      this.context = context.applicationContext
      return DownloaderConfig(this)
    }
  }

  companion object {
    @Volatile
    private var sConfig: DownloaderConfig? = null

    /**
     * 初始化
     */
    @JvmStatic
    fun init(config: DownloaderConfig) {
      synchronized(this@Companion) {
        if (sConfig == null) {
          sConfig = config
        }
      }
    }

    /**
     * 配置信息
     */
    internal fun get(): DownloaderConfig {
      return sConfig ?: synchronized(this@Companion) {
        checkNotNull(sConfig) { "You should call init() before this." }
      }
    }
  }
}

sealed interface DownloadProgressNotifyStrategy {
  /** 当下载进度增加大于等于[increased]时，发起通知 */
  data class WhenProgressIncreased(
    val increased: Float = 1f,
  ) : DownloadProgressNotifyStrategy
}

private fun Context.defaultDownloadDir(): File {
  val rootDir = getExternalFilesDir(null) ?: filesDir
  return rootDir.resolve("sd.lib.downloader")
    .let { dir ->
      val process = currentProcess()
      if (process.isNullOrEmpty()) dir else dir.resolve(process.replace(":", "_"))
    }
}

private fun Context.currentProcess(): String? {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    Application.getProcessName()
  } else {
    val pid = Process.myPid()
    (getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
      ?.runningAppProcesses
      ?.firstOrNull { it.pid == pid }
      ?.processName
  }
}