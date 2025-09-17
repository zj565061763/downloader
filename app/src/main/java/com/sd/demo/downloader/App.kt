package com.sd.demo.downloader

import android.app.Application
import com.sd.lib.downloader.DownloaderConfig

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    DownloaderConfig.init(
      DownloaderConfig.Builder()

        // 设置是否输出日志（tag：FDownloader），默认：false
        .setDebug(true)

        .build(this)
    )
  }
}