package com.mybetlive.data
import retrofit2.http.*
import okhttp3.ResponseBody
interface BankApi {
 @GET("api/v1/banks/bookmakers") suspend fun bookmakers():List<BankBookmaker>
 @POST("api/v1/banks/bookmakers") suspend fun addBookmaker(@Body input:BankBookmakerInput):BankBookmaker
 @GET("api/v1/banks") suspend fun list():List<BankView>
 @POST("api/v1/banks") suspend fun create(@Body input:CreateBankInput):BankMutation
 @PUT("api/v1/banks/{id}") suspend fun edit(@Path("id") id:String,@Body input:EditBankInput):BankMutation
 @POST("api/v1/banks/{id}/primary") suspend fun primary(@Path("id") id:String,@Body input:BankCommand):BankMutation
 @GET("api/v1/banks/{id}/summary") suspend fun summary(@Path("id") id:String,@Query("from") from:String?,@Query("to") to:String?):BankSummary
 @GET("api/v1/banks/{id}/bets") suspend fun history(@Path("id") id:String,@Query("page") page:Int,@Query("size") size:Int=10,
  @Query("result") result:String?=null,@Query("sport") sport:String?=null,@Query("bookmaker") bookmaker:String?=null,@Query("from") from:String?=null,@Query("to") to:String?=null,@Query("q") query:String?=null):BankPage<BankBet>
 @GET("api/v1/banks/{id}/bets/{bet}") suspend fun detail(@Path("id") id:String,@Path("bet") bet:String):BankBet
 @POST("api/v1/banks/{id}/bets") suspend fun addBet(@Path("id") id:String,@Body input:BankBetInput):BankMutation
 @PUT("api/v1/banks/{id}/bets/{bet}") suspend fun editBet(@Path("id") id:String,@Path("bet") bet:String,@Body input:BankBetInput):BankMutation
 @POST("api/v1/banks/{id}/bets/{bet}/copy") suspend fun copy(@Path("id") id:String,@Path("bet") bet:String,@Body input:BankCommand):BankMutation
 @POST("api/v1/banks/{id}/bets/{bet}/delete") suspend fun delete(@Path("id") id:String,@Path("bet") bet:String,@Body input:BankCommand):BankMutation
 @POST("api/v1/banks/{id}/bets/{bet}/settle") suspend fun settle(@Path("id") id:String,@Path("bet") bet:String,@Body input:BankSettlement):BankMutation
 @GET("api/v1/banks/{id}/statistics") suspend fun statistics(@Path("id") id:String,@Query("from") from:String,@Query("to") to:String,
  @Query("sport") sport:String?,@Query("market") market:String?,@Query("bookmaker") bookmaker:String?):BankStatistics
 @POST("api/v1/banks/{id}/csv/preview") suspend fun preview(@Path("id") id:String,@Body input:BankCsvInput):BankCsvPreview
 @POST("api/v1/banks/{id}/csv/import") suspend fun importCsv(@Path("id") id:String,@Body input:BankCsvImport):BankMutation
 @GET("api/v1/banks/{id}/csv") suspend fun export(@Path("id") id:String,@Query("kind") kind:String,@Query("template") template:Boolean):ResponseBody
}
