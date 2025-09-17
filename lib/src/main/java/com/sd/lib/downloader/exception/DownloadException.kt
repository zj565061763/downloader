package com.sd.lib.downloader.exception

open class DownloadException @JvmOverloads constructor(
  message: String? = null,
  cause: Throwable? = null,
) : Exception(message, cause) {

  /** 异常描述 */
  val desc: String
    get() = buildString {
      val formatMessage = formatMessage.ifEmpty { this@DownloadException.javaClass.simpleName }
      val formatCause = formatCause
      append(formatMessage)
      if (formatMessage.isNotEmpty() && formatCause.isNotEmpty()) append(" ")
      append(formatCause)
    }

  /** 异常信息 */
  protected open val formatMessage: String
    get() = message ?: ""

  /** 异常原因 */
  protected open val formatCause: String
    get() = cause?.toString() ?: ""

  override fun toString(): String {
    return desc
  }

  companion object {
    @JvmStatic
    fun wrap(e: Throwable): DownloadException {
      return if (e is DownloadException) e else DownloadException(cause = e)
    }
  }
}

/** 非法的请求 */
class DownloadExceptionIllegalRequest internal constructor(message: String) : DownloadException(message = message)

/** 创建临时文件异常 */
class DownloadExceptionCreateTempFile internal constructor() : DownloadException()

/** 提交任务异常 */
class DownloadExceptionSubmitTask internal constructor(cause: Throwable) : DownloadException(cause = cause)

/** 临时文件未找到 */
class DownloadExceptionTempFileNotFound internal constructor() : DownloadException()

/** 创建下载文件异常 */
class DownloadExceptionCreateDownloadFile internal constructor() : DownloadException()

/** 完成下载文件异常 */
class DownloadExceptionCompleteFile internal constructor() : DownloadException()

/** 取消下载 */
class DownloadExceptionCancellation internal constructor() : DownloadException()