package com.sd.lib.downloader

import java.io.File

interface IDownloader {
    /**
     * 添加回调对象，可以监听所有的下载任务
     */
    fun addCallback(callback: Callback)

    /**
     * 移除回调对象
     */
    fun removeCallback(callback: Callback)

    /**
     * 获取[url]对应的下载文件
     * @return null-不存在；不为null-存在
     */
    fun getDownloadFile(url: String?): File?

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
    fun hasTask(url: String?): Boolean

    /**
     * 添加下载任务
     * @return true-任务添加成功或者已经添加
     */
    fun addTask(url: String?): Boolean

    /**
     * 添加下载任务
     * @return true-任务添加成功或者已经添加
     */
    fun addTask(request: DownloadRequest): Boolean

    /**
     * 取消下载任务
     * @return true-任务被取消
     */
    fun cancelTask(url: String?): Boolean

    /**
     * 添加下载任务
     */
    suspend fun awaitTask(
        url: String,
        callback: Callback? = null,
    ): Result<File>

    /**
     * 添加下载任务
     */
    suspend fun awaitTask(
        request: DownloadRequest,
        callback: Callback? = null,
    ): Result<File>

    /**
     * 下载回调
     */
    interface Callback {
        fun onDownloadInfo(info: IDownloadInfo)
    }
}