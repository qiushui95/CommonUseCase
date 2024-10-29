package com.usecase.phone

import com.blankj.utilcode.util.RegexUtils

public class PhoneUseCaseImpl : PhoneUseCase {
    override fun getVMCodeFromIp(ip: String): String? {
        if (RegexUtils.isIP(ip).not()) return null

        return ip.split('.').joinToString(prefix = "VM") {
            it.padEnd(3, '0')
        }
    }
}
