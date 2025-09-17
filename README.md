[![Maven Central](https://img.shields.io/maven-central/v/io.github.zj565061763.android/downloader)](https://central.sonatype.com/search?q=g:io.github.zj565061763.android+downloader)

# Gradle

```kotlin
implementation("io.github.zj565061763.android:downloader:$version")
```

# 初始化

```kotlin
DownloaderConfig.init(
  DownloaderConfig.Builder()

    // 设置下载目录，如果为null或者不设置则默认路径为：(sd卡或者内部存储)/Android/data/包名/files/f_dir_lib_downloader
    .setDownloadDirectory(null)

    /**
     * 设置下载处理器，如果为null或者不设置则默认的下载处理器为：DefaultDownloadExecutor
     * limitedParallelism：下载中的最大任务数量，默认：3（注意这里是指下载中的数量，最大发起数量不限制）
     * preferBreakpoint：是否优先使用断点下载，默认：true
     */
    .setDownloadExecutor(DefaultDownloadExecutor(limitedParallelism = 3, preferBreakpoint = true))

    // 设置是否输出日志（tag：FDownloader），默认：false
    .setDebug(true)

    .build(this)
)
```

# 接口

```kotlin
interface Downloader {
  /**
   * 注册回调对象，监听所有下载任务
   */
  fun registerCallback(callback: Callback)

  /**
   * 取消注册
   */
  fun unregisterCallback(callback: Callback)

  /**
   * 删除所有临时文件（不含下载中的临时文件）
   */
  fun deleteTempFile()

  /**
   * 删除[block]返回true的下载文件（不含临时文件）
   */
  fun deleteDownloadFile(block: (File) -> Boolean)

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

  interface Callback {
    /** 主线程回调 */
    fun onDownloadInfo(info: DownloadInfo)
  }
}
```

```kotlin
sealed interface DownloadInfo {
  val url: String

  /**
   * 初始化
   */
  data class Initialized(override val url: String) : DownloadInfo

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
  ) : DownloadInfo

  /**
   * 下载成功
   */
  data class Success(
    override val url: String,
    val file: File,
  ) : DownloadInfo

  /**
   * 下载失败
   */
  data class Error(
    override val url: String,
    val exception: DownloadException,
  ) : DownloadInfo
}
```