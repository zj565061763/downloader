package com.sd.demo.downloader

import android.app.Application
import com.sd.lib.downloader.DownloaderConfig

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    DownloaderConfig.init(
      DownloaderConfig.Builder()
        .setDebug(true)
        .build(this)
    )
  }
}