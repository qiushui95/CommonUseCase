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

    fun allowRoot(pkgName: String) {
        val jsonObject = getModuleJsonObject() ?: return

        val allowArray = jsonObject.optJSONArray("suAllowApps") ?: return
        val denyArray = jsonObject.optJSONArray("suDenyApps") ?: return

        val allowSet = mutableSetOf<String>()
        val denySet = mutableSetOf<String>()

        repeat(allowArray.length()) { allowSet.add(allowArray.getString(it)) }
        repeat(denyArray.length()) { denySet.add(denyArray.getString(it)) }

        allowSet.add(pkgName)
        denySet.remove(pkgName)

        val cmdList = mutableListOf<String>()

        cmdList.add("dg am stop $pkgName")

        val allowStr = allowSet.joinToString(",")
        val denyStr = denySet.joinToString(",")

        cmdList.add("dg config -a system.suAllowApps=$allowStr -a system.suDenyApps=$denyStr")

        cmdList.add("dg am stop $pkgName")

        ShellUtils.execCmd(cmdList, false, true)
    }
}
