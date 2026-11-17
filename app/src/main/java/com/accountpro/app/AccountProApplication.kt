package com.accountpro.app

import android.app.Application
import com.accountpro.app.data.local.AccountProDatabase
import com.accountpro.app.data.local.PreferencesRepository
import com.accountpro.app.data.repository.AccountProRepository

class AccountProApplication : Application() {
    lateinit var repository: AccountProRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AccountProDatabase.getInstance(this)
        val prefs = PreferencesRepository(this)
        repository = AccountProRepository(database, prefs)
    }
}
