package com.sd.lib.downloader

import java.io.File
import java.security.MessageDigest

internal interface DownloadDir {
  /** [key]对应的临时文件 */
  fun tempFileForKey(key: String): File?

  /** [key]对应的文件，如果key有扩展名，则使用[key]的扩展名 */
  fun fileForKey(key: String): File?

  fun existOrNullFileForKey(key: String): File?

  /** 访问所有非临时文件 */
  fun <T> files(block: (List<File>) -> T): T

  companion object {
    fun get(dir: File): DownloadDir {
      return DownloadDirImpl(dir = dir)
    }
  }
}

/** 临时文件扩展名 */
private const val ExtTemp = "temp"

private class DownloadDirImpl(dir: File) : DownloadDir {
  private val _dir = dir

  override fun tempFileForKey(key: String): File? {
    return newFileForKey(
      key = key,
      ext = ExtTemp,
    )
  }

  override fun fileForKey(key: String): File? {
    return newFileForKey(
      key = key,
      ext = key.substringAfterLast(".", ""),
    )
  }

  override fun existOrNullFileForKey(key: String): File? {
    return newFileForKey(
      key = key,
      ext = key.substringAfterLast(".", ""),
      checkExist = true,
    )
  }

  override fun <T> files(block: (List<File>) -> T): T {
    return listFiles { files ->
      block(files.filter { it.extension != ExtTemp })
    }
  }

  private fun <T> listFiles(block: (files: Array<File>) -> T): T {
    return modify { dir ->
      block(dir?.listFiles() ?: emptyArray())
    }
  }

  private fun newFileForKey(
    key: String,
    ext: String,
    checkExist: Boolean = false,
  ): File? {
    val dotExt = ext.takeIf { it.isEmpty() || it.startsWith(".") } ?: ".$ext"
    return modify { dir ->
      val file = dir?.resolve(fMd5(key) + dotExt)
      if (checkExist) file?.takeIf { it.isFile } else file
    }
  }

  private fun <T> modify(block: (dir: File?) -> T): T {
    synchronized(this@DownloadDirImpl) {
      return block(_dir.takeIf { it.fMakeDirs() })
    }
  }
}

private fun File.fMakeDirs(): Boolean {
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