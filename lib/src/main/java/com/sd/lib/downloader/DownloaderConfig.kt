package com.sd.lib.downloader

import android.content.Context
import com.sd.lib.downloader.executor.DefaultDownloadExecutor
import com.sd.lib.downloader.executor.IDownloadExecutor
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 下载器配置
 */
class DownloaderConfig private constructor(builder: Builder) {
    internal val isDebug: Boolean
    internal val downloadDirectory: File
    internal val downloadExecutor: IDownloadExecutor

    init {
        this.isDebug = builder.isDebug
        this.downloadDirectory = builder.downloadDirectory ?: builder.context.run {
            getExternalFilesDir(null) ?: filesDir ?: error("files dir is unavailable")
        }.resolve("f_dir_lib_downloader")
        this.downloadExecutor = builder.downloadExecutor ?: DefaultDownloadExecutor()
    }

    class Builder {
        internal lateinit var context: Context
            private set

        internal var isDebug = false
            private set

        internal var downloadDirectory: File? = null
            private set

        internal var downloadExecutor: IDownloadExecutor? = null
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
        fun setDownloadExecutor(executor: IDownloadExecutor?) = apply {
            this.downloadExecutor = executor
        }

        fun build(context: Context): DownloaderConfig {
            this.context = context.applicationContext
            return DownloaderConfig(this)
        }
    }

    companion object {
        private val sInitFlag = AtomicBoolean(false)

        @Volatile
        private var sConfig: DownloaderConfig? = null

        /**
         * 初始化
         */
        @JvmStatic
        fun init(config: DownloaderConfig) {
            if (sInitFlag.compareAndSet(false, true)) {
                sConfig = config
            }
        }

        /**
         * 配置信息
         */
        internal fun get(): DownloaderConfig {
            return checkNotNull(sConfig) { "You should call init() before this." }
        }
    }
}