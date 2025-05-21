package com.usecase.phone.dg.system

import com.blankj.utilcode.util.ShellUtils
import com.usecase.phone.dg.BaseDGUseCase

internal class SystemDGUseCaseImpl : BaseDGUseCase(), SystemDGUseCase {
    override val moduleName: String = "system"

    override fun getIPAddress(): String? {
        return getModuleJsonObject()?.getString("deviceIp")
    }

    override fun getVmCode(): String? {
        return getModuleJsonObject()?.getString("deviceVmCode")
    }

    private fun handleRoot(pkgNameSet: Set<String>, isAllow: Boolean) {
        val jsonObject = getModuleJsonObject() ?: return

        val allowArray = jsonObject.optJSONArray("suAllowApps") ?: return
        val denyArray = jsonObject.optJSONArray("suDenyApps") ?: return

        val allowSet = mutableSetOf<String>()
        val denySet = mutableSetOf<String>()

        repeat(allowArray.length()) { allowSet.add(allowArray.getString(it)) }
        repeat(denyArray.length()) { denySet.add(denyArray.getString(it)) }

        if (isAllow) {
            allowSet.addAll(pkgNameSet)
            denySet.removeAll(pkgNameSet)
        } else {
            allowSet.removeAll(pkgNameSet)
            denySet.addAll(pkgNameSet)
        }

        val cmdList = mutableListOf<String>()

        pkgNameSet.mapTo(cmdList) { "dg am stop $it" }

        val allowStr = allowSet.joinToString(",")
        val denyStr = denySet.joinToString(",")

        cmdList.add("dg config -a system.suAllowApps=$allowStr -a system.suDenyApps=$denyStr")

        pkgNameSet.mapTo(cmdList) { "dg am stop $it" }

        ShellUtils.execCmd(cmdList, false, true)
    }

    override fun allowRoot(pkgName: String) {
        allowRoot(setOf(pkgName))
    }

    override fun allowRoot(pkgNameSet: Set<String>) {
        handleRoot(pkgNameSet, isAllow = true)
    }

    override fun denyRoot(pkgName: String) {
        denyRoot(setOf(pkgName))
    }

    override fun denyRoot(pkgNameSet: Set<String>) {
        handleRoot(pkgNameSet, isAllow = false)
    }
}
