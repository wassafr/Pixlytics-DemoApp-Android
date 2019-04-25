package io.wassa.pixlyticssdksampleapp

import android.annotation.SuppressLint
import android.app.Application
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import io.wassa.pixlytics_sdk.common.Pixlytics
import io.wassa.pixlytics_sdk.common.listener.OnLoadPixlyticsListener

/**
 * In application class you can initialise Pixlytics.
 *
 * To get a license key please contact support.
 *
 * Pixlytics must be initialise before any use of the library.
 */
class PixApplication: Application(), OnLoadPixlyticsListener {

    override fun onCreate() {
        super.onCreate()
        Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID)
        Pixlytics.init(this,"---> YOUR LICENCE KEY <---", this)
    }

    override fun onError(error: Throwable) {
        Toast.makeText(this, "Unable to load Pixlytics. Please check logs", Toast.LENGTH_LONG).show()
        Log.e(TAG, "Unable to load Pixlytics", error)
    }

    override fun onSuccess() {
        Toast.makeText(this, "Pixlytics load successful", Toast.LENGTH_SHORT).show()
    }

    companion object {
        val TAG = this::class.simpleName
    }
}