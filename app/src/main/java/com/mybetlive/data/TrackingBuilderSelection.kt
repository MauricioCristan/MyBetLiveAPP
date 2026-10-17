package com.mybetlive.data

data class TrackingBuilderSelection(val market:String,val selection:String,val line:Double?=null,val team:String?=null,
 val playerId:String?=null,val scope:String="MATCH",val targetScore:String?=null,
 val status:String?=null,val currentValue:Double?=null,val remaining:Double?=null){
 fun title(event:EventSnapshot)=selectionTitle(market,selection,line,team,null,event,scope,targetScore)
 fun asBet(parent:BetView)=parent.copy(market=market,selection=selection,line=line,team=team,scope=scope,targetScore=targetScore,
  status=status?:"PENDING",currentValue=currentValue,remaining=remaining,builderSelections=null)
}

private data class BuilderPushState(val status:String,val currentValue:Double?,val remaining:Double?)
internal fun BetView.builderPush(payload:String?):List<TrackingBuilderSelection>? {
 val legs=builderSelections?:return null
 if(payload==null||legs.isEmpty())return legs
 return runCatching{
  require(payload.length<6000)
  val states=com.google.gson.Gson().fromJson(payload,Array<BuilderPushState>::class.java)
  require(states.size==legs.size&&states.all{it.status in statusLabels&&(it.currentValue==null||it.currentValue.isFinite())&&(it.remaining==null||it.remaining.isFinite())})
  legs.mapIndexed{i,leg->leg.copy(status=states[i].status,currentValue=states[i].currentValue,remaining=states[i].remaining)}
 }.getOrDefault(legs)
}
