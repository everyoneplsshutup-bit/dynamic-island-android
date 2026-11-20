package com.example.vault

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.DynamicIslandApp

class VaultExpiryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? DynamicIslandApp ?: return Result.failure()
        app.vaultManager.processExpirations()
        return Result.success()
    }

    companion object {
        const val KEY_ITEM_ID = "vault_item_id"
    }
}

class VaultPurgeWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? DynamicIslandApp ?: return Result.failure()
        app.vaultManager.processExpirations()
        return Result.success()
    }
}
