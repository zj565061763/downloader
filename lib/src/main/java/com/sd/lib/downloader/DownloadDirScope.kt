package com.sd.lib.downloader

interface DownloadDirScope {
  /** 删除所有下载文件，并返回删除的文件个数 */
  fun deleteDownloadFiles(): Int
}

internal class DownloadDirScopeImpl(
  private val dir: DownloadDir,
) : DownloadDirScope {
  override fun deleteDownloadFiles(): Int {
    return dir.files { files ->
      var count = 0
      files.forEach { file ->
        if (file.deleteRecursively()) count++
      }
      count
    }
  }
}