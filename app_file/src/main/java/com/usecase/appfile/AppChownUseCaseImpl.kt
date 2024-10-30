package com.usecase.appfile

import com.blankj.utilcode.util.ShellUtils

public class AppChownUseCaseImpl : AppChownUseCase {
    override fun chownInternalDir(pkgName: String, uid: String) {
        val cmdList = mutableListOf<String>()

        cmdList.add("chown -R $uid:$uid /data/data/$pkgName")

        ShellUtils.execCmd(cmdList, true, true)
    }
}
