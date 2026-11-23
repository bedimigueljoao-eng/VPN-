package com.example

import android.app.Application
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.VaultDatabase
import com.example.data.repository.VaultRepository
import com.example.network.NetworkMonitor

class InternetVaultApp : Application() {

    lateinit var database: VaultDatabase
        private set

    lateinit var repository: VaultRepository
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        database = VaultDatabase.getInstance(this)
        val prefsRepo = UserPreferencesRepository(this)
        repository = VaultRepository(database.vaultDao(), prefsRepo)
        networkMonitor = NetworkMonitor(this)
        networkMonitor.startObserving()
    }
}
