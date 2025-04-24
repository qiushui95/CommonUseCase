package com.usecase.phone

import com.blankj.utilcode.util.ShellUtils
import org.json.JSONObject

public object CloudPhoneUtils {

    private fun getSystemJsonObject(): JSONObject? {
        val json = ShellUtils.execCmd("dg dump system", false, true).successMsg

        return runCatching { JSONObject(json).getJSONObject("system") }
            .onFailure { it.printStackTrace() }
            .getOrNull()
    }

    public fun getIPAddress(): String? {
        return getSystemJsonObject()?.getString("deviceIp")
    }

    public fun getVmCode(): String? {
        return getSystemJsonObject()?.getString("deviceVmCode")
    }

    public fun allowRoot(pkgName: String) {
        val allowArray = runCatching { getSystemJsonObject()?.getJSONArray("suAllowApps") }
            .onFailure { it.printStackTrace() }
            .getOrNull() ?: return

        val allowSet = mutableSetOf<String>()

        repeat(allowArray.length()) { allowSet.add(allowArray.getString(it)) }

        allowSet.add(pkgName)

        val cmdList = mutableListOf<String>()

        cmdList.add("dg am stop $pkgName")
        cmdList.add("dg config -a system.suAllowApps=${allowSet.joinToString(",")}")
        cmdList.add("dg am stop $pkgName")

        ShellUtils.execCmd(cmdList, false, true)
    }

    private fun getCurByPassSet(): MutableSet<String> {
        val json = ShellUtils.execCmd("dg dump net", false, true).successMsg

        val array = JSONObject(json).getJSONObject("net").getJSONArray("vpn.bypassPkgs")

        val set = mutableSetOf<String>()

        repeat(array.length()) { set.add(array.getString(it)) }

        return set
    }

    public fun allowVPNByPass(pkgName: String) {

        val pkgNameSet = runCatching { getCurByPassSet() }
            .onFailure { it.printStackTrace() }
            .getOrNull() ?: return

        if (pkgNameSet.contains(pkgName)) return

        pkgNameSet.add(pkgName)

        val cmdValue = pkgNameSet.joinToString(",")

        ShellUtils.execCmd("dg config -a net.vpn.bypassPkgs=$cmdValue", false, true)
    }
}