package com.mybetlive.application

/** Intro belongs to a fresh launcher entry, never to restored screens or external destinations. */
internal fun shouldShowStartupIntro(restored: Boolean, launcherEntry: Boolean, hasDestination: Boolean,
    animationsEnabled: Boolean): Boolean = !restored && launcherEntry && !hasDestination && animationsEnabled
