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

# 下载

```kotlin
private fun startDownload() {
  // 构建下载请求
  val request = DownloadRequest.Builder()
    /** 是否优先使用断点下载，默认跟随[DownloadExecutor]配置 */
    .setPreferBreakpoint(true)
    /** 连接超时时间（毫秒），默认10秒 */
    .setConnectTimeout(10_000)
    /** 下载进度通知策略，进度每增加1，通知进度回调 */
    .setProgressNotifyStrategy(DownloadProgressNotifyStrategy.WhenProgressIncreased(increased = 1f))
    /** 下载文件要保存的目录，默认空表示初始化设置的下载根目录 */
    .setDirname(_dirname)
    /** 下载地址 */
    .build(_downloadUrl)

  // 添加下载任务
  FDownloader.addTask(request)
}
```

# 接口

```kotlin
interface Downloader {
  /** 注册回调对象，监听所有下载任务 */
  fun registerCallback(callback: Callback)

  /** 取消注册 */
  fun unregisterCallback(callback: Callback)

  /** 获取[url]对应的下载文件，如果文件不存在则返回null */
  fun getDownloadFile(url: String, dirname: String = ""): File?

  /** 访问下载目录 */
  fun <T> downloadDir(block: DownloadDirScope.(dir: File) -> T): T

  /** 获取[url]对应的下载信息 */
  fun getDownloadInfo(url: String): AccessibleDownloadInfo?

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
  /**
   * 删除指定目录[dirname]下的所有直接子级下载文件（不含临时文件），
   * 如果[dirname]为空，则为下载根目录
   */
  fun deleteAllDownloadFile(dirname: String)
}
```