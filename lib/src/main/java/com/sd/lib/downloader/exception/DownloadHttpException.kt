package com.sd.lib.downloader.exception

open class DownloadHttpException(
    message: String? = null,
    cause: Throwable? = null,
) : DownloadException(message, cause)