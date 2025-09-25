package com.sd.lib.downloader

interface DownloadDirScope {
  /** 删除子目录下的所有临时文件（不含下载中的临时文件），并返回删除的个数 */
  fun deleteAllTempFile(): Int

  /** 删除子目录下的所有下载文件（不含临时文件），并返回删除的个数 */
  fun deleteAllDownloadFile(): Int
}

internal class DownloadDirScopeImpl(
  private val dirname: String,
  private val dir: DownloadDir,
) : DownloadDirScope {
  override fun deleteAllTempFile(): Int {
    return dir.tempFiles(dirname = dirname) { files ->
      var count = 0
      files.forEach { if (it.delete()) count++ }
      count
    }
  }

  override fun deleteAllDownloadFile(): Int {
    return dir.files(dirname = dirname) { files ->
      var count = 0
      files.forEach { if (it.delete()) count++ }
      count
    }
  }
}