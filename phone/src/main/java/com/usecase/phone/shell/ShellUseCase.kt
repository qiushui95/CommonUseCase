package com.usecase.phone.shell

internal interface ShellUseCase {
    fun runCmd(cmd: String, logger: (String) -> Unit): ShellRunResult

    fun runCmd(cmdList: List<String>, logger: (String) -> Unit): ShellRunResult
}
