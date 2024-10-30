package com.usecase.service

import android.util.Log
import androidx.annotation.CallSuper
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.delay

public abstract class BaseService : LifecycleService() {
    @CallSuper
    override fun onCreate() {
        Log.e(javaClass.simpleName, "onCreate")
        super.onCreate()
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()

        Log.e(javaClass.simpleName, "onDestroy")
    }

    protected suspend fun <T> tryDoSomething(block: suspend () -> T): T? {
        repeat(3) {
            delay(it * 3000L)

            val result = runCatching { block() }

            val value = result.getOrNull()

            if (result.isSuccess && value != null) {
                return value
            }
        }

        return null
    }
}
