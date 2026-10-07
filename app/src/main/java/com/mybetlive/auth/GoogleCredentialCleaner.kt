package com.mybetlive.auth
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.ClearCredentialStateRequest
import com.mybetlive.application.CredentialCleaner
class GoogleCredentialCleaner(private val context:Context):CredentialCleaner {
    override suspend fun clear(){CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())}
}
