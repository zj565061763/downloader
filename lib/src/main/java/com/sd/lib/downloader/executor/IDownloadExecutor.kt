package com.sd.lib.downloader.executor

import com.sd.lib.downloader.DownloadRequest
import java.io.File

interface IDownloadExecutor {
    /**
     * 提交下载任务
     *
     * @param request 下载请求
     * @param file    要保存的下载文件
     * @param updater 下载信息更新对象
     */
    fun submit(request: DownloadRequest, file: File, updater: Updater)

    /**
     * 取消[url]下载任务
     *
     * @return true-任务取消
     */
    fun cancel(url: String?): Boolean

    interface Updater {
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
}