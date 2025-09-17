package com.sd.lib.downloader

import java.io.File
import java.security.MessageDigest

internal interface DownloadDir {
  /**
   * 获取[key]对应的临时文件，扩展名[TEMP_EXT]
   */
  fun getTempFileForKey(key: String): File?

  /**
   * 获取[key]对应的文件，如果key有扩展名，则返回的文件名包括[key]的扩展名
   */
  fun getFileForKey(key: String): File?

  /**
   * 删除当前目录下的临时文件(扩展名为[TEMP_EXT])
   * @param block 遍历临时文件，返回true则删除该文件
   * @return 返回删除的文件数量
   */
  fun deleteTempFile(block: ((File) -> Boolean)? = null): Int

  /**
   * 删除当前目录下的文件，临时文件(扩展名为[TEMP_EXT])不会被删除
   * @param block 遍历文件，返回true则删除该文件
   * @return 返回删除的文件数量
   */
  fun deleteFile(block: ((File) -> Boolean)? = null): Int

  companion object {
    fun get(dir: File): DownloadDir {
      return DownloadDirImpl(dir = dir)
    }
  }
}

/** 临时文件扩展名 */
private const val TEMP_EXT = "temp"

private class DownloadDirImpl(dir: File) : DownloadDir {
  private val _dir = dir

  override fun getTempFileForKey(key: String): File? {
    return newFileForKey(
      key = key,
      ext = TEMP_EXT,
    )
  }

  override fun getFileForKey(key: String): File? {
    return newFileForKey(
      key = key,
      ext = key.substringAfterLast(".", ""),
    )
  }

  override fun deleteTempFile(block: ((File) -> Boolean)?): Int {
    return listFiles { files ->
      var count = 0
      for (item in files) {
        if (item.extension != TEMP_EXT) continue
        if (block == null || block(item)) {
          if (item.deleteRecursively()) count++
        }
      }
      count
    }
  }

  override fun deleteFile(block: ((File) -> Boolean)?): Int {
    return listFiles { files ->
      var count = 0
      for (item in files) {
        if (item.extension == TEMP_EXT) continue
        if (block == null || block(item)) {
          if (item.deleteRecursively()) count++
        }
      }
      count
    }
  }

  private fun <T> listFiles(block: (files: Array<File>) -> T): T {
    return modify {
      val files = it?.listFiles() ?: emptyArray()
      block(files)
    }
  }

  private fun <T> modify(block: (dir: File?) -> T): T {
    synchronized(this@DownloadDirImpl) {
      val directory = if (_dir.fMakeDirs()) _dir else null
      return block(directory)
    }
  }

  private fun newFileForKey(key: String, ext: String): File? {
    val dotExt = ext.takeIf { it.isEmpty() || it.startsWith(".") } ?: ".$ext"
    return modify { dir ->
      dir?.resolve(fMd5(key) + dotExt)
    }
  }
}

private fun File?.fMakeDirs(): Boolean {
  if (this == null) return false
  if (this.isDirectory) return true
  if (this.isFile) this.delete()
  return this.mkdirs()
}

private fun fMd5(input: String): String {
  val md5Bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
  return buildString {
    for (byte in md5Bytes) {
      val hex = Integer.toHexString(0xff and byte.toInt())
      if (hex.length == 1) append("0")
      append(hex)
    }
  }
}