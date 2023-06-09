package com.sd.lib.downloader

internal class DownloadTask(val url: String) {
    private var _state = DownloadState.None
    private val _transmitParam = TransmitParam()

    @Synchronized
    fun notifyInitialized(): Boolean {
        if (_state != DownloadState.None) return false
        _state = DownloadState.Initialized
        return true
    }

    /**
     * 下载进度
     */
    @Synchronized
    fun notifyProgress(total: Long, current: Long): IDownloadInfo.Progress? {
        if (_state.isFinished) return null
        _state = DownloadState.Progress
        return if (_transmitParam.transmit(total, current)) {
            IDownloadInfo.Progress(
                url = url,
                total = _transmitParam.total,
                current = _transmitParam.current,
                progress = _transmitParam.progress,
                speedBps = _transmitParam.speedBps,
            )
        } else {
            null
        }
    }

    /**
     * 下载成功
     */
    @Synchronized
    fun notifySuccess(): Boolean {
        if (_state.isFinished) return false
        this._state = DownloadState.Success
        return true
    }

    /**
     * 下载失败
     */
    @Synchronized
    fun notifyError(): Boolean {
        if (_state.isFinished) return false
        _state = DownloadState.Error
        return true
    }

    private enum class DownloadState {
        None,

        /** 已提交 */
        Initialized,

        /** 下载中 */
        Progress,

        /** 下载成功 */
        Success,

        /** 下载失败 */
        Error;

        /** 是否处于完成状态，[Success]或者[Error] */
        val isFinished: Boolean
            get() = this == Success || this == Error
    }
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
     *
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
        return "${current}/${total} $progress ${super.toString()}"
    }
}