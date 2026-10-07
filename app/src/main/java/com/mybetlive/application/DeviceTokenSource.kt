package com.mybetlive.application
interface DeviceTokenSource {suspend fun token():String?}
interface CredentialCleaner {suspend fun clear()}
