package com.mybetlive.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mybetlive.composition.ClientDependencies
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/** Read-only connection diagnostic. Never prints credentials or account data. */
@RunWith(AndroidJUnit4::class)
class BankConnectionDeviceTest {
 @Test fun inspectBankCapability()=runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val configured=Class.forName("com.mybetlive.BuildConfig").getField("AUTH_BACKEND_URL").get(null)
  val repository=ClientDependencies.repository(context)
  println("BANK_CHECK configured=$configured sessionBackend=${repository.session?.baseUrl?:"NONE"}")
  if(repository.session!=null) {
   try {
    val subscription=repository.subscription();println("BANK_CHECK bankAccess=${subscription.bankAccess!=null}")
    if(subscription.bankAccess!=null){val banks=repository.authorizedClient(BankApi::class.java){it.list()};println("BANK_CHECK banksReadable=true count=${banks.size}")}
   }
   catch(error:Exception){println("BANK_CHECK subscriptionError=${error.javaClass.simpleName}")}
  }
 }
}
