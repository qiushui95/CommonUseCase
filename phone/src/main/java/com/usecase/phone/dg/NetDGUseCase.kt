package com.usecase.phone.dg

import com.blankj.utilcode.util.ShellUtils

internal class NetDGUseCase : BaseDGUseCase() {
    override val moduleName: String = "net"

    private fun getCurByPassSet(): MutableSet<String>? {
        val array = getModuleJsonObject(throwException = true)
            ?.getJSONArray("vpn.bypassPkgs")
            ?: return null

        val set = mutableSetOf<String>()

        repeat(array.length()) { set.add(array.getString(it)) }

        return set
    }

    fun allowVPNByPass(pkgNameSet: Set<String>) {
        val result = runCatching { getCurByPassSet() }

        if (result.isSuccess.not()) return

        val curSet = result.getOrNull() ?: return

        if (curSet.containsAll(pkgNameSet)) return

        curSet.addAll(pkgNameSet)

        val cmdValue = curSet.joinToString(",")

        ShellUtils.execCmd("dg config -a net.vpn.bypassPkgs=$cmdValue", false, true)
    }
}
