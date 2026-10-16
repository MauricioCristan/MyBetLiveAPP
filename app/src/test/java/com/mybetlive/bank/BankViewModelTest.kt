package com.mybetlive.bank
import android.os.Looper
import com.mybetlive.application.BankOperations
import com.mybetlive.data.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.IOException
import java.lang.reflect.Proxy
import kotlinx.coroutines.delay
import java.time.Duration
@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class BankViewModelTest {
 private class Fake:BankOperations {
  var who:String?="one"
  val bank=BankView("bank","Personal","MXN","America/Mexico_City","2026-01-01T00:00:00Z","1000",version=5,capital="1000.00",available="1000.00",writable=true)
  var data=BankCache(listOf(bank),BankSummary(bank,BankAccess(),"".let{emptyList()},listOf(BankPoint("2026-01-01","0")),emptyList(),0,0,"2026-01-02T00:00:00Z"))
  var failBefore=false;var failAfter=false;var hang=false;var writes=0;val requests=mutableListOf<BankCommand>()
  val applied=mutableMapOf<String,BankMutation>()
  var historyRows:List<BankBet> = emptyList();val historyRequests=mutableListOf<List<Any?>>()
  val summaryRequests=mutableListOf<List<Any?>>()
  val statisticsRequests=mutableListOf<List<Any?>>()
  val api=Proxy.newProxyInstance(BankApi::class.java.classLoader,arrayOf(BankApi::class.java)){_,method,args->
   when(method.name){
    "list"->data.banks
    "bookmakers"->data.bookmakers
    "addBookmaker"->{val name=(args!![0] as BankBookmakerInput).name;val row=data.bookmakers.find{it.name.equals(name,true)}?:BankBookmaker("house-"+data.bookmakers.size,name);data=data.copy(bookmakers=(data.bookmakers.filterNot{it.id==row.id}+row));row}
    "summary"->{summaryRequests.add(args!!.take(3));data.summary!!}
    "statistics"->{statisticsRequests.add(args!!.take(6));BankStatistics(emptyMap(),emptyList(),emptyList(),true,"2026-01-02T00:00:00Z")}
    "history"->{val page=args!![1] as Int;historyRequests.add(args.take(9));BankPage(historyRows.drop(page*10).take(10),page,(historyRows.size+9)/10,historyRows.size.toLong())}
    "primary"->{val input=args!![1] as BankCommand;requests.add(input);applied.getOrPut(input.operationId){writes++;val changed=bank.copy(version=bank.version+1);data=data.copy(banks=listOf(changed),summary=data.summary!!.copy(bank=changed));BankMutation(changed)}}
    else->error(method.name)
   }
  } as BankApi
  override fun owner()=who
  override fun cached()=data
  override fun remember(cache:BankCache){data=cache}
  override suspend fun <T> call(action:suspend(BankApi)->T):T {if(hang)delay(Long.MAX_VALUE);if(failBefore)throw IOException("offline");val result=action(api);if(failAfter){failAfter=false;throw IOException("lost response")};return result}
 }
 private fun idle()=shadowOf(Looper.getMainLooper()).idle()
 @Test fun historyAppendsTenAtATimeAndStopsAtTheLastPage(){
  val repository=Fake();repository.historyRows=(1..21).map{BankBet("bet-$it","bank","2026-01-01T12:00:00Z","BACK","10","2",selections=listOf(BankLeg(title="Pick $it",sport="SOCCER")))}
  val model=BankViewModel(repository);model.reload();idle()
  assertEquals(10,model.state.value.history!!.content.size)
  model.loadMoreHistory();idle();assertEquals(20,model.state.value.history!!.content.size)
  model.loadMoreHistory();idle();assertEquals(21,model.state.value.history!!.content.size)
  model.loadMoreHistory();idle();assertEquals(listOf(0,1,2),repository.historyRequests.map{it[1]})
  assertEquals(21,model.state.value.history!!.content.distinctBy{it.id}.size)
 }
 @Test fun failedNextPageKeepsRowsAndRetryPreservesSearchAndFilters(){
  val repository=Fake();repository.historyRows=(1..21).map{BankBet("bet-$it","bank","2026-01-01T12:00:00Z","BACK","10","2",selections=listOf(BankLeg(title="Pick $it",sport="SOCCER")))}
  val model=BankViewModel(repository);model.reload();idle()
  model.history(0,"PENDING","SOCCER","Casino",null,null,"Madrid");idle()
  repository.failBefore=true;model.loadMoreHistory();idle()
  assertEquals(10,model.state.value.history!!.content.size);assertNotNull(model.state.value.historyMoreError);assertFalse(model.state.value.historyLoadingMore)
  repository.failBefore=false;model.loadMoreHistory();idle()
  assertEquals(20,model.state.value.history!!.content.size);assertNull(model.state.value.historyMoreError)
  assertEquals("PENDING",repository.historyRequests.last()[3]);assertEquals("Madrid",repository.historyRequests.last()[8])
  model.history(0,null,null,null,null,null);idle();assertEquals(10,model.state.value.history!!.content.size)
 }
 @Test fun sharedPeriodReachesSummaryHistorySearchPaginationAndStatistics(){
  val repository=Fake();repository.historyRows=(1..21).map{BankBet("bet-$it","bank","2026-01-01T12:00:00Z","BACK","10","2",selections=listOf(BankLeg(title="Pick $it",sport="SOCCER")))}
  val model=BankViewModel(repository);model.reload();idle()
  model.statistics("SOCCER",null,"Casino");idle()
  model.period("2026-01-01","2026-02-28");idle()
  assertEquals(listOf("bank","2026-01-01","2026-02-28"),repository.summaryRequests.last())
  assertEquals(listOf("2026-01-01","2026-02-28"),repository.historyRequests.last().slice(6..7))
  assertEquals(10,model.state.value.history!!.content.size)
  model.history(0,"PENDING",null,null,null,null,"Madrid");idle();model.loadMoreHistory();idle()
  assertEquals(listOf("2026-01-01","2026-02-28"),repository.historyRequests.last().slice(6..7))
  model.refreshStatistics();idle()
  assertEquals(listOf("bank","2026-01-01","2026-02-28","SOCCER",null,"Casino"),repository.statisticsRequests.last())
  repository.failBefore=true;model.period("2026-03-01","2026-03-31");idle()
  assertEquals("2026-01-01",model.state.value.from);assertEquals("2026-02-28",repository.data.to)
 }
 @Test fun personalBookmakersPersistWithoutCreatingFinancialOperations(){
  val repository=Fake();val model=BankViewModel(repository);model.reload();idle()
  var selected="";model.addBookmaker("Mi Casino"){selected=it};idle()
  assertEquals("Mi Casino",selected);assertEquals(1,repository.data.bookmakers.size)
  model.addBookmaker("mi casino"){selected=it};idle()
  assertEquals(1,model.state.value.bookmakers.size);assertEquals(0,repository.writes)
  assertTrue(repository.data.pendingOperations.isEmpty())
  val restored=BankViewModel(repository);assertEquals("Mi Casino",restored.state.value.bookmakers.single().name)
  repository.who=null;restored.disposeIfChanged();assertTrue(restored.state.value.bookmakers.isEmpty())
 }
 @Test fun offlineKeepsLastSnapshotAndNeverQueuesFinancialWrites(){
  val repository=Fake();repository.data=repository.data.copy(from="2026-01-01",to="2026-01-31");val model=BankViewModel(repository);repository.failBefore=true;model.reload();idle()
  assertTrue(model.state.value.offline);assertEquals("1000.00",model.state.value.summary!!.bank.capital)
  assertEquals("2026-01-01",model.state.value.from);assertEquals("2026-01-31",model.state.value.to)
  model.write("primary",{api,op,v->api.primary("bank",BankCommand(op,v))});idle();assertEquals(0,repository.writes)
 }
 @Test fun unknownWriteOutcomeReusesDurableOperationAcrossViewModels(){
  val repository=Fake();val first=BankViewModel(repository);first.reload();idle();repository.failAfter=true
  first.write("primary",{api,op,v->api.primary("bank",BankCommand(op,v))});idle();assertTrue(first.state.value.offline);assertEquals(1,repository.data.pendingOperations.size)
  val second=BankViewModel(repository);second.reload();idle();second.write("primary",{api,op,v->api.primary("bank",BankCommand(op,v))});idle()
  assertEquals(1,repository.writes);assertEquals(repository.requests[0],repository.requests[1]);assertTrue(repository.data.pendingOperations.isEmpty())
 }
 @Test fun logoutDiscardsThePreviousAccountsUiAndDoesNotWrite(){
  val repository=Fake();val model=BankViewModel(repository);repository.who=null;model.disposeIfChanged();assertTrue(model.state.value.banks.isEmpty())
  model.write("primary",{api,op,v->api.primary("bank",BankCommand(op,v))});idle();assertEquals(0,repository.writes)
 }
 @Test fun bankAmountTextNeverPassesThroughFloatingPoint(){
  assertEquals("0.10",bankDecimal("0,10"));assertEquals("999999999999999999.99",bankDecimal("999999999999999999.99"));assertNull(bankDecimal("1.000.00"));assertEquals("—",bankPercent(null))
 }
 @Test fun hangingWriteStopsLoadingAndRetainsItsOperationForSafeRetry(){
  val repository=Fake();val model=BankViewModel(repository,100);model.reload();idle();repository.hang=true
  model.write("primary",{api,op,v->api.primary("bank",BankCommand(op,v))});idle()
  assertTrue(model.state.value.busy)
  shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(150))
  assertFalse(model.state.value.busy);assertTrue(model.state.value.offline)
  assertNotNull(model.state.value.error);assertEquals(1,repository.data.pendingOperations.size)
  val operation=repository.data.pendingOperations.values.single()
  repository.hang=false;model.reload();idle();model.write("primary",{api,op,v->api.primary("bank",BankCommand(op,v))});idle()
  assertEquals(operation.operationId,repository.requests.single().operationId);assertEquals(1,repository.writes)
 }
}


