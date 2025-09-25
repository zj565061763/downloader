package com.sd.lib.downloader

interface DownloadDirScope {
  /** 删除子目录下的所有下载文件（不含临时文件） */
  fun deleteAllDownloadFile()
}

internal class DownloadDirScopeImpl(
  private val dirname: String,
  private val dir: DownloadDir,
) : DownloadDirScope {
  override fun deleteAllDownloadFile() {
    return dir.files(dirname = dirname) { files ->
      files.forEach { it.delete() }
    }
  }
}