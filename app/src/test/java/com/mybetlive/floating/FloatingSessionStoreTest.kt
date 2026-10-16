package com.mybetlive.floating

import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class FloatingSessionStoreTest {
    @Test fun processRecoveryRequiresTheSameLoginAndExplicitCloseInvalidatesIt(){
        val context=RuntimeEnvironment.getApplication();val store=FloatingSessionStore(context);store.clear()
        assertNull(store.read("account|generation1"))
        store.save("account|generation1",listOf("a","b"),"b")
        val restored=FloatingSessionStore(context).read("account|generation1")!!
        assertEquals(listOf("a","b"),restored.ids);assertEquals("b",restored.selected)
        assertNull(store.read("other|generation1"));assertNull(store.read("account|generation2"))
        store.clear();assertNull(FloatingSessionStore(context).read("account|generation1"))
    }
}
