package com.sd.lib.downloader

import android.content.Context
import com.sd.lib.downloader.executor.DefaultDownloadExecutor
import com.sd.lib.downloader.executor.IDownloadExecutor
import java.io.File

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
            externalCacheDir ?: cacheDir ?: error("cache dir is unavailable")
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
         * 调试模式
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
         * 返回配置
         */
        @JvmStatic
        fun get(): DownloaderConfig {
            val config = sConfig
            if (config != null) return config
            synchronized(this@Companion) {
                return sConfig ?: error("DownloaderConfig has not been initialized")
            }
        }
    }
}