package com.usecase.phone.dg

import com.blankj.utilcode.util.ShellUtils

internal class SystemDGUseCase : BaseDGUseCase() {
    override val moduleName: String = "system"

    fun getIPAddress(): String? {
        return getModuleJsonObject()?.getString("deviceIp")
    }

    fun getVmCode(): String? {
        return getModuleJsonObject()?.getString("deviceVmCode")
    }

    fun allowRoot(pkgNameSet: Set<String>) {
        val jsonObject = getModuleJsonObject() ?: return

        val allowArray = jsonObject.optJSONArray("suAllowApps") ?: return
        val denyArray = jsonObject.optJSONArray("suDenyApps") ?: return

        val allowSet = mutableSetOf<String>()
        val denySet = mutableSetOf<String>()

        repeat(allowArray.length()) { allowSet.add(allowArray.getString(it)) }
        repeat(denyArray.length()) { denySet.add(denyArray.getString(it)) }

        allowSet.addAll(pkgNameSet)
        denySet.removeAll(pkgNameSet)

        val cmdList = mutableListOf<String>()

        pkgNameSet.mapTo(cmdList) { "dg am stop $it" }

        val allowStr = allowSet.joinToString(",")
        val denyStr = denySet.joinToString(",")

        cmdList.add("dg config -a system.suAllowApps=$allowStr -a system.suDenyApps=$denyStr")

        pkgNameSet.mapTo(cmdList) { "dg am stop $it" }

        ShellUtils.execCmd(cmdList, false, true)
    }
}
