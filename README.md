# Gradle

[![](https://jitpack.io/v/zj565061763/downloader.svg)](https://jitpack.io/#zj565061763/downloader)

# 初始化

```kotlin
DownloaderConfig.init(
    DownloaderConfig.Builder()

        // 设置下载目录，如果为null或者不设置则默认路径为：(sd卡或者内部存储)/Android/data/包名/cache/f_dir_lib_downloader
        .setDownloadDirectory(null)

        /**
         * 设置下载处理器，如果为null或者不设置则默认的下载处理器为：DefaultDownloadExecutor
         * limitedParallelism：下载中的最大任务数量，默认：3（注意这里是指下载中的数量，最大发起数量不限制）
         * preferBreakpoint：是否优先使用断点下载，默认：false
         */
        .setDownloadExecutor(
            DefaultDownloadExecutor(limitedParallelism = 3, preferBreakpoint = false)
        )

        // 设置是否输出日志（tag：FDownloader），默认：false
        .setDebug(true)

        .build(this)
)
```

# 接口

```kotlin
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
        fun onDownloadInfo(info: IDownloadInfo)
    }
}
```

```kotlin
sealed interface IDownloadInfo {
    val url: String

    /**
     * 初始化
     */
    data class Initialized(override val url: String) : IDownloadInfo

    /**
     * 下载中
     */
    data class Progress(
        override val url: String,

        /** 总数量 */
        val total: Long,

        /** 已传输数量 */
        val current: Long,

        /** 传输进度[0-100] */
        val progress: Int,

        /** 传输速率（B/S） */
        val speedBps: Int,
    ) : IDownloadInfo

    /**
     * 下载成功
     */
    data class Success(
        override val url: String,
        val file: File,
    ) : IDownloadInfo

    /**
     * 下载失败
     */
    data class Error(
        override val url: String,
        val exception: DownloadException,
    ) : IDownloadInfo
}
```