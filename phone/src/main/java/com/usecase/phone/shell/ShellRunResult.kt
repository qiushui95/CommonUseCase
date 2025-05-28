package com.usecase.phone.shell

import com.blankj.utilcode.util.ShellUtils

private typealias CommandResult = ShellUtils.CommandResult

public sealed class ShellRunResult(public val isSuccess: Boolean) {
    public data class Success(val outputList: List<String>) : ShellRunResult(true)

    public data class Failure(val code: Int, val errorMsg: String) : ShellRunResult(false) {
        internal constructor(result: CommandResult) : this(result.result, result.errorMsg)
    }
}
