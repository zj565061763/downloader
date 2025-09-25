package com.sd.demo.downloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.sd.demo.downloader.databinding.SampleDownloadBinding
import com.sd.lib.downloader.DownloadInfo
import com.sd.lib.downloader.DownloadInfoCallback
import com.sd.lib.downloader.DownloadProgressNotifyStrategy
import com.sd.lib.downloader.DownloadRequest
import com.sd.lib.downloader.Downloader
import com.sd.lib.downloader.exception.DownloadExceptionCancellation
import com.sd.lib.downloader.executor.DownloadExecutor

class SampleDownload : ComponentActivity() {
  private val _binding by lazy { SampleDownloadBinding.inflate(layoutInflater) }
  private val _downloadUrl = "https://dldir1v6.qq.com/weixin/android/weixin8063android2920_0x28003f33_arm64.apk"

  /** 获取子目录下载器 */
  private val _downloader = Downloader.dirname("apk")

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(_binding.root)
    _binding.btnStartDownload.setOnClickListener {
      startDownload()
    }
    _binding.btnCancelDownload.setOnClickListener {
      cancelDownload()
    }
    _binding.btnDeleteFiles.setOnClickListener {
      deleteFiles()
    }

    // 注册下载回调
    _downloader.registerCallback(_callback)
  }

  /** 开始下载 */
  private fun startDownload() {
    // 构建下载请求
    val request = DownloadRequest.Builder()
      /** 是否优先使用断点下载，默认跟随[DownloadExecutor]配置 */
      .setPreferBreakpoint(true)
      /** 连接超时时间（毫秒），默认10秒 */
      .setConnectTimeout(10_000)
      /** 下载进度通知策略，进度每增加1，通知进度回调 */
      .setProgressNotifyStrategy(DownloadProgressNotifyStrategy.WhenProgressIncreased(increased = 1f))
      /** 下载地址 */
      .build(_downloadUrl)

    // 添加下载任务
    _downloader.addTask(request)
  }

  /** 取消下载任务 */
  private fun cancelDownload() {
    _downloader.cancelTask(_downloadUrl)
  }

  /** 下载回调 */
  private val _callback = object : DownloadInfoCallback {
    override fun onDownloadInfo(info: DownloadInfo) {
      updateDownloadInfo(info)
      when (info) {
        is DownloadInfo.Error -> logMsg { "$info desc:${info.exception.getDesc(this@SampleDownload)}" }
        else -> logMsg { info.toString() }
      }
    }
  }

  /** 更新下载信息 */
  private fun updateDownloadInfo(info: DownloadInfo) {
    when (info) {
      is DownloadInfo.Initialized -> "开始下载"
      is DownloadInfo.Progress -> "${info.progress.toInt()}%"
      is DownloadInfo.Cancelling -> "取消中..."
      is DownloadInfo.Error -> {
        when (val exception = info.exception) {
          is DownloadExceptionCancellation -> "已取消下载"
          else -> "下载失败:${exception.getDesc(this@SampleDownload)}"
        }
      }
      is DownloadInfo.Success -> "下载成功:${info.file}"
    }.also { text ->
      _binding.tvProgress.text = text
    }
  }

  private fun deleteFiles() {
    _downloader.downloadDir {
      // 删除子目录下的所有临时文件（不含下载中的临时文件），并返回删除的个数
      deleteAllTempFile().also { count ->
        logMsg { "deleteAllTempFile count:$count" }
      }

      // 删除子目录下的所有下载文件（不含临时文件），并返回删除的个数
      deleteAllDownloadFile().also { count ->
        logMsg { "deleteAllDownloadFile count:$count" }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    // 取消注册下载回调
    _downloader.unregisterCallback(_callback)
    cancelDownload()
    deleteFiles()
  }
}