package com.mybetlive.application

import org.junit.Assert.*
import org.junit.Test

class StartupPresentationTest {
    @Test fun freshLauncherEntryShowsIntro() {
        assertTrue(shouldShowStartupIntro(false,true,false,true))
    }
    @Test fun restoredScreensAndExternalDestinationsSkipIntro() {
        assertFalse(shouldShowStartupIntro(true,true,false,true))
        assertFalse(shouldShowStartupIntro(false,false,false,true))
        assertFalse(shouldShowStartupIntro(false,true,true,true))
    }
    @Test fun disabledSystemAnimationsSkipIntro() {
        assertFalse(shouldShowStartupIntro(false,true,false,false))
    }
}
