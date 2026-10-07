package com.mybetlive
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ApplicationBoundaryTest {
    @Test fun applicationPortsAndRewardCaseAreIndependentOfAndroidAndNetworkSdks() {
        val root=listOf(File("src/main/java/com/mybetlive"),File("app/src/main/java/com/mybetlive")).first{it.exists()}
        val sources=File(root,"application").walkTopDown().filter{it.extension=="kt"}.toList()
        assertTrue(sources.size>=3)
        val forbidden=Regex("(?m)^import (android\\.|androidx\\.|retrofit2\\.|okhttp3\\.|com\\.google\\.)")
        sources.forEach{assertFalse(it.name,forbidden.containsMatchIn(it.readText()))}
        val viewModel=File(root,"ui/TrackingViewModel.kt").readText()
        listOf("TrackingRepository(","BetNotifications(","repository.vault","retrofit2.","com.google.gson.","androidx.credentials.").forEach{assertFalse(it,viewModel.contains(it))}
    }
}
