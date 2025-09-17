package com.sd.lib.downloader.executor

import com.sd.lib.downloader.DownloadRequest
import java.io.File
import java.util.concurrent.CancellationException

interface DownloadExecutor {
  /**
   * 提交下载任务
   * @param request 下载请求
   * @param file    要保存的下载文件
   * @param updater 下载信息更新对象
   */
  @Throws(Throwable::class)
  fun submit(
    request: DownloadRequest,
    file: File,
    updater: Updater,
  )

  /**
   * 取消[url]下载任务
   */
  fun cancel(url: String)

  interface Updater {
    /**
     * 通知下载进度
     * @param total   总数量
     * @param current 已传输数量
     */
    fun notifyProgress(total: Long, current: Long)

    /**
     * 通知下载成功
     */
    fun notifySuccess()

    /**
     * 通知下载错误，如果[e]是[CancellationException]，表示取消下载
     */
    fun notifyError(e: Throwable)
  }
}