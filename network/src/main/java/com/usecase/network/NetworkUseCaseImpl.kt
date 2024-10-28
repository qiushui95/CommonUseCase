package com.usecase.network

import com.blankj.utilcode.util.ShellUtils

public class NetworkUseCaseImpl : NetworkUseCase {
    override fun getIPAddress(): String {
        return ShellUtils.execCmd(
            "ifconfig eth0 | awk '/inet addr/{print substr(\$2,6)}'",
            false,
            true,
        ).successMsg.ifBlank { null }
            ?: ShellUtils.execCmd(
                "ifconfig wlan0 | awk '/inet addr/{print substr(\$2,6)}'",
                false,
                true,
            ).successMsg.ifBlank { null }
            ?: "0.0.0.0"
    }
}
