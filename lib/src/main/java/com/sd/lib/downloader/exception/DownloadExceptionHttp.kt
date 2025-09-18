package com.sd.lib.downloader.exception

import android.content.Context
import com.sd.lib.downloader.R
import java.net.SocketTimeoutException

open class DownloadExceptionHttp(
  message: String? = null,
  cause: Throwable? = null,
) : DownloadException(message, cause) {
  override fun formatCause(context: Context): String {
    return when (cause) {
      is SocketTimeoutException -> context.getString(R.string.lib_downloader_ExceptionHttp_SocketTimeoutException)
      else -> super.formatCause(context)
    }
  }
}

class DownloadExceptionHttpResponseCode(val code: Int) : DownloadExceptionHttp() {
  override fun formatMessage(context: Context): String {
    return buildString {
      append(context.getString(R.string.lib_downloader_ExceptionHttpResponseCode))
      append("(").append(code).append(")")
    }
  }
}