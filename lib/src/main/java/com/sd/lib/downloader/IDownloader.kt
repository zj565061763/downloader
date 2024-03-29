package com.sd.lib.downloader

interface IDownloader {
    /**
     * 注册回调对象，可以监听所有的下载任务
     */
    fun registerCallback(callback: Callback)

    /**
     * 取消注册回调对象
     */
    fun unregisterCallback(callback: Callback)

    /**
     * 删除下载文件(临时文件不会被删除)
     * @param ext 文件扩展名(例如mp3)，null-删除所有下载文件
     */
    fun deleteDownloadFile(ext: String?)

    /**
     * 删除所有临时文件(下载中的临时文件不会被删除)
     */
    fun deleteTempFile()

    /**
     * 是否有[url]对应的下载任务
     */
    fun hasTask(url: String): Boolean

    /**
     * 添加下载任务
     * @return true-任务添加成功或者已经添加
     */
    fun addTask(url: String): Boolean

    /**
     * 添加下载任务
     * @return true-任务添加成功或者已经添加
     */
    fun addTask(request: DownloadRequest): Boolean

    /**
     * 取消下载任务
     */
    fun cancelTask(url: String)

    /**
     * 下载回调
     */
    interface Callback {
        /**
         * 下载信息回调(MainThread)
         */
        fun onDownloadInfo(info: IDownloadInfo)
    }
}