package com.mybetlive.data

/** An explicit debug environment switch must never keep credentials for another server. */
internal object BackendSessionPolicy {
    fun needsReset(debug:Boolean,explicitOverride:Boolean,savedUrl:String?,configuredUrl:String):Boolean =
        debug && explicitOverride && savedUrl!=null && savedUrl.trimEnd('/')!=configuredUrl.trimEnd('/')
}
