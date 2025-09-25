package com.sd.lib.downloader

interface DownloadDirScope {
  /** 删除子目录下的所有下载文件（不含临时文件） */
  fun deleteAllDownloadFile()

  /** 删除子目录下的所有临时文件（不含下载中的临时文件） */
  fun deleteAllTempFile()
}

internal class DownloadDirScopeImpl(
  private val dirname: String,
  private val dir: DownloadDir,
) : DownloadDirScope {
  override fun deleteAllDownloadFile() {
    dir.files(dirname = dirname) { files ->
      files.forEach { it.delete() }
    }
  }

  override fun deleteAllTempFile() {
    dir.tempFiles(dirname = dirname) { files ->
      files.forEach { it.delete() }
    }
  }
}