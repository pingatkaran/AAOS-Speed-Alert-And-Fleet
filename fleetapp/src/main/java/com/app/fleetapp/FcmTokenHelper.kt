package com.app.fleetapp

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class FcmTokenHelper(private val context: Context) {

    private val coroutineScope = CoroutineScope(Job() + Dispatchers.IO)

    // Call this method to generate the token
    fun generateAccessToken(onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        coroutineScope.launch {
            try {
                val token = getAccessToken()
                withContext(Dispatchers.Main) {
                    onSuccess(token)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    private suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        try {
            // Load service account JSON from assets
            val inputStream: InputStream = context.assets.open("service_account.json")
            
            GoogleCredentials
                .fromStream(inputStream)
                .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
                .apply { refreshIfExpired() }
                .accessToken
                .tokenValue
        } catch (e: Exception) {
            throw Exception("FCM Token Error: ${e.message}")
        }
    }

    fun clear() {
        coroutineScope.coroutineContext.cancel()
    }
}