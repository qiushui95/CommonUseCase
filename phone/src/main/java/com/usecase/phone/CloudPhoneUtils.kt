package com.usecase.phone

import com.usecase.phone.dg.AmDGUseCase
import com.usecase.phone.dg.NetDGUseCase
import com.usecase.phone.dg.SystemDGUseCase

public object CloudPhoneUtils {
    private val systemDGUseCase by lazy { SystemDGUseCase() }
    private val netDGUseCase by lazy { NetDGUseCase() }
    private val amDGUseCase by lazy { AmDGUseCase() }

    public fun getIPAddress(): String? {
        return systemDGUseCase.getIPAddress()
    }

    public fun getVmCode(): String? {
        return systemDGUseCase.getVmCode()
    }

    public fun allowRoot(pkgName: String) {
        return systemDGUseCase.allowRoot(setOf(pkgName))
    }

    public fun allowRoot(pkgNameSet: Set<String>) {
        return systemDGUseCase.allowRoot(pkgNameSet)
    }

    public fun allowVPNByPass(pkgName: String) {
        return netDGUseCase.allowVPNByPass(setOf(pkgName))
    }

    public fun allowVPNByPass(pkgNameSet: Set<String>) {
        return netDGUseCase.allowVPNByPass(pkgNameSet)
    }

    public fun allowKeepAlive(pkgName: String) {
        return amDGUseCase.allowKeepAlive(setOf(pkgName))
    }

    public fun allowKeepAlive(pkgNameSet: Set<String>) {
        return amDGUseCase.allowKeepAlive(pkgNameSet)
    }

    public fun denyKeepAlive(pkgName: String) {
        return amDGUseCase.denyKeepAlive(setOf(pkgName))
    }

    public fun denyKeepAlive(pkgNameSet: Set<String>) {
        return amDGUseCase.denyKeepAlive(pkgNameSet)
    }
}
