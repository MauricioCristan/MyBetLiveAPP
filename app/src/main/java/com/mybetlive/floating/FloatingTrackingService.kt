package com.mybetlive.floating

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.TextView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.*
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.*
import com.mybetlive.MainActivity
import com.mybetlive.R
import com.mybetlive.application.*
import com.mybetlive.composition.ClientDependencies
import com.mybetlive.data.*
import kotlinx.coroutines.*

class FloatingTrackingService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    override val lifecycle=LifecycleRegistry(this)
    private val registry=SavedStateRegistryController.create(this)
    override val savedStateRegistry get()=registry.savedStateRegistry
    override val viewModelStore=ViewModelStore()
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private lateinit var manager:WindowManager
    private val picks=FloatingPickRegistry()
    private class Bubble(val id:String,var expanded:Boolean=false,var view:ComposeView?=null,var layout:WindowManager.LayoutParams?=null)
    private val windows=linkedMapOf<String,Bubble>()
    private var trash:TextView?=null
    private var locked=false
    private var owner:String?=null
    private var refreshing=false
    private var closed=false
    private var lastRead=0L
    private val live get()=ClientDependencies.live
    private val repository get()=ClientDependencies.repository(this)
    private val preferences by lazy{getSharedPreferences("floating-position",MODE_PRIVATE)}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private val screen get()=resources.displayMetrics
    private val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context?,i:Intent?){
        when(i?.action){Intent.ACTION_SCREEN_OFF->{locked=true;hideAll()};Intent.ACTION_USER_PRESENT->{locked=false;render();refresh()}}
    }}
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onCreate(){
        super.onCreate();registry.performAttach();registry.performRestore(null);lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        manager=getSystemService(WindowManager::class.java);locked=getSystemService(KeyguardManager::class.java).isKeyguardLocked
        val filter=IntentFilter().apply{addAction(Intent.ACTION_SCREEN_OFF);addAction(Intent.ACTION_USER_PRESENT)}
        if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,filter,RECEIVER_NOT_EXPORTED) else registerReceiver(receiver,filter)
        scope.launch{live.state.collect{if(owner!=null){if(it.owner!=owner)stopSelf() else{syncWindows();render()}}}}
        scope.launch{ClientDependencies.sync.foreground.collect{if(it)hideAll() else{render();refresh()}}}
        scope.launch{while(isActive){delay(1000);if(owner!=null&&(!Settings.canDrawOverlays(this@FloatingTrackingService)||repository.session?.trackingOwner()!=owner))stopSelf()}}
        scope.launch{while(isActive){delay(FloatingRefreshPolicy.INTERVAL_MS);refresh()}}
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
        if(intent?.action=="STOP"){stopSelf();return START_NOT_STICKY}
        val requestedOwner=intent?.getStringExtra("owner")
        if(requestedOwner==null||requestedOwner!=repository.session?.trackingOwner()||!Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY}
        val id=intent.getStringExtra("betId")
        if(id==null||live.state.value.bets.none{it.id==id&&it.status !in settledStatuses}){if(windows.isEmpty())stopSelf();return START_NOT_STICKY}
        owner=requestedOwner;picks.activate(id,live.state.value.bets)
        notification()
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        syncWindows();render();refresh()
        return START_NOT_STICKY
    }
    private fun translate(s:String)=com.mybetlive.i18n.AppStrings.translate(s,resources)
    private fun notification(){
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("floating-tracking",translate("Burbuja de seguimiento"),NotificationManager.IMPORTANCE_LOW))
        val open=PendingIntent.getActivity(this,731,Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop=PendingIntent.getService(this,732,Intent(this,FloatingTrackingService::class.java).setAction("STOP"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n=NotificationCompat.Builder(this,"floating-tracking").setSmallIcon(R.drawable.ic_mybet_notification).setContentTitle("MyBetLive")
            .setContentText(translate("Seguimientos en burbujas")).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .addAction(0,translate("Cerrar todas las burbujas"),stop).build()
        if(Build.VERSION.SDK_INT>=34)startForeground(731,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(731,n)
    }
    private fun syncWindows(){
        picks.sync(live.state.value.bets)
        val ids=picks.ids
        windows.keys.toList().filter{it !in ids}.forEach{hide(it);windows.remove(it)}
        ids.forEach{windows.getOrPut(it){Bubble(it)}}
        if(windows.isEmpty())stopSelf()
    }
    private fun refresh(){
        if(closed||owner==null||locked||ClientDependencies.sync.foreground.value||refreshing)return
        if(getSystemService(ConnectivityManager::class.java).activeNetwork==null){owner?.let(live::offline);return}
        val ids=picks.ids
        val hasActive=live.state.value.bets.any{it.id in ids&&it.status !in settledStatuses}
        if(!FloatingRefreshPolicy.due(SystemClock.elapsedRealtime(),lastRead,false,locked,true,hasActive))return
        refreshing=true
        scope.launch{try{ClientDependencies.sync.backgroundRead{
            if(repository.session?.trackingOwner()!=owner)return@backgroundRead
            lastRead=SystemClock.elapsedRealtime();repository.activeBets(ids)
        }}catch(e:CancellationException){throw e}catch(e:Exception){owner?.let(live::offline);if(e is retrofit2.HttpException&&e.code() in listOf(401,403))stopSelf()}
        finally{refreshing=false}}
    }
    private fun createView()=ComposeView(this).apply{
        id=View.generateViewId();setViewTreeLifecycleOwner(this@FloatingTrackingService)
        setViewTreeViewModelStoreOwner(this@FloatingTrackingService);setViewTreeSavedStateRegistryOwner(this@FloatingTrackingService)
    }
    private fun render(){
        if(closed||owner==null||windows.isEmpty())return
        if(locked||getSystemService(KeyguardManager::class.java).isKeyguardLocked||ClientDependencies.sync.foreground.value){hideAll();return}
        if(!Settings.canDrawOverlays(this)){stopSelf();return}
        windows.values.toList().sortedBy{it.expanded}.forEachIndexed{index,bubble->renderBubble(bubble,index)}
    }
    private fun renderBubble(bubble:Bubble,index:Int){
        if(bubble.view!=null)return
        val bet=live.state.value.bets.find{it.id==bubble.id}?:return
        val width=if(bubble.expanded)minOf(dp(342),screen.widthPixels-dp(32)) else dp(bet.compactFloating().widthDp)
        val defaultRight=if(index%2==0)preferences.getBoolean("right",true) else !preferences.getBoolean("right",true)
        val defaultY=(preferences.getFloat("y",.4f)+(index/2)*dp(104).toFloat()/screen.heightPixels).coerceAtMost(.8f)
        val params=WindowManager.LayoutParams(width,WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT).apply{
            gravity=Gravity.TOP or Gravity.LEFT
            x=if(bubble.expanded)(screen.widthPixels-width)/2 else if(preferences.getBoolean("right:${bubble.id}",defaultRight))screen.widthPixels-width-dp(8) else dp(8)
            val limit=if(bubble.expanded)(screen.heightPixels*.28f).toInt()-dp(36) else screen.heightPixels-dp(140)
            y=(preferences.getFloat("y:${bubble.id}",defaultY)*screen.heightPixels).toInt().coerceIn(dp(36),limit.coerceAtLeast(dp(36)))
        }
        bubble.layout=params
        if(!bubble.expanded)preferences.edit().putBoolean("right:${bubble.id}",params.x>screen.widthPixels/2)
            .putFloat("y:${bubble.id}",params.y.toFloat()/screen.heightPixels).apply()
        val view=createView();bubble.view=view
        view.setContent{
            val state by live.state.collectAsState()
            state.bets.find{it.id==bubble.id}?.let{row->FloatingPickView(row,state.offline,bubble.expanded,
                maxHeight=screen.heightPixels/resources.displayMetrics.density*.72f,
                expand={expand(bubble.id)},minimize={bubble.expanded=false;hide(bubble.id);render()},close={close(bubble.id)},
                open={openPick(bubble.id)},dragStart={showTrash()},drag={dx,dy->move(bubble.id,dx,dy)},dragEnd={finishDrag(bubble.id)})}
        }
        try{manager.addView(view,params)}catch(e:RuntimeException){bubble.view=null;view.disposeComposition();close(bubble.id)}
    }
    private fun expand(id:String){
        windows.values.forEach{if(it.expanded){it.expanded=false;hide(it.id)}}
        windows[id]?.expanded=true;hide(id);render()
    }
    private fun close(id:String){hide(id);windows.remove(id);picks.close(id);if(windows.isEmpty())stopSelf()}
    private fun openPick(id:String){windows[id]?.expanded=false;hideAll();startActivity(Intent(this,MainActivity::class.java).putExtra("betId",id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))}
    private fun hide(id:String){windows[id]?.let{b->b.view?.let{runCatching{manager.removeView(it)};it.disposeComposition()};b.view=null};hideTrash()}
    private fun hideAll(){windows.keys.toList().forEach(::hide);hideTrash()}
    private fun move(id:String,dx:Float,dy:Float){val b=windows[id]?:return;b.layout?.let{p->
        p.x=(p.x+dx).toInt().coerceIn(0,(screen.widthPixels-p.width).coerceAtLeast(0))
        p.y=(p.y+dy).toInt().coerceIn(dp(32),(screen.heightPixels-(b.view?.height?:dp(96))).coerceAtLeast(dp(32)))
        b.view?.let{runCatching{manager.updateViewLayout(it,p)}}
    }}
    private fun showTrash(){if(trash!=null)return
        val v=TextView(this).apply{text="×";textSize=28f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.rgb(112,40,50))};contentDescription=translate("Cerrar burbuja")}
        val p=WindowManager.LayoutParams(dp(64),dp(64),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL;y=dp(44)}
        runCatching{manager.addView(v,p);trash=v}
    }
    private fun hideTrash(){trash?.let{runCatching{manager.removeView(it)}};trash=null}
    private fun finishDrag(id:String){hideTrash();val b=windows[id]?:return;val p=b.layout?:return
        if(p.y>screen.heightPixels-dp(180)&&kotlin.math.abs(p.x+p.width/2-screen.widthPixels/2)<dp(90)){close(id);return}
        val right=p.x+p.width/2>screen.widthPixels/2;p.x=if(right)screen.widthPixels-p.width-dp(8) else dp(8)
        preferences.edit().putBoolean("right:$id",right).putFloat("y:$id",p.y.toFloat()/screen.heightPixels).apply()
        b.view?.let{runCatching{manager.updateViewLayout(it,p)}}
    }
    override fun onConfigurationChanged(newConfig:android.content.res.Configuration){super.onConfigurationChanged(newConfig);hideAll();render()}
    override fun onDestroy(){closed=true;scope.cancel();hideAll();runCatching{unregisterReceiver(receiver)};stopForeground(STOP_FOREGROUND_REMOVE)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);viewModelStore.clear();super.onDestroy()}
}
