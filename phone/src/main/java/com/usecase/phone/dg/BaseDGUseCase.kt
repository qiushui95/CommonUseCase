package com.usecase.phone.dg

import com.blankj.utilcode.util.ShellUtils
import org.json.JSONObject

internal abstract class BaseDGUseCase {
    protected abstract val moduleName: String

    protected fun getModuleJsonObject(throwException: Boolean = false): JSONObject? {
        val json = ShellUtils.execCmd("dg dump $moduleName", false, true).successMsg

        try {
            return JSONObject(json).getJSONObject(moduleName)
        } catch (ex: Exception) {
            if (throwException) throw ex else ex.printStackTrace()
            return null
        }
    }
}
