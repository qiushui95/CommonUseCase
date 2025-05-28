package com.usecase.phone.shell

import com.blankj.utilcode.util.ShellUtils

private typealias Logger = (String) -> Unit

internal class ShellUseCaseImpl : ShellUseCase {
    private fun checkNeedRoot(cmd: String): Boolean {
        if (cmd.contains("/data/")) return true
        if (cmd.contains("pm install")) return true
        if (cmd.contains("pm uninstall")) return true
        if (cmd.contains("cd /")) return true

        return false
    }

    override fun runCmd(cmd: String, logger: Logger): ShellRunResult {
        return runCmd(listOf(cmd), logger)
    }

    override fun runCmd(cmdList: List<String>, logger: Logger): ShellRunResult {
        val resultList = mutableListOf<ShellUtils.CommandResult>()

        val iterator = cmdList.iterator()

        var shellRunResult: ShellRunResult? = null

        while (iterator.hasNext()) {
            val cmd = iterator.next()

            val root = checkNeedRoot(cmd)

            logger("start run cmd($root): $cmd")

            val result = ShellUtils.execCmd(cmd, root, true)

            val sb = StringBuilder()
            sb.append("finish run cmd, result: ${result.result}, ")

            if (result.result == 0) {
                sb.append("success: ${result.successMsg}")
            } else {
                sb.append("errorMsg: ${result.errorMsg}")
            }

            logger(sb.toString())

            if (result.result != 0) {
                shellRunResult = ShellRunResult.Failure(result)
                break
            }

            resultList.add(result)
        }

        while (iterator.hasNext()) {
            val cmd = iterator.next()

            logger("skip cmd: $cmd")
        }

        return shellRunResult ?: ShellRunResult.Success(resultList.map { it.successMsg })
    }
}
