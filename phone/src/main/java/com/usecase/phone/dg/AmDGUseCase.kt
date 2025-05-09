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

    private fun updateKeepAlive(pkgNameSet: MutableSet<String>) {
        val cmdValue = pkgNameSet.joinToString(",")

        ShellUtils.execCmd("dg config -a am.persistentPkgs=$cmdValue", false, true)
    }

    fun allowKeepAlive(pkgName: String) {
        val result = runCatching { getCurKeepAliveSet() }

        if (result.isSuccess.not()) return

        val pkgNameSet = result.getOrNull() ?: return

        if (pkgNameSet.contains(pkgName)) return

        pkgNameSet.add(pkgName)

        updateKeepAlive(pkgNameSet)
    }

    fun denyKeepAlive(pkgName: String) {
        val result = runCatching { getCurKeepAliveSet() }

        if (result.isSuccess.not()) return

        val pkgNameSet = result.getOrNull() ?: return

        if (pkgNameSet.contains(pkgName).not()) return

        pkgNameSet.remove(pkgName)

        updateKeepAlive(pkgNameSet)
    }
}
