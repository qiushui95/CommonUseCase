package com.usecase.phone.dg.system

internal interface SystemDGUseCase {
    /**
     * 获取局域网IP地址
     */
    fun getIPAddress(): String?

    /**
     * 获取云手机编号
     */
    fun getVmCode(): String?

    /**
     * 授权应用Root
     * @param pkgName 应用包名
     */
    fun allowRoot(pkgName: String)

    /**
     * 授权应用Root
     * @param pkgNameSet 应用包名集合
     */
    fun allowRoot(pkgNameSet: Set<String>)

    /**
     * 拒绝应用Root
     * @param pkgName 应用包名
     */
    fun denyRoot(pkgName: String)

    /**
     * 拒绝应用Root
     * @param pkgNameSet 应用包名集合
     */
    fun denyRoot(pkgNameSet: Set<String>)
}
