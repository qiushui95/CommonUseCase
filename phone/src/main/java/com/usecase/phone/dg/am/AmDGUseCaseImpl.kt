package com.usecase.phone.dg.am

import com.blankj.utilcode.util.ShellUtils
import com.usecase.phone.TopAppInfo
import com.usecase.phone.dg.BaseDGUseCase

internal class AmDGUseCaseImpl : BaseDGUseCase(), AmDGUseCase {
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

    override fun allowKeepAlive(pkgName: String) {
        return allowKeepAlive(setOf(pkgName))
    }

    override fun allowKeepAlive(pkgNameSet: Set<String>) {
        val result = runCatching { getCurKeepAliveSet() }

        if (result.isSuccess.not()) return

        val curSet = result.getOrNull() ?: return

        if (curSet.containsAll(pkgNameSet)) return

        curSet.addAll(pkgNameSet)

        updateKeepAlive(curSet)
    }

    override fun denyKeepAlive(pkgName: String) {
        return denyKeepAlive(setOf(pkgName))
    }

    override fun denyKeepAlive(pkgNameSet: Set<String>) {
        val result = runCatching { getCurKeepAliveSet() }

        if (result.isSuccess.not()) return

        val curSet = result.getOrNull() ?: return

        if (curSet.containsAll(pkgNameSet)) return

        curSet.removeAll(pkgNameSet)

        updateKeepAlive(curSet)
    }

    override fun getTopInfo(): TopAppInfo? {
        val jsonObject = getModuleJsonObject()?.optJSONObject("topApp") ?: return null

        val pkgName = jsonObject.optString("pkg").ifBlank { null } ?: return null

        val uid = jsonObject.optInt("uid", -1).takeIf { it != -1 } ?: return null
        val pid = jsonObject.optInt("pid", -1).takeIf { it != -1 } ?: return null

        val activityName = jsonObject.optString("activity").ifBlank { null } ?: return null

        return TopAppInfo(pkgName = pkgName, activityName = activityName, uid = uid, pid = pid)
    }

    override fun launchApp(pkgName: String) {
        ShellUtils.execCmd("dg am start $pkgName", false, true)
    }

    override fun relaunchApp(pkgName: String) {
        ShellUtils.execCmd("dg am restart $pkgName", false, true)
    }

    override fun stopApp(pkgName: String) {
        ShellUtils.execCmd("dg am stop $pkgName", false, true)
    }
}
