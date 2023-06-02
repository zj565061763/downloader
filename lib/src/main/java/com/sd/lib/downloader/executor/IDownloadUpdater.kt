package com.sd.lib.downloader.executor

interface IDownloadUpdater {
    /**
     * 通知下载进度
     *
     * @param total   总数量
     * @param current 已传输数量
     */
    fun notifyProgress(total: Long, current: Long)

    /**
     * 通知下载成功
     */
    fun notifySuccess()

    /**
     * 通知下载错误
     */
    fun notifyError(t: Throwable)

    /**
     * 通知下载被取消
     */
    fun notifyCancel()
}