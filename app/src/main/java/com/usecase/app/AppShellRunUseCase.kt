package com.usecase.app

public interface AppShellRunUseCase {
    public fun runShell(cmd: String)

    public fun runShell(cmdList: List<String>)
}
