package com.sd.lib.downloader

import java.io.File
import java.security.MessageDigest

/** 临时文件扩展名 */
private const val TEMP_EXT = "temp"

internal interface IDownloadDir {
    /**
     * 获取[key]对应的文件，如果key有扩展名，则返回的文件名包括[key]的扩展名
     */
    fun getKeyFile(key: String): File?

    /**
     * 获取[key]对应的临时文件，扩展名[TEMP_EXT]
     */
    fun getKeyTempFile(key: String): File?

    /**
     * 删除当前目录下的文件，临时文件(扩展名为[TEMP_EXT])不会被删除
     * @param block 遍历文件，返回true则删除该文件
     * @return 返回删除的文件数量
     */
    fun deleteFile(block: ((File) -> Boolean)? = null): Int

    /**
     * 删除当前目录下的临时文件(扩展名为[TEMP_EXT])
     * @param block 遍历临时文件，返回true则删除该文件
     * @return 返回删除的文件数量
     */
    fun deleteTempFile(block: ((File) -> Boolean)? = null): Int
}

internal class DownloadDir(dir: File) : IDownloadDir {
    private val _dir = dir

    override fun getKeyFile(key: String): File? {
        return newKeyFile(
            key = key,
            ext = key.substringAfterLast(".", ""),
        )
    }

    override fun getKeyTempFile(key: String): File? {
        return newKeyFile(
            key = key,
            ext = TEMP_EXT,
        )
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

    private fun <T> listFiles(block: (files: Array<File>) -> T): T {
        return modify {
            val files = it?.listFiles() ?: emptyArray()
            block(files)
        }
    }

    private fun <T> modify(block: (dir: File?) -> T): T {
        synchronized(this@DownloadDir) {
            val directory = if (_dir.fMakeDirs()) _dir else null
            return block(directory)
        }
    }

    private fun newKeyFile(key: String, ext: String): File? {
        val dotExt = ext.takeIf { it.isEmpty() } ?: ".$ext"
        return modify { dir ->
            dir?.resolve(md5(key) + dotExt)
        }
    }
}

private fun File?.fMakeDirs(): Boolean {
    if (this == null) return false
    if (this.isDirectory) return true
    if (this.isFile) this.delete()
    return this.mkdirs()
}

private fun md5(value: String): String {
    return MessageDigest.getInstance("MD5")
        .digest(value.toByteArray())
        .joinToString("") { "%02X".format(it) }
}