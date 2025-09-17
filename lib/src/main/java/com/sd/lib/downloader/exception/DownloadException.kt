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