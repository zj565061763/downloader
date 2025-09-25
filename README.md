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
/** 获取子目录下载器 */
private val _downloader = Downloader.dirname("apk")

/** 下载回调 */
private val _callback = object : DownloadInfoCallback {
  override fun onDownloadInfo(info: DownloadInfo) {
    // 下载信息回调
  }
}

// 注册下载回调
_downloader.registerCallback(_callback)

// 取消注册下载回调
_downloader.unregisterCallback(_callback)

/** 开始下载 */
private fun startDownload() {
  // 构建下载请求
  val request = DownloadRequest.Builder()
    // 是否优先使用断点下载，默认false
    .setPreferBreakpoint(true)
    // 连接超时时间（毫秒），默认10秒
    .setConnectTimeout(10_000)
    // 下载进度通知策略，进度每增加1，通知进度回调
    .setProgressNotifyStrategy(DownloadProgressNotifyStrategy.WhenProgressIncreased(increased = 1f))
    // 下载地址
    .build(_downloadUrl)

  // 添加下载任务
  _downloader.addTask(request)

  // 取消下载任务
  _downloader.cancelTask(_downloadUrl)
}
```

# 接口

```kotlin
interface Downloader {
  /** 注册回调对象，监听所有下载任务 */
  fun registerCallback(callback: DownloadInfoCallback)

  /** 取消注册 */
  fun unregisterCallback(callback: DownloadInfoCallback)

  /** 获取[url]对应的下载文件，如果文件不存在则返回null */
  fun getDownloadFile(url: String): File?

  /** 访问下载目录 */
  fun <T> downloadDir(block: DownloadDirScope.() -> T): T

  /** 获取[url]对应的下载信息 */
  fun getDownloadInfo(url: String): AccessibleDownloadInfo?

  /** 是否有[url]对应的下载任务 */
  fun hasTask(url: String): Boolean

  /**
   * 添加下载任务
   * @return true-任务添加成功或者已经添加
   */
  fun addTask(request: DownloadRequest): Boolean

  /** 取消下载任务 */
  fun cancelTask(url: String)

  companion object {
    /**
     * 获取子目录名称为[name]的下载器，
     * 如果[name]为空，则默认子目录为:default
     */
    @JvmStatic
    fun dirname(name: String): Downloader {
      return DownloaderImpl(dirname = name.ifEmpty { "default" })
    }
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
  /** 删除子目录下的所有临时文件（不含下载中的临时文件），并返回删除的个数 */
  fun deleteAllTempFile(): Int

  /** 删除子目录下的所有下载文件（不含临时文件），并返回删除的个数 */
  fun deleteAllDownloadFile(): Int

  /** 不删除[url]对应的下载文件，其他逻辑同[deleteAllDownloadFile] */
  fun deleteAllDownloadFileExcept(url: String): Int
}
```