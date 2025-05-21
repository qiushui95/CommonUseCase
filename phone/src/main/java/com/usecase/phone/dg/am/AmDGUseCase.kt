package com.usecase.phone.dg.am

import com.usecase.phone.TopAppInfo

internal interface AmDGUseCase {
    /**
     * 添加保活应用
     * @param pkgName 应用包名
     */
    fun allowKeepAlive(pkgName: String)

    /**
     *添加保活应用
     * @param pkgNameSet 应用包名集合
     */
    fun allowKeepAlive(pkgNameSet: Set<String>)

    /**
     * 移除保活应用
     * @param pkgName 应用包名
     */
    fun denyKeepAlive(pkgName: String)

    /**
     * 移除保活应用
     * @param pkgNameSet 应用包名集合
     */
    fun denyKeepAlive(pkgNameSet: Set<String>)

    /**
     * 获取当前前台应用信息
     * @return 前台应用信息
     */
    fun getTopInfo(): TopAppInfo?

    /**
     * 启动应用
     * @param pkgName 应用包名
     */
    fun launchApp(pkgName: String)

    /**
     * 重启应用
     * @param pkgName 应用包名
     */
    fun relaunchApp(pkgName: String)

    /**
     * 停止应用
     * @param pkgName 应用包名
     */
    fun stopApp(pkgName: String)
}
