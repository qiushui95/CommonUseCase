package com.usecase.phone.shell

import com.blankj.utilcode.util.ShellUtils
import com.usecase.phone.Logger

internal class ShellUseCaseImpl : ShellUseCase {
    private fun checkNeedRoot(cmd: String): Boolean {
        if (cmd.contains("/data/")) return true
        if (cmd.contains("pm install")) return true
        if (cmd.contains("pm uninstall")) return true
        if (cmd.contains("cd /")) return true
        if (cmd.contains("settings put secure")) return true

        return false
    }

    private fun needLogResult(cmd: String): Boolean {
        if (cmd.contains("tar")) return false

        return true
    }

    override fun runCmd(cmd: String, skipError: Boolean, logger: Logger): ShellRunResult {
        return runCmd(listOf(cmd), skipError, logger)
    }

    override fun runCmd(cmdList: List<String>, skipError: Boolean, logger: Logger): ShellRunResult {
        return runCmd(cmdList.map { ShellRunConfig(it, skipError) }, logger)
    }

    override fun runCmd(cmdList: List<ShellRunConfig>, logger: Logger): ShellRunResult {
        val resultList = mutableListOf<ShellUtils.CommandResult>()

        val iterator = cmdList.iterator()

        var shellRunResult: ShellRunResult? = null

        while (iterator.hasNext()) {
            val (cmd, skipError) = iterator.next()

            val root = checkNeedRoot(cmd)

            logger.log("start run cmd root:$root, skipError: $skipError, cmd: $cmd")

            val result = ShellUtils.execCmd(cmd, root, true)

            val sb = StringBuilder()
            sb.append("finish run cmd, result: ${result.result}, ")

            if (result.result == 0) {
                if (needLogResult(cmd)) {
                    sb.append("success: ${result.successMsg}")
                }
            } else {
                sb.append("errorMsg: ${result.errorMsg}")
            }

            logger.log(sb.toString())

            if (result.result != 0 && skipError.not()) {
                shellRunResult = ShellRunResult.Failure(result)
                break
            }

            resultList.add(result)
        }

        while (iterator.hasNext()) {
            val cmd = iterator.next()

            logger.log("skip cmd: $cmd")
        }

        return shellRunResult ?: ShellRunResult.Success(resultList.map { it.successMsg })
    }
}
