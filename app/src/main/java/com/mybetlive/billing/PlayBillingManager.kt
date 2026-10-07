package com.mybetlive.billing
import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.mybetlive.data.*
class PlayBillingManager(context:Context,
 private val onPrices:(Map<String,StorePrice>)->Unit,
 private val onPurchases:(List<String>)->Unit,
 private val onMessage:(String)->Unit) {
 private var plans:List<Plan> = emptyList()
 private var connecting=false
 private var closed=false
 private val pending=mutableListOf<()->Unit>()
 private val client=BillingClient.newBuilder(context).setListener {result,purchases->
  when(result.responseCode){
   BillingClient.BillingResponseCode.OK -> process(purchases.orEmpty())
   BillingClient.BillingResponseCode.USER_CANCELED -> onMessage("Compra cancelada.")
   BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restore()
   else -> onMessage("Google Play no pudo completar la compra. Reintenta.")
  }
 }.enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
  .enableAutoServiceReconnection().build()
 fun load(catalog:List<Plan>){
  plans=catalog.filter{it.code!="FREE" && it.playProductId!=null && it.playBasePlanId!=null}
  if(plans.isEmpty()){onPrices(emptyMap());return}
  connect {query {details->
   onPrices(plans.mapNotNull{plan->
    val offer=details.firstOrNull{it.productId==plan.playProductId}?.subscriptionOfferDetails
      ?.firstOrNull{it.basePlanId==plan.playBasePlanId && it.offerId==null}
    val phase=offer?.pricingPhases?.pricingPhaseList?.lastOrNull()
    if(phase==null)null else plan.id to StorePrice(phase.formattedPrice,period(phase.billingPeriod),
      phase.recurrenceMode==ProductDetails.RecurrenceMode.INFINITE_RECURRING)
   }.toMap())
  };restore()}
 }
 private fun period(value:String)=when(value){"P1W"->"semana";"P1M"->"mes";"P3M"->"3 meses";"P6M"->"6 meses";"P1Y"->"año";else->value}
 private fun connect(action:()->Unit){
  if(closed)return
  if(client.isReady){action();return}
  pending.add(action)
  if(connecting)return
  connecting=true
  client.startConnection(object:BillingClientStateListener{
   override fun onBillingSetupFinished(result:BillingResult){
    connecting=false
    if(closed)return
    val actions=pending.toList();pending.clear()
    if(result.responseCode==BillingClient.BillingResponseCode.OK)actions.forEach{it()}
    else onMessage("Google Play no está disponible en este dispositivo.")
   }
   override fun onBillingServiceDisconnected(){}
  })
 }
 private fun query(callback:(List<ProductDetails>)->Unit){
  val products=plans.mapNotNull{it.playProductId}.distinct().map{
   QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(BillingClient.ProductType.SUBS).build()
  }
  if(products.isEmpty()){callback(emptyList());return}
  client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(products).build()){result,data->
   if(closed)return@queryProductDetailsAsync
   if(result.responseCode==BillingClient.BillingResponseCode.OK)callback(data.productDetailsList)
   else onMessage("No se pudieron consultar los precios. Actualiza o reintenta.")
  }
 }
 fun buy(activity:Activity,plan:Plan,accountId:String){
  connect {query {details->
   val product=details.firstOrNull{it.productId==plan.playProductId}
   val offer=product?.subscriptionOfferDetails?.firstOrNull{it.basePlanId==plan.playBasePlanId && it.offerId==null}
   if(product==null || offer==null){onMessage("El plan aún no está disponible en Google Play.");return@query}
   val params=BillingFlowParams.newBuilder().setObfuscatedAccountId(accountId).setProductDetailsParamsList(listOf(
    BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).setOfferToken(offer.offerToken).build()
   )).build()
   val result=client.launchBillingFlow(activity,params)
   if(result.responseCode!=BillingClient.BillingResponseCode.OK)onMessage("No se pudo abrir la compra en Google Play.")
  }}
 }
 fun restore(){connect{
  client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()){result,purchases->
   if(closed)return@queryPurchasesAsync
   if(result.responseCode==BillingClient.BillingResponseCode.OK)process(purchases)
   else onMessage("No se pudieron restaurar las compras. Reintenta.")
  }
 }}
 private fun process(purchases:List<Purchase>){
  if(closed)return
  val tokens=purchases.filter{it.purchaseState==Purchase.PurchaseState.PURCHASED}.map{it.purchaseToken}
  if(tokens.isNotEmpty())onPurchases(tokens)
  if(purchases.any{it.purchaseState==Purchase.PurchaseState.PENDING})onMessage("Pago pendiente. El acceso se activará cuando Google Play confirme la compra.")
 }
 fun close(){closed=true;pending.clear();client.endConnection()}
}
