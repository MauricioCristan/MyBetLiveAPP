package com.mybetlive.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import org.junit.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BankDevicePreviewTest {
 @get:Rule val compose=createAndroidComposeRule<BankDevicePreviewActivity>()
 @Test fun realDeviceSummaryAndManualForm() {
  compose.onNodeWithTag("nav-BANK").performClick()
  compose.onNodeWithText("Mi bank").assertIsDisplayed()
  compose.onNodeWithText("Añadir apuesta").assertIsDisplayed().assertIsEnabled()
  capture("bank-honor-resumen")
  compose.onNodeWithText("Añadir apuesta").performClick()
  compose.onNodeWithText("Vincular seguimiento: Registro manual").assertExists()
  capture("bank-honor-registro")
  compose.onNodeWithText("Guardar apuesta").performScrollTo().assertIsDisplayed()
  // Do not submit: these fixtures are purely visual and have no network operations.
 }
 private fun capture(name:String) {
  compose.waitForIdle()
  val instrumentation=InstrumentationRegistry.getInstrumentation()
  // Compose can be idle while the native dialog window is still fading in.
  android.os.SystemClock.sleep(650)
  val image=instrumentation.uiAutomation.takeScreenshot()
  val file=File(instrumentation.targetContext.filesDir,"bank-qa/$name.png")
  file.parentFile!!.mkdirs()
  file.outputStream().use{image.compress(Bitmap.CompressFormat.PNG,100,it)}
  image.recycle()
 }
}
