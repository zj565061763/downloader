package com.sd.lib.downloader

class DownloadRequest private constructor(builder: Builder) {
    /** 下载地址 */
    val url: String

    /** 是否需要断点下载 */
    val preferBreakpoint: Boolean?

    init {
        this.url = builder.url
        this.preferBreakpoint = builder.preferBreakpoint
    }

    class Builder {
        internal lateinit var url: String
            private set

        internal var preferBreakpoint: Boolean? = null
            private set

        /**
         * 设置是否需要断点下载
         * @param preferBreakpoint true-是；false-否；null-跟随默认配置
         */
        fun setPreferBreakpoint(preferBreakpoint: Boolean?) = apply {
            this.preferBreakpoint = preferBreakpoint
        }

        fun build(url: String): DownloadRequest {
            this.url = url
            return DownloadRequest(this)
        }
    }
}