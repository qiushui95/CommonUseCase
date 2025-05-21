package com.usecase.phone.dg.net

import com.blankj.utilcode.util.ShellUtils
import com.usecase.phone.dg.BaseDGUseCase

internal class NetDGUseCaseImpl : BaseDGUseCase(), NetDGUseCase {
    override val moduleName: String = "net"

    private fun getCurByPassSet(): MutableSet<String>? {
        val array = getModuleJsonObject(throwException = true)
            ?.getJSONArray("vpn.bypassPkgs")
            ?: return null

        val set = mutableSetOf<String>()

        repeat(array.length()) { set.add(array.getString(it)) }

        return set
    }

    private fun handleVPNByPass(pkgNameSet: Set<String>, isAllow: Boolean) {
        val result = runCatching { getCurByPassSet() }

        if (result.isSuccess.not()) return

        val curSet = result.getOrNull() ?: return

        val oldList = curSet.toSet()

        if (isAllow) {
            curSet.addAll(pkgNameSet)
        } else {
            curSet.removeAll(pkgNameSet)
        }

        if (oldList.containsAll(curSet) && curSet.containsAll(oldList)) return

        val cmdValue = curSet.joinToString(",")

        ShellUtils.execCmd("dg config -a net.vpn.bypassPkgs=$cmdValue", false, true)
    }

    override fun allowVPNByPass(pkgName: String) {
        return allowVPNByPass(setOf(pkgName))
    }

    override fun allowVPNByPass(pkgNameSet: Set<String>) {
        handleVPNByPass(pkgNameSet, isAllow = true)
    }

    override fun removeVPNByPass(pkgName: String) {
        return removeVPNByPass(setOf(pkgName))
    }

    override fun removeVPNByPass(pkgNameSet: Set<String>) {
        handleVPNByPass(pkgNameSet, isAllow = false)
    }
}
