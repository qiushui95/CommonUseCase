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
        return systemDGUseCase.allowRoot(pkgName)
    }

    public fun allowVPNByPass(pkgName: String) {
        return netDGUseCase.allowVPNByPass(pkgName)
    }

    public fun allowKeepAlive(pkgName: String) {
        return amDGUseCase.allowKeepAlive(pkgName)
    }

    public fun denyKeepAlive(pkgName: String) {
        return amDGUseCase.denyKeepAlive(pkgName)
    }
}
