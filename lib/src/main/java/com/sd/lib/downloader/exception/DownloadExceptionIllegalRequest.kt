package com.sd.lib.downloader.exception

/**
 * 非法的请求
 */
class DownloadExceptionIllegalRequest internal constructor(
    message: String,
) : DownloadException(message = message)