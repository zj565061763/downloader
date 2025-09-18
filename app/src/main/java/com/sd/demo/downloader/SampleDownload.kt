package com.sd.demo.downloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.sd.demo.downloader.databinding.SampleDownloadBinding
import com.sd.lib.downloader.DownloadInfo
import com.sd.lib.downloader.DownloadProgressNotifyStrategy
import com.sd.lib.downloader.DownloadRequest
import com.sd.lib.downloader.Downloader
import com.sd.lib.downloader.FDownloader

class SampleDownload : ComponentActivity() {
  private val _binding by lazy { SampleDownloadBinding.inflate(layoutInflater) }
  private val url = "https://dldir1v6.qq.com/weixin/android/weixin8063android2920_0x28003f33_arm64.apk"

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

  /**
   * 开始下载
   */
  private fun startDownload() {
    // 构建下载请求
    val request = DownloadRequest.Builder()
      // true-优先断点下载；false-不使用断点下载；null-跟随初始化配置
      .setPreferBreakpoint(true)
      // 设置下载进度通知策略
      .setProgressNotifyStrategy(DownloadProgressNotifyStrategy.WhenProgressIncreased(increased = 1f))
      // 下载地址
      .build(url)

    // 添加下载任务
    FDownloader.addTask(request)
  }

  /**
   * 取消下载
   */
  private fun cancelDownload() {
    // 取消下载任务
    FDownloader.cancelTask(url)
  }

  /**
   * 下载回调
   */
  private val _downloadCallback = object : Downloader.Callback {
    override fun onDownloadInfo(info: DownloadInfo) {
      when (info) {
        is DownloadInfo.Error -> logMsg {
          "$info desc:${info.exception.getDesc(this@SampleDownload)}"
        }
        else -> logMsg { info.toString() }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    cancelDownload()
    // 取消注册下载回调
    FDownloader.unregisterCallback(_downloadCallback)
  }
}