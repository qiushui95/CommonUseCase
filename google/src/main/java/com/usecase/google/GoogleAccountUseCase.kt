package com.usecase.google

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerFuture
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

public class GoogleAccountUseCase(private val context: Context) {

    private companion object {
        private const val GOOGLE_ACCOUNT_TYPE = "com.google"
        private const val GOOGLE_AUTH_TOKEN_TYPE = "androidmarket"
    }

    private val accountManager by lazy { AccountManager.get(context) }

    private fun futureStartActivity(future: AccountManagerFuture<Bundle>) {

        @Suppress("DEPRECATION")
        val intent = future.result.getParcelable<Intent>("intent") ?: return

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
    }

    public fun getAccountList(): List<Account> {
        return accountManager.getAccountsByType(GOOGLE_ACCOUNT_TYPE).toList()
    }

    public suspend fun removeAccount(account: Account): Unit = suspendCoroutine { continuation ->
        accountManager.removeAccount(account, null, { futureRemoveAccount(it, continuation) }, null)
    }

    private fun futureRemoveAccount(
        future: AccountManagerFuture<Bundle>,
        continuation: Continuation<Unit>
    ) {
        futureStartActivity(future)
        continuation.resume(Unit)
    }

    public suspend fun removeAllAccount() {
        for (account in getAccountList()) {
            removeAccount(account)
        }
    }

    public fun launchGoogleLogin() {
        accountManager.addAccount(
            GOOGLE_ACCOUNT_TYPE,
            GOOGLE_AUTH_TOKEN_TYPE,
            null,
            null,
            null,
            ::futureStartActivity,
            null
        )
    }

}