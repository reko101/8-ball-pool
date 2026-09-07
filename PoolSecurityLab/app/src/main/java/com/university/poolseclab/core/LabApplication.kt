package com.university.poolseclab.core

import android.app.Application

/**
 * Application entry point. It only seeds the shared match state, so there is
 * nothing here that can fail at start up.
 */
class LabApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        GameHolder.ensureInitialised()
    }
}
