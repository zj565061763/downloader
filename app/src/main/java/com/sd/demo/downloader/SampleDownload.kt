package com.sd.demo.downloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.sd.demo.downloader.databinding.SampleDownloadBinding
import com.sd.lib.downloader.DownloadCallback
import com.sd.lib.downloader.DownloadRequest
import com.sd.lib.downloader.FDownloader
import com.sd.lib.downloader.IDownloadInfo
import com.sd.lib.downloader.register
import com.sd.lib.downloader.unregister

class SampleDownload : ComponentActivity() {
    private val _binding by lazy { SampleDownloadBinding.inflate(layoutInflater) }
    private val url = "https://dldir1.qq.com/weixin/Windows/WeChatSetup.exe"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(_binding.root)
        _binding.btnStartDownload.setOnClickListener { startDownload() }
        _binding.btnCancelDownload.setOnClickListener { cancelDownload() }

        // 注册下载回调
        _downloadCallback.register()
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
    private val _downloadCallback = object : DownloadCallback() {
        override fun onInitialized(info: IDownloadInfo.Initialized) {
            logMsg { "callback onInitialized" }
        }

        override fun onProgress(info: IDownloadInfo.Progress) {
            logMsg { "callback onProgress ${info.progress}" }
        }

        override fun onSuccess(info: IDownloadInfo.Success) {
            logMsg { "callback onSuccess file:${info.file.absolutePath}" }
        }

        override fun onError(info: IDownloadInfo.Error) {
            logMsg { "callback onError ${info.exception}" }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelDownload()

        // 取消注册下载回调
        _downloadCallback.unregister()

        // 删除所有临时文件（下载中的临时文件不会被删除）
        FDownloader.deleteTempFile()

        // 删除下载文件（临时文件不会被删除）
        FDownloader.deleteDownloadFile(null)
    }
}