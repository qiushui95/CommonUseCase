package com.usecase.phone.dg.net

internal interface NetDGUseCase {
    /**
     * 添加VPN绕过名单
     * @param pkgName 应用包名
     */
    fun allowVPNByPass(pkgName: String)

    /**
     * 添加VPN绕过名单
     * @param pkgNameSet 应用包名集合
     */
    fun allowVPNByPass(pkgNameSet: Set<String>)

    /**
     * 移除VPN绕过名单
     * @param pkgName 应用包名
     */
    fun removeVPNByPass(pkgName: String)

    /**
     * 移除VPN绕过名单
     * @param pkgNameSet 应用包名集合
     */
    fun removeVPNByPass(pkgNameSet: Set<String>)
}
