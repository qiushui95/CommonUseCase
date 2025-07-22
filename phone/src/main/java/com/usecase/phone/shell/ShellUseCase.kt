package com.usecase.phone.shell

import com.usecase.phone.Logger

internal interface ShellUseCase {
    fun runCmd(cmd: String, skipError: Boolean = false, logger: Logger): ShellRunResult

    fun runCmd(cmdList: List<String>, skipError: Boolean = false, logger: Logger): ShellRunResult

    fun runCmd(cmdList: List<ShellRunConfig>, logger: Logger): ShellRunResult
}
