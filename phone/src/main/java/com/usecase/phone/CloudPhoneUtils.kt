package com.usecase.phone

import com.blankj.utilcode.util.ShellUtils
import org.json.JSONArray
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

    private fun getJsonArray(jsonObject: JSONObject, key: String): JSONArray? {
        return runCatching { jsonObject.getJSONArray(key) }
            .onFailure { it.printStackTrace() }
            .getOrNull()
    }

    public fun allowRoot(pkgName: String, cmdLogger: ((List<String>) -> Unit)?) {
        val jsonObject = runCatching { getSystemJsonObject() }.getOrNull() ?: return

        val allowArray = getJsonArray(jsonObject, "suAllowApps") ?: return
        val denyArray = getJsonArray(jsonObject, "suDenyApps") ?: return

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

        cmdLogger?.invoke(cmdList)

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
