package com.sd.lib.downloader

interface DownloadDirScope {
  /** 删除指定目录[dirname]下的所有下载文件（不含临时文件） */
  fun deleteAllDownloadFile(dirname: String = "")
}

internal class DownloadDirScopeImpl(
  private val dir: DownloadDir,
) : DownloadDirScope {
  override fun deleteAllDownloadFile(dirname: String) {
    return dir.files(dirname = dirname) { files ->
      files.forEach { it.deleteRecursively() }
    }
  }
}