package com.mybetlive.i18n
import android.content.res.Configuration
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Locale
@RunWith(RobolectricTestRunner::class) @Config(sdk=[28],qualifiers="en-rUS")
class AppStringsTest {
    private fun resources(language:String)=RuntimeEnvironment.getApplication().createConfigurationContext(Configuration().apply{setLocale(Locale.forLanguageTag(language))}).resources
    @Test fun deviceLanguageAndSpanishFallbackAreRespected(){
        assertEquals("Games",AppStrings.translate("Partidos",resources("en-US")))
        assertEquals("Trackers",AppStrings.translate("Seguimientos",resources("en-GB")))
        assertEquals("Partidos",AppStrings.translate("Partidos",resources("es-MX")))
        assertEquals("Partidos",AppStrings.translate("Partidos",resources("fr-FR")))
    }
    @Test fun dynamicMessagesScopesUnitsAndProperNamesArePreserved(){
        val english=resources("en")
        assertEquals("Active (3)",AppStrings.translate("Activos (3)",english))
        assertEquals("Scope: 1st period",AppStrings.translate("Alcance: 1.er período",english))
        assertEquals("Goals · Boston Bruins: 2",AppStrings.translate("Goles · Boston Bruins: 2",english))
        assertEquals("Los Angeles Dodgers",AppStrings.translate("Los Angeles Dodgers",english))
        assertEquals("Line 3.5",AppStrings.translate("Línea 3.5",english))
        assertEquals("0 / 1 active trackers",AppStrings.translate("0 / 1 seguimientos activos",english))
        assertEquals("Receptions",AppStrings.translate("Recepciones",english))
        assertEquals("three-pointers",AppStrings.translate("triples",english))
        assertEquals("rushing yards",AppStrings.translate("yardas terrestres",english))
        assertEquals("total shots",AppStrings.translate("tiros totales",english))
    }
    @Test fun allResourcesHaveTranslationsAndTemplatesAreComplete(){
        val r=resources("en")
        val sources=r.getStringArray(com.mybetlive.R.array.ui_source)
        val targets=r.getStringArray(com.mybetlive.R.array.ui_translation)
        assertEquals(sources.size,targets.size)
        sources.zip(targets).forEach{(a,b)->
            assertTrue(b.isNotEmpty())
            assertEquals(Regex("\\{[0-9]+}").findAll(a).map{it.value}.toSet(),Regex("\\{[0-9]+}").findAll(b).map{it.value}.toSet())
        }
    }
    @Test fun everyRequestSendsTheCurrentLanguageWithoutChangingAuthentication(){
        val previous=AppStrings.resources
        val server=okhttp3.mockwebserver.MockWebServer();server.start()
        try {
            AppStrings.resources=resources("en-GB")
            server.enqueue(okhttp3.mockwebserver.MockResponse().setBody("{}"))
            val client=okhttp3.OkHttpClient.Builder().addInterceptor(com.mybetlive.data.SessionAuthInterceptor{"token"}).build()
            client.newCall(okhttp3.Request.Builder().url(server.url("/api/v1/tracking/bets")).build()).execute().close()
            val request=server.takeRequest()
            assertEquals("en",request.getHeader("Accept-Language"));assertEquals("Bearer token",request.getHeader("Authorization"))
        } finally {server.shutdown();AppStrings.resources=previous}
    }
}
