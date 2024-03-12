package com.sd.lib.downloader

import java.util.concurrent.atomic.AtomicReference

internal class DownloadTask(
    val url: String,
) {
    private val _state: AtomicReference<DownloadState> = AtomicReference(DownloadState.None)
    private val _transmitParam = TransmitParam()

    /**
     * 初始化
     */
    fun notifyInitialized(): Boolean {
        return _state.compareAndSet(DownloadState.None, DownloadState.Initialized)
    }

    /**
     * 下载进度
     */
    fun notifyProgress(total: Long, current: Long): IDownloadInfo.Progress? {
        return when (_state.get()) {
            DownloadState.None -> error("Task not initialized")
            DownloadState.Initialized,
            DownloadState.Progress -> {
                synchronized(_transmitParam) {
                    if (_transmitParam.transmit(total, current)) {
                        _transmitParam.toProgress(url)
                    } else {
                        null
                    }
                }
            }
            else -> null
        }
    }

    /**
     * 下载成功
     */
    fun notifySuccess(): Boolean {
        return when (val state = _state.get()) {
            DownloadState.None -> error("Task not initialized")
            DownloadState.Initialized,
            DownloadState.Progress -> _state.compareAndSet(state, DownloadState.Success)
            else -> false
        }
    }

    /**
     * 下载失败
     */
    fun notifyError(): Boolean {
        return when (val state = _state.get()) {
            DownloadState.Success,
            DownloadState.Error -> false
            else -> _state.compareAndSet(state, DownloadState.Error)
        }
    }

    private enum class DownloadState {
        None,

        /** 初始化 */
        Initialized,

        /** 下载中 */
        Progress,

        /** 下载成功 */
        Success,

        /** 下载失败 */
        Error;
    }
}

private fun TransmitParam.toProgress(url: String): IDownloadInfo.Progress {
    return IDownloadInfo.Progress(
        url = url,
        total = this.total,
        current = this.current,
        progress = this.progress,
        speedBps = this.speedBps,
    )
}

private class TransmitParam {
    private var _lastSpeedTime: Long = 0
    private var _lastSpeedCount: Long = 0

    /** 总数量 */
    var total: Long = 0
        private set

    /** 已传输数量 */
    var current: Long = 0
        private set

    /** 传输进度[0-100] */
    var progress: Int = 0
        private set

    /** 传输速率（B/S） */
    var speedBps: Int = 0
        private set

    /**
     * 传输
     * @param total 总量
     * @param current 当前传输量
     * @return true-进度发生了变化
     */
    fun transmit(total: Long, current: Long): Boolean {
        val oldProgress = progress

        if (total <= 0 || current <= 0) {
            reset()
            return oldProgress != progress
        }

        this.total = total
        this.current = current

        val newProgress = (current * 100 / total).toInt().coerceAtMost(100)
        this.progress = newProgress

        if (newProgress > oldProgress) {
            val time = System.currentTimeMillis()
            val changeTime = time - _lastSpeedTime
            val changeCount = current - _lastSpeedCount
            speedBps = (changeCount * (1000f / changeTime)).toInt().coerceAtLeast(0)
            _lastSpeedTime = time
            _lastSpeedCount = current
        }

        return newProgress > oldProgress
    }

    private fun reset() {
        _lastSpeedTime = 0
        _lastSpeedCount = 0
        total = 0
        current = 0
        progress = 0
        speedBps = 0
    }

    override fun toString(): String {
        return "${current}/${total} $progress $speedBps"
    }
}