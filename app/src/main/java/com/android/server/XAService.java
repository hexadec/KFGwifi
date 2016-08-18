package com.android.server;
import de.robv.android.xposed.*;
import android.content.*;
import android.os.*;
import com.android.server.XAService;
import android.net.*;
import java.util.*;
import android.util.*;
import android.app.*;
import android.net.wifi.*;
import android.content.pm.*;
//import de.robv.android.xposed.IXposedMod

/**Use Xposed method to inform the OS that the network is ready to be used, probably only works on Android 5.X*/
public class XAService extends android.os.IXAService.Stub {

private Context mContext;
private static XAService oInstance;
public boolean mSystemReady = false;
private ArrayList<Intent> mDeferredBroadcasts;
	private static final String ACTION_CAPTIVE_PORTAL_LOGGED_IN =
	"android.net.netmon.captive_portal_logged_in";
	
		private static final String ACTION="hu.kfg.wifimanager.LOGGED_IN";
		private BroadcastReceiver yourReceiver;

	
		public IBinder onBind(Intent arg0) {
			return null;
		}
			
	public static void inject(final Class ActivityManagerServiceClazz) {
	XposedBridge.log("KFG injecting class");
//final Class ActivityManagerServiceClazz = XposedHelpers.findClass("com.android.server.am.ActivityManagerService", null);
/*XposedBridge.hookAllMethods(ActivityManagerServiceClazz, "main", 

new XC_MethodHook() {

@Override
protected final void afterHookedMethod(final MethodHookParam param) {
try {
Context context = (Context) param.getResult();*/
try {
oInstance = new XAService();
XposedBridge.log("KFG Method hooked1"+(oInstance==null));
XposedHelpers.callStaticMethod(
Class.forName("android.os.ServiceManager"), "addService", 
new Class[]{String.class, IBinder.class}, "user.kfgwifi-service", oInstance);
	XposedBridge.hookAllMethods(
		ActivityManagerServiceClazz, 
		"systemReady", 
		new XC_MethodHook() {
			@Override
			protected final void afterHookedMethod(final MethodHookParam param) {
				XposedBridge.log("KFG Method hooked"+(oInstance==null));
				oInstance.systemReady();
			}
		}
	);
} catch (Exception e){
	XposedBridge.log("KFG error");
	e.printStackTrace();
}
/*}
}
);
*/


	/*XposedBridge.hookAllMethods(
		ActivityManagerServiceClazz, 
		"systemReady", 
		new XC_MethodHook() {
			@Override
			protected final void afterHookedMethod(final MethodHookParam param) {
				XposedBridge.log("KFG Method hooked"+(oInstance==null));
				oInstance.systemReady();
			}
		}
	);*/

}

public XAService() {
	//mContext = context;
	
}

@Override
public void sendBroadcast() throws RemoteException
{
	// TODO: Implement this method
}

public void sendBroadcasta(Context context){
	mContext = context;
	WifiManager wifiManager = (WifiManager)mContext.getSystemService(mContext.WIFI_SERVICE);              
	int mNetId = -1;         
	for (WifiConfiguration tmp : wifiManager.getConfiguredNetworks()) {
		if (tmp.SSID.equals( "\""+"kfg"+"\"")) 
		{mNetId = tmp.networkId; }
		}
	Intent intent = new Intent(ACTION_CAPTIVE_PORTAL_LOGGED_IN);
	intent.putExtra(Intent.EXTRA_TEXT, String.valueOf(mNetId));
	intent.putExtra("result","1");
	Log.d("KFGwifiService","Broadcasting....");
	SystemClock.sleep(1000);
	sendBroadcast(intent);
	//intent = new Intent("hu.test.TEST");
	//sendBroadcast(intent);
	        // sendStickyBroadcast(intent);
			
	     }
		 
	private void sendBroadcast(Intent intent) {
		synchronized(this) {
			             if (mSystemReady) {
							 try {

								 mContext.sendBroadcastAsUser(intent,android.os.Process.myUserHandle());
								 
							} catch (Exception e){
								Log.e("KFGwifiService","Error sending broadcast");
								e.printStackTrace();
							}
				             } else {
				                 if (mDeferredBroadcasts == null) {
					                     mDeferredBroadcasts = new ArrayList<Intent>();
					                 }
				                 mDeferredBroadcasts.add(intent);
				             }
			         }
		 }
	 
	     void systemReady() {
			 try {
				 mContext = AndroidAppHelper.currentApplication().createPackageContext("hu.kfg.wifimanager", Context.CONTEXT_IGNORE_SECURITY);
			 } catch (Exception e){
				 XposedBridge.log("Context error");
				 XposedBridge.log(e.getMessage());
				 try {
					 mContext = AndroidAppHelper.currentApplication().createPackageContext("com.android.server", Context.CONTEXT_IGNORE_SECURITY);
				 } catch (Exception ee){
					 XposedBridge.log("Context error");
					 XposedBridge.log(ee.getMessage());
				 }
			 }
			 final IntentFilter theFilter = new IntentFilter();
			 theFilter.addAction(ACTION);
			 yourReceiver = new BroadcastReceiver() {

				 @Override
				 public void onReceive(Context context, Intent intent) {
					 // Do whatever you need it to do when it receives the broadcast
					 // Example show a Toast message...
					 Log.d("KFGwifiService","Received, uid/pid/userhandle"+android.os.Process.myUid()+"/"+android.os.Process.myPid()+"/"+android.os.Process.myUserHandle());
						sendBroadcasta(context);
				 }
			 };
			 mContext.registerReceiver(yourReceiver, theFilter);
			 Log.d("KFGwifiService","Receiver registered...");
			 XposedBridge.log("KFGwifiService registered");
		       synchronized(this) {
		          mSystemReady = true;
		           if (mDeferredBroadcasts != null) {
		                 int count = mDeferredBroadcasts.size();
			           for (int i = 0; i < count; i++) {
						   try {
							   mContext.sendBroadcastAsUser(mDeferredBroadcasts.get(i),android.os.Process.myUserHandle());
		               		//mContext.sendBroadcast(mDeferredBroadcasts.get(i));
					   } catch (Exception e){
						   Log.e("KFGwifiService","Error sending broadcast");
						   e.printStackTrace();
					   }
			              }
				            mDeferredBroadcasts = null;
				           }
			        }
		     }
			 
	


}

