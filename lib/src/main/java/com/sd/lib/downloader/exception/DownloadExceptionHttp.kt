package com.sd.lib.downloader.exception

import android.content.Context
import com.sd.lib.downloader.R

open class DownloadExceptionHttp(
  message: String? = null,
  cause: Throwable? = null,
) : DownloadException(message, cause)

class DownloadExceptionHttpResponseCode(val code: Int) : DownloadExceptionHttp() {
  override fun formatMessage(context: Context): String {
    return buildString {
      append(context.getString(R.string.lib_downloader_ExceptionHttpResponseCode))
      append("(").append(code).append(")")
    }
  }
}