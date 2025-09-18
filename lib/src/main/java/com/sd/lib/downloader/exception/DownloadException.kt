package com.sd.lib.downloader.exception

import android.content.Context
import com.sd.lib.downloader.R

open class DownloadException @JvmOverloads constructor(
  message: String? = null,
  cause: Throwable? = null,
) : Exception(message, cause) {

  /** 异常描述 */
  fun getDesc(context: Context): String {
    return buildString {
      val formatMessage = formatMessage(context).ifEmpty { this@DownloadException.javaClass.simpleName }
      val formatCause = formatCause(context)
      append(formatMessage)
      if (formatMessage.isNotEmpty() && formatCause.isNotEmpty()) append(" ")
      append(formatCause)
    }
  }

  /** 异常信息 */
  protected open fun formatMessage(context: Context): String {
    return message ?: ""
  }

  /** 异常原因 */
  protected open fun formatCause(context: Context): String {
    return cause?.toString() ?: ""
  }

  companion object {
    internal fun wrap(e: Throwable): DownloadException {
      return if (e is DownloadException) e else DownloadException(cause = e)
    }
  }
}

/** 非法的请求（下载地址为空） */
class DownloadExceptionIllegalRequestEmptyUrl internal constructor() : DownloadException() {
  override fun formatMessage(context: Context): String {
    return context.getString(R.string.lib_downloader_ExceptionIllegalRequestEmptyUrl)
  }
}

/** 创建临时文件异常 */
class DownloadExceptionCreateTempFile internal constructor() : DownloadException() {
  override fun formatMessage(context: Context): String {
    return context.getString(R.string.lib_downloader_ExceptionCreateTempFile)
  }
}

/** 提交任务异常 */
class DownloadExceptionSubmitTask internal constructor(cause: Throwable) : DownloadException(cause = cause) {
  override fun formatMessage(context: Context): String {
    return context.getString(R.string.lib_downloader_ExceptionSubmitTask)
  }
}

/** 临时文件未找到 */
class DownloadExceptionTempFileNotFound internal constructor() : DownloadException() {
  override fun formatMessage(context: Context): String {
    return context.getString(R.string.lib_downloader_ExceptionTempFileNotFound)
  }
}

/** 创建下载文件异常 */
class DownloadExceptionCreateDownloadFile internal constructor() : DownloadException() {
  override fun formatMessage(context: Context): String {
    return context.getString(R.string.lib_downloader_ExceptionCreateDownloadFile)
  }
}

/** 重命名文件异常 */
class DownloadExceptionRenameFile internal constructor() : DownloadException() {
  override fun formatMessage(context: Context): String {
    return context.getString(R.string.lib_downloader_ExceptionRenameFile)
  }
}

/** 取消下载 */
class DownloadExceptionCancellation internal constructor() : DownloadException()