package com.sd.demo.downloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.sd.demo.downloader.databinding.SampleDownloadBinding
import com.sd.lib.downloader.DownloadRequest
import com.sd.lib.downloader.Downloader
import com.sd.lib.downloader.FDownloader
import com.sd.lib.downloader.IDownloadInfo

class SampleDownload : ComponentActivity() {
  private val _binding by lazy { SampleDownloadBinding.inflate(layoutInflater) }
  private val url = "https://dldir1.qq.com/weixin/Windows/WeChatSetup.exe"

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
    override fun onDownloadInfo(info: IDownloadInfo) {
      when (info) {
        is IDownloadInfo.Initialized -> logMsg { "callback onInitialized" }
        is IDownloadInfo.Progress -> logMsg { "callback onProgress ${info.progress}" }
        is IDownloadInfo.Success -> logMsg { "callback onSuccess file:${info.file.absolutePath}" }
        is IDownloadInfo.Error -> logMsg { "callback onError ${info.exception}" }
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