package com.usecase.phone.dg

import com.blankj.utilcode.util.ShellUtils

internal class AmDGUseCase : BaseDGUseCase() {
    override val moduleName: String = "am"

    private fun getCurKeepAliveSet(): MutableSet<String>? {
        val array = getModuleJsonObject(throwException = true)
            ?.getJSONArray("persistentPkgs")
            ?: return null

        val set = mutableSetOf<String>()

        repeat(array.length()) { set.add(array.getString(it)) }

        return set
    }

    private fun updateKeepAlive(pkgNameSet: Set<String>) {
        val cmdValue = pkgNameSet.joinToString(",")

        ShellUtils.execCmd("dg config -a am.persistentPkgs=$cmdValue", false, true)
    }

    fun allowKeepAlive(pkgNameSet: Set<String>) {
        val result = runCatching { getCurKeepAliveSet() }

        if (result.isSuccess.not()) return

        val curSet = result.getOrNull() ?: return

        if (curSet.containsAll(pkgNameSet)) return

        curSet.addAll(pkgNameSet)

        updateKeepAlive(curSet)
    }

    fun denyKeepAlive(pkgNameSet: Set<String>) {
        val result = runCatching { getCurKeepAliveSet() }

        if (result.isSuccess.not()) return

        val curSet = result.getOrNull() ?: return

        if (curSet.containsAll(pkgNameSet)) return

        curSet.removeAll(pkgNameSet)

        updateKeepAlive(curSet)
    }
}
