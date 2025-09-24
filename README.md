[![Maven Central](https://img.shields.io/maven-central/v/io.github.zj565061763.android/downloader)](https://central.sonatype.com/search?q=g:io.github.zj565061763.android+downloader)

# Gradle

```kotlin
implementation("io.github.zj565061763.android:downloader:$version")
```

# 初始化

```kotlin
DownloaderConfig.init(
  DownloaderConfig.Builder()
    // 设置是否输出日志（tag：FDownloader），默认：false
    .setDebug(true)
    .build(this)
)
```

# 接口

```kotlin
interface Downloader {
  /** 注册回调对象，监听所有下载任务 */
  fun registerCallback(callback: Callback)

  /** 取消注册 */
  fun unregisterCallback(callback: Callback)

  /** 获取[url]对应的下载文件，如果文件不存在则返回null */
  fun getDownloadFile(url: String): File?

  /** 访问下载目录 */
  fun <T> downloadDir(block: DownloadDirScope.() -> T): T

  /** 获取[url]对应的下载信息 */
  fun getDownloadInfo(url: String): AccessibleDownloadInfo?

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

  /** 取消下载任务 */
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

  /** 初始化 */
  data class Initialized(override val url: String) : DownloadInfo, AccessibleDownloadInfo

  /** 取消中（底层实现是异步取消时才有这个状态） */
  data class Cancelling(override val url: String) : DownloadInfo, AccessibleDownloadInfo

  /** 下载中 */
  data class Progress(
    override val url: String,
    /** 总数量 */
    val total: Long,
    /** 已传输数量 */
    val current: Long,
    /** 传输进度[0-100] */
    val progress: Float,
  ) : DownloadInfo, AccessibleDownloadInfo

  /** 下载成功 */
  data class Success(
    override val url: String,
    val file: File,
  ) : DownloadInfo

  /** 下载失败 */
  data class Error(
    override val url: String,
    val exception: DownloadException,
  ) : DownloadInfo
}

sealed interface AccessibleDownloadInfo
```

```kotlin
interface DownloadDirScope {
  /** 删除所有下载文件（不含临时文件），并返回删除的文件个数 */
  fun deleteDownloadFiles(): Int

  /** 删除[url]对应的下载文件，并返回本次调用是否删除了文件 */
  fun deleteDownloadFile(url: String): Boolean
}
```