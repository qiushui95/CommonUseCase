package com.usecase.phone

import com.usecase.phone.dg.am.AmDGUseCase
import com.usecase.phone.dg.am.AmDGUseCaseImpl
import com.usecase.phone.dg.net.NetDGUseCase
import com.usecase.phone.dg.net.NetDGUseCaseImpl
import com.usecase.phone.dg.system.SystemDGUseCase
import com.usecase.phone.dg.system.SystemDGUseCaseImpl
import com.usecase.phone.shell.ShellUseCase
import com.usecase.phone.shell.ShellUseCaseImpl

public object CloudPhoneUtils :
    NetDGUseCase by NetDGUseCaseImpl(),
    SystemDGUseCase,
    AmDGUseCase by AmDGUseCaseImpl(),
    ShellUseCase by ShellUseCaseImpl() {
    private val systemDGUseCase: SystemDGUseCase by lazy { SystemDGUseCaseImpl() }

    private fun ip2VmCode(ip: String?): String? {
        ip ?: return null

        return ip.split(".").joinToString("", prefix = "VM") { it.padStart(3, '0') }
    }

    override fun getIPAddress(): String? {
        return systemDGUseCase.getIPAddress()
    }

    override fun getVmCode(): String? {
        return systemDGUseCase.getVmCode() ?: ip2VmCode(getIPAddress())
    }

    override fun allowRoot(pkgName: String) {
        systemDGUseCase.allowRoot(pkgName)
    }

    override fun allowRoot(pkgNameSet: Set<String>) {
        systemDGUseCase.allowRoot(pkgNameSet)
    }

    override fun denyRoot(pkgName: String) {
        systemDGUseCase.denyRoot(pkgName)
    }

    override fun denyRoot(pkgNameSet: Set<String>) {
        systemDGUseCase.denyRoot(pkgNameSet)
    }
}
