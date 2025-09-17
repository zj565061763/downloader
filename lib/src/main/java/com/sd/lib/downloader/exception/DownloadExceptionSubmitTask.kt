package com.sd.lib.downloader.exception

/**
 * 提交任务异常
 */
class DownloadExceptionSubmitTask internal constructor(
  cause: Throwable,
) : DownloadException(cause = cause)