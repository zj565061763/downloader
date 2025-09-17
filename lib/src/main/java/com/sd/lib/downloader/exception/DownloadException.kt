package com.sd.lib.downloader.exception

open class DownloadException @JvmOverloads constructor(
  message: String? = null,
  cause: Throwable? = null,
) : Exception(message, cause) {

  /** 异常描述 */
  val desc: String
    get() = buildString {
      val message = formatMessage ?: this@DownloadException.javaClass.simpleName
      val cause = formatCause ?: ""

      append(message)
      if (message.isNotEmpty() && cause.isNotEmpty()) {
        append(" ")
      }
      append(cause)
    }

  /** 异常信息 */
  protected open val formatMessage: String?
    get() = localizedMessage

  /** 异常原因 */
  protected open val formatCause: String?
    get() = cause?.toString()

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