package com.mybetlive.data
data class BankBookmaker(val id:String,val name:String)
data class BankBookmakerInput(val name:String)
data class BankAccess(val advanced:Boolean=false,val maxActiveBanks:Int?=1,val primaryBankId:String?=null)
data class BankView(val id:String,val name:String,val currency:String,val timezone:String,val openedAt:String,val initialCapital:String,
 val archived:Boolean=false,val notes:String="",val defaultBookmaker:String="",val defaultSport:String="SOCCER",val defaultStake:String?=null,val defaultOdds:String?=null,
 val version:Long=0,val capital:String="0.00",val reserved:String="0.00",val available:String="0.00",val profit:String="0.00",val roi:String?=null,val writable:Boolean=false,val updatedAt:String="")
data class BankLeg(val id:String?=null,val title:String,val sport:String,val market:String?=null,val odds:String?=null,val trackedBetId:String?=null,val suggestedResult:String?=null)
data class BankBet(val id:String,val bankId:String,val placedAt:String,val format:String,val stake:String,val odds:String,val closingOdds:String?=null,
 val freeBet:Boolean=false,val bonusPercent:String="0",val commissionPercent:String="0",val bookmaker:String="",val notes:String="",val result:String="PENDING",
 val payout:String?=null,val exposure:String="0.00",val profit:String?=null,val settledAt:String?=null,val version:Long=0,val selections:List<BankLeg> = emptyList(),
 val suggestedResult:String?=null,val suggestedPayout:String?=null,val potentialReturn:String?=null)
data class BankPoint(val date:String,val value:String)
data class BankDay(val date:String,val bets:Long,val profit:String)
data class BankSummary(val bank:BankView,val access:BankAccess,val recent:List<BankBet>,val chart:List<BankPoint>,val calendar:List<BankDay>,val pending:Long,val totalBets:Long,val observedAt:String,val metrics:Map<String,String?> = emptyMap())
data class BankBreakdown(val label:String,val bets:Long,val profit:String,val roi:String?)
data class BankStatistics(val metrics:Map<String,String?>,val chart:List<BankPoint>,val calendar:List<BankDay>,val advanced:Boolean,val observedAt:String,val breakdowns:Map<String,List<BankBreakdown>> = emptyMap())
data class BankHistoryGroup(val type:String,val start:String,val bets:Long,val pending:Long,val gained:String,val lost:String,val net:String)
data class BankPage<T>(val content:List<T>,val page:Int,val totalPages:Int,val totalElements:Long,val groups:List<BankHistoryGroup> = emptyList())
data class BankMutation(val bank:BankView,val bet:BankBet?=null)
data class BankCommand(val operationId:String,val version:Long)
data class CreateBankInput(val operationId:String,val name:String,val currency:String,val timezone:String,val openedAt:String,val initialCapital:String)
data class EditBankInput(val operationId:String,val version:Long,val name:String,val timezone:String,val notes:String,val defaultBookmaker:String,
 val defaultSport:String,val defaultStake:String?,val defaultOdds:String?,val initialCapital:String?,val openedAt:String?,val archived:Boolean)
data class BankLegInput(val title:String,val sport:String,val market:String?,val odds:String?,val trackedBetId:String?)
data class BankBetInput(val operationId:String,val version:Long,val placedAt:String,val format:String,val stake:String?,val stakePercent:String?,
 val odds:String,val closingOdds:String?,val freeBet:Boolean,val bonusPercent:String,val commissionPercent:String,val bookmaker:String,val notes:String,val selections:List<BankLegInput>)
data class BankSettlement(val operationId:String,val version:Long,val result:String,val payout:String,val settledAt:String)
data class BankCsvInput(val kind:String,val csv:String)
data class BankCsvRow(val row:Int,val duplicate:Boolean,val error:String?,val title:String)
data class BankCsvPreview(val rows:List<BankCsvRow>,val valid:Int,val duplicates:Int,val invalid:Int,val checksum:String)
data class BankCsvImport(val operationId:String,val version:Long,val kind:String,val csv:String,val checksum:String)
data class BankPendingOperation(val operationId:String,val version:Long)
data class BankCache(val banks:List<BankView> = emptyList(),val summary:BankSummary?=null,val history:BankPage<BankBet>?=null,val statistics:BankStatistics?=null,val savedAt:String="",val pendingOperations:Map<String,BankPendingOperation> = emptyMap(),val from:String?=null,val to:String?=null,val bookmakers:List<BankBookmaker> = emptyList())
fun bankDecimal(value:String):String?=value.trim().replace(',','.').toBigDecimalOrNull()?.takeIf{it.scale()<=4}?.toPlainString()
fun bankMoney(value:String?,currency:String):String {
 if(value==null)return "—"
 val number=value.toBigDecimalOrNull()?:return "—"
 return java.text.NumberFormat.getCurrencyInstance(java.util.Locale.getDefault()).apply {this.currency=java.util.Currency.getInstance(currency)}.format(number)
}
fun bankPercent(value:String?):String=if(value==null)"—" else "$value%"

