package com.mybetlive.application
import org.junit.Assert.*
import org.junit.Test
class EmailAccountsTest {
    private val token="a".repeat(64)
    @Test fun acceptsOnlyOurAccountRoutesAndFixedFormatTokens() {
        assertEquals(EmailLinkKind.VERIFY,EmailAccountLink.parse("mybetlive://auth/verify?token=$token")!!.kind)
        assertEquals(EmailLinkKind.RESET,EmailAccountLink.parse("mybetlive://auth/reset?token=$token")!!.kind)
        for(uri in listOf("https://evil.test/reset?token=$token","mybetlive://evil/reset?token=$token","mybetlive://user@auth/reset?token=$token",
            "mybetlive://auth/reset?token=$token&server=https://evil.test","mybetlive://auth/reset?token=short","mybetlive://auth/reset?token=$token#x"))assertNull(uri,EmailAccountLink.parse(uri))
    }
    @Test fun validatesEmailUsernameAndExactPasswordWithoutTrimming() {
        assertNull(emailRegistrationError("mau","user@example.test","  password123  ","  password123  "))
        assertNotNull(emailRegistrationError("x","user@example.test","password123","password123"))
        assertNotNull(emailRegistrationError("mau","invalid","password123","password123"))
        assertNotNull(passwordChangeError("short","short"))
        assertNotNull(passwordChangeError("  password123  ","password123"))
        assertNotNull(passwordChangeError("é".repeat(40),"é".repeat(40)))
    }
    @Test fun acceptsProductionHttpsLinksWithoutBroadeningTrust() {
        assertEquals(EmailAccountLink(EmailLinkKind.RESET,token),EmailAccountLink.parse("https://mybetliveadmin.pages.dev/reset-password?token=$token"))
        assertEquals(EmailAccountLink(EmailLinkKind.VERIFY,token),EmailAccountLink.parse("https://mybetliveadmin.pages.dev/verify-email?token=$token"))
        for(uri in listOf(
            "http://mybetliveadmin.pages.dev/reset-password?token=$token",
            "https://mybetliveadmin.pages.dev.evil.test/reset-password?token=$token",
            "https://user@mybetliveadmin.pages.dev/reset-password?token=$token",
            "https://mybetliveadmin.pages.dev:443/reset-password?token=$token",
            "https://mybetliveadmin.pages.dev/admin/mybet?token=$token",
            "https://mybetliveadmin.pages.dev/reset-password/extra?token=$token",
            "https://mybetliveadmin.pages.dev/reset-password?token=$token&token=$token",
            "https://mybetliveadmin.pages.dev/reset-password?token=short",
            "https://mybetliveadmin.pages.dev/verify-email?token=$token#fragment"
        ))assertNull(uri,EmailAccountLink.parse(uri))
    }
}
