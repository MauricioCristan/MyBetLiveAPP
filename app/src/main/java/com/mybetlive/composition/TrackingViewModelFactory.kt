package com.mybetlive.composition

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mybetlive.data.TrackingRepository
import com.mybetlive.data.ClientErrorMessages
import com.mybetlive.notifications.BetNotifications
import com.mybetlive.ui.TrackingViewModel

class TrackingViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == TrackingViewModel::class.java)
        val repository=ClientDependencies.repository(app)
        @Suppress("UNCHECKED_CAST")
        return TrackingViewModel(app,repository,repository,repository,repository,repository,
            BetNotifications(app),ClientErrorMessages()) as T
    }
}
