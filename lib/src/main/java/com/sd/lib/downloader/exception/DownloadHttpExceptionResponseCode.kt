package com.sd.lib.downloader.exception

/**
 * Http返回吗异常
 */
class DownloadHttpExceptionResponseCode(
  val code: Int,
) : DownloadHttpException()