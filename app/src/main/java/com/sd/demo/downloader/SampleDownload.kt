package com.sd.demo.downloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.sd.demo.downloader.databinding.SampleDownloadBinding
import com.sd.lib.downloader.DownloadRequest
import com.sd.lib.downloader.FDownloader
import com.sd.lib.downloader.IDownloadInfo
import com.sd.lib.downloader.IDownloader

class SampleDownload : ComponentActivity() {
    private val _binding by lazy { SampleDownloadBinding.inflate(layoutInflater) }
    private val url = "https://dldir1.qq.com/weixin/Windows/WeChatSetup.exe"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(_binding.root)
        _binding.btnStartDownload.setOnClickListener { startDownload() }
        _binding.btnCancelDownload.setOnClickListener { cancelDownload() }

        // 添加下载回调
        FDownloader.addCallback(_downloadCallback)
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
    private val _downloadCallback = object : IDownloader.Callback {
        override fun onDownloadInfo(info: IDownloadInfo) {
            when (info) {
                is IDownloadInfo.Initialized -> logMsg { "callback Initialized" }
                is IDownloadInfo.Progress -> logMsg { "callback Progress ${info.progress}" }
                is IDownloadInfo.Success -> logMsg { "callback Success file:${info.file.absolutePath}" }
                is IDownloadInfo.Error -> logMsg { "callback Error ${info.exception.javaClass.name} ${info.exception}" }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelDownload()

        // 移除下载回调
        FDownloader.removeCallback(_downloadCallback)

        // 删除所有临时文件（下载中的临时文件不会被删除）
        FDownloader.deleteTempFile()

        // 删除下载文件（临时文件不会被删除）
        FDownloader.deleteDownloadFile(null)
    }
}