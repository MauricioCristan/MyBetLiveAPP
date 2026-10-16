package com.mybetlive.bank
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mybetlive.application.BankOperations
import com.mybetlive.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.*
import java.util.UUID
data class BankState(val banks:List<BankView> = emptyList(),val summary:BankSummary?=null,val history:BankPage<BankBet>?=null,
 val statistics:BankStatistics?=null,val busy:Boolean=false,val offline:Boolean=false,
 val error:String?=null,val message:String?=null,val selectedBet:BankBet?=null,val preview:BankCsvPreview?=null,
 val from:String=LocalDate.now().withDayOfMonth(1).toString(),val to:String=LocalDate.now().toString(),val observedAt:String?=null,val bookmakers:List<BankBookmaker> = emptyList(),val historyLoadingMore:Boolean=false,val historyMoreError:String?=null,val historyRevision:Int=0)
class BankViewModel(private val repository:BankOperations,private val operationTimeoutMillis:Long=45_000):ViewModel(){
 private val owner=repository.owner()
 private val mutable=MutableStateFlow(repository.cached().let{BankState(it.banks,it.summary,it.history,it.statistics,offline=it.banks.isNotEmpty(),observedAt=it.savedAt.takeIf(String::isNotBlank),from=it.from?:LocalDate.now().withDayOfMonth(1).toString(),to=it.to?:LocalDate.now().toString(),bookmakers=it.bookmakers)})
 val state=mutable.asStateFlow()
 private val pending=repository.cached().pendingOperations.toMutableMap()
 private data class HistoryFilter(val result:String?,val sport:String?,val bookmaker:String?,val from:String?,val to:String?,val query:String?=null)
 private var historyFilter=HistoryFilter(null,null,null,null,null)
 private var statisticsFilter=Triple<String?,String?,String?>(null,null,null)
 private fun ensureOwner(){if(owner==null||owner!=repository.owner()){mutable.value=BankState();throw CancellationException("La cuenta cambió")}}
 private fun cache(){ensureOwner();val s=mutable.value;repository.remember(BankCache(s.banks,s.summary,s.history,s.statistics,s.observedAt.orEmpty(),pending.toMap(),s.from,s.to,s.bookmakers))}
 private fun error(e:Exception){if(e is CancellationException)throw e;mutable.update{it.copy(error=ClientErrorMessages().message(e),offline=e is java.io.IOException||e is retrofit2.HttpException&&e.code()>=500)}}
 private fun action(block:suspend()->Unit){
  if(mutable.value.busy)return
  viewModelScope.launch{
   mutable.update{it.copy(busy=true,error=null,message=null)}
   try{withTimeout(operationTimeoutMillis){ensureOwner();block();ensureOwner()}}
   catch(e:TimeoutCancellationException){
    ensureOwner()
    mutable.update{it.copy(error="El servidor tardó demasiado. Actualiza el bank antes de reintentar; tu operación no se duplicará.",offline=true)}
   }
   catch(e:Exception){error(e)}
   finally{mutable.update{it.copy(busy=false)}}
  }
 }
 fun clearMessage(){mutable.update{it.copy(error=null,message=null)}}
 fun disposeIfChanged(){if(owner!=repository.owner()){mutable.value=BankState();pending.clear()}}
 fun reload(id:String?=mutable.value.summary?.bank?.id)=action{load(id)}
 private suspend fun load(id:String?){
  val bookmakers=repository.call{it.bookmakers()};ensureOwner();mutable.update{it.copy(bookmakers=bookmakers)}
  val rows=repository.call{it.list()};ensureOwner();val selected=id?.let{selected->rows.find{it.id==selected}}?:rows.firstOrNull{!it.archived}?:rows.firstOrNull()
  mutable.update{it.copy(banks=rows)}
  if(selected==null){mutable.update{it.copy(summary=null,history=null,statistics=null,offline=false,observedAt=Instant.now().toString())};cache();return}
  if(mutable.value.summary?.bank?.id!=selected.id){historyFilter=HistoryFilter(null,null,null,null,null);statisticsFilter=Triple(null,null,null);val today=LocalDate.now(ZoneId.of(selected.timezone));mutable.update{it.copy(from=today.withDayOfMonth(1).toString(),to=today.toString())}}
  val summary=repository.call{it.summary(selected.id,mutable.value.from,mutable.value.to)};ensureOwner()
  val filter=historyFilter.copy(from=mutable.value.from,to=mutable.value.to);val history=repository.call{it.history(selected.id,0,result=filter.result,sport=filter.sport,bookmaker=filter.bookmaker,from=filter.from,to=filter.to,query=filter.query)};ensureOwner()
  historyFilter=filter
  mutable.update{it.copy(summary=summary,history=history,historyMoreError=null,historyRevision=it.historyRevision+1,statistics=null,selectedBet=null,preview=null,offline=false,observedAt=summary.observedAt)};cache()
 }
 fun select(id:String){if(mutable.value.offline){if(mutable.value.summary?.bank?.id!=id)mutable.update{it.copy(error="Este bank no tiene datos guardados. Conéctate para consultarlo.")};return};reload(id)}
 fun addBookmaker(name:String,onSuccess:(String)->Unit)=action{
  if(mutable.value.offline)throw java.io.IOException("Conéctate antes de modificar el bank.")
  val row=repository.call{it.addBookmaker(BankBookmakerInput(name))};ensureOwner()
  mutable.update{it.copy(bookmakers=(it.bookmakers.filterNot{b->b.id==row.id}+row).sortedBy{b->b.name.lowercase()},message="Casa de apuestas guardada.")}
  cache();onSuccess(row.name)
 }
 fun period(from:String,to:String)=action{val previous=mutable.value;mutable.update{it.copy(from=from,to=to)};try{load(previous.summary?.bank?.id)}catch(e:Exception){mutable.update{it.copy(from=previous.from,to=previous.to)};throw e}}
 fun history(page:Int,result:String?,sport:String?,bookmaker:String?,from:String?,to:String?,query:String?=null)=action{
  val id=mutable.value.summary?.bank?.id?:return@action
  val period=mutable.value
  val rows=repository.call{it.history(id,page,result=result,sport=sport,bookmaker=bookmaker,from=period.from,to=period.to,query=query)};ensureOwner();mutable.update{it.copy(history=rows,offline=false,historyMoreError=null,historyRevision=it.historyRevision+1)};cache()
  historyFilter=HistoryFilter(result,sport,bookmaker,period.from,period.to,query)
 }
 fun historyPage(page:Int){val f=historyFilter;history(page,f.result,f.sport,f.bookmaker,f.from,f.to,f.query)}
 fun loadMoreHistory(){
  val current=mutable.value;val previous=current.history?:return
  if(current.busy||current.historyLoadingMore||previous.page+1>=previous.totalPages)return
  val bankId=current.summary?.bank?.id?:return;val filter=historyFilter
  action{
   mutable.update{it.copy(historyLoadingMore=true,historyMoreError=null)}
   try{
    val next=repository.call{it.history(bankId,previous.page+1,result=filter.result,sport=filter.sport,bookmaker=filter.bookmaker,from=filter.from,to=filter.to,query=filter.query)}
    ensureOwner()
    if(mutable.value.summary?.bank?.id!=bankId)return@action
    mutable.update{it.copy(history=next.copy(content=(previous.content+next.content).distinctBy{bet->bet.id}),offline=false)};cache()
   }catch(e:Exception){
    if(e is CancellationException&&e !is TimeoutCancellationException)throw e
    mutable.update{it.copy(historyMoreError="No se pudieron cargar más apuestas. Reintenta.")}
    if(e is TimeoutCancellationException)throw e
   }finally{mutable.update{it.copy(historyLoadingMore=false)}}
  }
 }
 fun statistics(sport:String?,market:String?,bookmaker:String?)=action{val id=mutable.value.summary?.bank?.id?:return@action;val data=repository.call{it.statistics(id,mutable.value.from,mutable.value.to,sport,market,bookmaker)};ensureOwner();statisticsFilter=Triple(sport,market,bookmaker);mutable.update{it.copy(statistics=data,offline=false)};cache()}
 fun refreshStatistics(){val f=statisticsFilter;statistics(f.first,f.second,f.third)}
 fun openBet(bet:BankBet)=action{if(mutable.value.offline){mutable.update{it.copy(selectedBet=bet)};return@action};val data=repository.call{it.detail(bet.bankId,bet.id)};ensureOwner();mutable.update{it.copy(selectedBet=data)}}
 fun closeBet(){mutable.update{it.copy(selectedBet=null)}}
 fun write(key:String,callback:suspend(BankApi,String,Long)->BankMutation,onSuccess:()->Unit={})=action{
  if(mutable.value.offline)throw java.io.IOException("Conéctate antes de modificar el bank.")
  val bank=mutable.value.summary?.bank;val pendingKey=(bank?.id?:"")+"|"+key
  val op=pending.getOrPut(pendingKey){BankPendingOperation(UUID.randomUUID().toString(),bank?.version?:0L)};cache()
  try{
   val result=repository.call{callback(it,op.operationId,op.version)};ensureOwner();pending.remove(pendingKey)
   mutable.update{it.copy(banks=it.banks.filterNot{b->b.id==result.bank.id}+result.bank,summary=it.summary?.takeIf{s->s.bank.id==result.bank.id}?.copy(bank=result.bank),message="Cambios guardados.",selectedBet=result.bet)}
   cache();onSuccess()
   try{load(result.bank.id);mutable.update{it.copy(message="Cambios guardados.")}}catch(e:Exception){if(e is CancellationException)throw e;error(e);mutable.update{it.copy(message="El cambio se guardó. No se pudo actualizar el historial; pulsa Actualizar.")}}
  }catch(e:retrofit2.HttpException){if(e.code() in 400..499&&e.code()!=408){pending.remove(pendingKey);cache()};if(e.code()==409){try{load(bank?.id)}catch(refresh:Exception){if(refresh is CancellationException)throw refresh}};throw e}
 }
 fun preview(kind:String,csv:String)=action{val id=mutable.value.summary?.bank?.id?:return@action;mutable.update{it.copy(preview=null)};val data=repository.call{it.preview(id,BankCsvInput(kind,csv))};ensureOwner();mutable.update{it.copy(preview=data)}}
 fun export(kind:String,template:Boolean,receive:(String)->Unit)=action{
  val id=mutable.value.summary?.bank?.id?:return@action;val text=repository.call{it.export(id,kind,template).use{body->body.string()}};ensureOwner();receive(text)
 }
}

