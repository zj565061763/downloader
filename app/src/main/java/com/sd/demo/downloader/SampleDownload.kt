package com.sd.demo.downloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.sd.demo.downloader.databinding.SampleDownloadBinding
import com.sd.lib.downloader.DownloadInfo
import com.sd.lib.downloader.DownloadProgressNotifyStrategy
import com.sd.lib.downloader.DownloadRequest
import com.sd.lib.downloader.Downloader
import com.sd.lib.downloader.FDownloader
import com.sd.lib.downloader.exception.DownloadExceptionCancellation
import com.sd.lib.downloader.executor.DownloadExecutor

class SampleDownload : ComponentActivity() {
  private val _binding by lazy { SampleDownloadBinding.inflate(layoutInflater) }
  private val url = "https://dldir1v6.qq.com/weixin/android/weixin8063android2920_0x28003f33_arm64.apk"
  private val dirname = "apk"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(_binding.root)
    _binding.btnStartDownload.setOnClickListener {
      startDownload()
    }
    _binding.btnCancelDownload.setOnClickListener {
      cancelDownload()
    }

    // 注册下载回调
    FDownloader.registerCallback(_downloadCallback)
  }

  /** 开始下载 */
  private fun startDownload() {
    // 构建下载请求
    val request = DownloadRequest.Builder()
      /** 是否优先使用断点下载，默认跟随[DownloadExecutor]配置 */
      .setPreferBreakpoint(true)
      /** 连接超时时间（毫秒），默认10秒 */
      .setConnectTimeout(10_000)
      /** 下载进度通知策略 */
      .setProgressNotifyStrategy(DownloadProgressNotifyStrategy.WhenProgressIncreased(increased = 1f))
      /** 下载文件要保存的目录，默认空表示根目录 */
      .setDirname(dirname)
      /** 下载地址 */
      .build(url)

    // 添加下载任务
    FDownloader.addTask(request)
  }

  /** 取消下载 */
  private fun cancelDownload() {
    // 取消下载任务
    FDownloader.cancelTask(url)
  }

  /** 下载回调 */
  private val _downloadCallback = object : Downloader.Callback {
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

  override fun onDestroy() {
    super.onDestroy()
    cancelDownload()
    // 取消注册下载回调
    FDownloader.unregisterCallback(_downloadCallback)
    // 删除下载文件
    FDownloader.downloadDir {
      deleteAllDownloadFile(dirname)
    }
  }
}