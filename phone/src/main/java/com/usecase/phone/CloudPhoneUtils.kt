package com.usecase.phone

import com.usecase.phone.dg.am.AmDGUseCase
import com.usecase.phone.dg.am.AmDGUseCaseImpl
import com.usecase.phone.dg.net.NetDGUseCase
import com.usecase.phone.dg.net.NetDGUseCaseImpl
import com.usecase.phone.dg.system.SystemDGUseCase
import com.usecase.phone.dg.system.SystemDGUseCaseImpl

public object CloudPhoneUtils :
    NetDGUseCase by NetDGUseCaseImpl(),
    SystemDGUseCase by SystemDGUseCaseImpl(),
    AmDGUseCase by AmDGUseCaseImpl()
