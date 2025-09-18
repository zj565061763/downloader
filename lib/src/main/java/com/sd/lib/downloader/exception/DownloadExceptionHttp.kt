package com.sd.lib.downloader.exception

import android.content.Context
import com.sd.lib.downloader.R
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

open class DownloadExceptionHttp(
  message: String? = null,
  cause: Throwable? = null,
) : DownloadException(message, cause) {
  override fun formatMessage(context: Context): String? {
    return when (cause) {
      is ConnectException -> ""
      is SocketException -> ""
      is SocketTimeoutException -> ""
      is UnknownHostException -> ""
      is SSLHandshakeException -> ""
      else -> super.formatMessage(context)
    }
  }

  override fun formatCause(context: Context): String {
    return when (cause) {
      is ConnectException -> context.getString(R.string.lib_downloader_ExceptionHttp_ConnectException)
      is SocketException -> context.getString(R.string.lib_downloader_ExceptionHttp_SocketException)
      is SocketTimeoutException -> context.getString(R.string.lib_downloader_ExceptionHttp_SocketTimeoutException)
      is UnknownHostException -> context.getString(R.string.lib_downloader_ExceptionHttp_UnknownHostException)
      is SSLHandshakeException -> context.getString(R.string.lib_downloader_ExceptionHttp_SSLHandshakeException)
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