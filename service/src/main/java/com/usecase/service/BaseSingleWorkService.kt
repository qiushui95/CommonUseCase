package com.usecase.service

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

public abstract class BaseSingleWorkService : BaseService() {
    private val dbWorkDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val dbWorkJob = SupervisorJob()

    private val intervalReportJob = SupervisorJob()
    private val intervalWorkJob = SupervisorJob()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        onStartCommand(intent)

        return onCommandResult(intent)
    }

    protected open fun onCommandResult(intent: Intent?): Int {
        return START_STICKY
    }

    private fun onStartCommand(intent: Intent?) {
        onReceiveCommand(intent)

        startIntervalReport()

        startIntervalWorking()
    }

    protected abstract fun onReceiveCommand(intent: Intent?)

    protected abstract fun logMessage(message: String)

    protected fun startDBWork(
        block: suspend () -> Unit,
    ): Job = lifecycleScope.launch(dbWorkDispatcher + dbWorkJob) {
        block()
    }

    protected suspend fun waitDBIdle() {
        dbWorkJob.children.forEach { it.join() }
    }

    private fun startIntervalReport() {
        if (intervalReportJob.children.any { it.isActive }) return

        lifecycleScope.launch(Dispatchers.IO + intervalReportJob) {
            while (isActive) {
                delay(5000)
                waitDBIdle()

                if (startReport().not()) {
                    logMessage("暂无任务需要上报,退出监听")
                    break
                }
            }
        }
    }

    /**
     * 开始上报
     * @return 是否继续下一轮上报
     */
    protected abstract suspend fun startReport(): Boolean

    private fun startIntervalWorking() {
        if (intervalWorkJob.children.any { it.isActive }) return

        lifecycleScope.launch(Dispatchers.IO + intervalWorkJob) {
            while (isActive) {
                waitDBIdle()

                if (startWork().not()) {
                    logMessage("暂无任务,退出监听")
                    break
                }
            }
        }
    }

    /**
     * 开始工作
     * @return 是否继续下一轮上报
     */
    protected abstract suspend fun startWork(): Boolean
}
