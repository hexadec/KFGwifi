package hu.kfg.wifimanager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.util.Log;
import android.util.Base64;
import android.os.SystemClock;
import android.widget.*;
import android.net.*;
import android.app.*;
import org.apache.http.protocol.*;
import android.os.*;
import hu.hexadecimal.textsecure.*;
import org.apache.http.params.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;


public class KFGreceiver extends BroadcastReceiver {
	boolean isFirstTry = true;
	int runTimes = 0;
	public static boolean firstConnect = true;
	public static boolean firstDHCP = true;
	public static short numberOfSessions = 0;
	public static int numOfTries = 0;
	private Context con;
	private final Handler showSuccessToast = new Handler() {
		public void handleMessage(Message msg) {
		}
	};
	
	   @Override
	   public void onReceive(final Context context, final Intent intent) {
		   final String TAG = "KFGreceiver";
		   boolean user = intent.getAction().equals(Intent.ACTION_USER_PRESENT);
		   final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		   final boolean autologin = pref.getBoolean("autologin", false);
		   final boolean stamina = pref.getBoolean("stamina", false);
		   final boolean statech = intent.getAction().equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		   final boolean statech2 = intent.getAction().equals("android.net.wifi.STATE_CHANGE");
		   Log.d("KFGreceiver",intent.getAction());
		   if (!((statech||statech2||user||intent.getAction().equals("hu.kfg.wifimanager.MANUAL_LOGIN"))&&autologin)){
			   return;
		   }
		   if(user&&(!stamina)){
			   return;
		   }
		   if (!(statech||statech2)){
			   NetworkInfo mWifi =  ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			   if (!mWifi.isConnected()) {
				   if (!user){
					   Log.d(TAG,"WiFi not connected!");
				   }
					return;
				} else{
				Log.d(TAG,user?"Screen unlock event":"WiFi connected!");
				}
			}
			con = context;
		   final boolean security = pref.getBoolean("security", true);
			new Thread(new Runnable() {

						@Override
						public void run() {
							Log.d(TAG,"Autologin-thread started... " + ++numberOfSessions);
							WifiManager mWifi =  ((WifiManager) context.getSystemService(Context.WIFI_SERVICE));
							if (mWifi.getConnectionInfo()!=null) {
								runTimes = 0;
								
		   						WifiInfo wifiInfo = mWifi.getConnectionInfo();
	           						String ssid = wifiInfo.getSSID();
									
									DhcpInfo dhcp = null;
									dhcp = mWifi.getDhcpInfo();
									Log.d(TAG,"Dhcp null: "+(dhcp==null));
									if (security) {
									try {
										
										int i = dhcp.ipAddress;
									} catch (Exception e) {
										if (ssid.equals("\"kfg\"")) {
										Log.e(TAG,"DHCP is null!");
										if (!firstDHCP) {
											notifyIfFailed(1,context);
										} else {
											SystemClock.sleep(4000);
											firstDHCP = false;
											onReceive(context,intent);
										}
											
										}
										return;
									}
									}
								
									if (ssid.equals("\"kfg\"")&&((!security||((intToIp(dhcp.gateway).startsWith("172."))&&(intToIp(dhcp.netmask).equals("255.255.0.0")))))&&(firstConnect||intent.getAction().equals("hu.kfg.wifimanager.MANUAL_LOGIN")||intent.getAction().equals(Intent.ACTION_USER_PRESENT))) {
										firstConnect = false;
										if (security) {
										String[] macAddress = context.getResources().getStringArray(R.array.macAddresses);
										boolean match = false;
										for (String macAddress1 : macAddress) {
											if (macAddress1.equals(wifiInfo.getBSSID())||macAddress1.toLowerCase().equals(wifiInfo.getBSSID())) match = true;
										}
										if (!match){ 
											Log.d(TAG,"Different wifi with the name \"kfg\"  MAC!");
											showSuccessToast.postAtFrontOfQueue(new Runnable() {
													@Override
													public void run () {
														Toast.makeText(context,"A wifi is connected with the name \"kfg\", but it is not the real one!\nDo not log in here!",Toast.LENGTH_SHORT).show();
													}
												});
											return;
										}
										}
										Log.d(TAG,"kfg connected, login in progress...");
										final String username = pref.getString("username", "");
										final String password2 = pref.getString("b64", "");
										if (username.equals("")||password2.equals("")){
											return;
										}
										final boolean timeout = pref.getBoolean("timeout", false);
										String txt = "";
										try{
											txt = new EncryptUtils().cryptThreedog(new EncryptUtils().base64decode(password2),true,username);
										} catch (Exception e){
											Log.e(TAG,"Decryption failed");
											notifyIfFailed(1,context);
										}
	            						int i = randInt(90, 210);
										if (statech) {
										try {
											Thread.sleep(400+i);
										} catch (InterruptedException e) {
											Log.d(TAG,"Thread sleep failed");
										}
										}
	            						if (connect(txt,username,context,intent)&&timeout)  {
											Log.d(TAG,"Starting TimeoutKiller...");
	            							AlarmManager alarmManager=(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	            							Intent intente = new Intent(context, TimeoutKiller.class);
	            							PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intente, 0);
											if (Build.VERSION.SDK_INT >= 19) {
												alarmManager.setExact(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+(i*1000),pendingIntent);
											} else {
												alarmManager.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+(i*1000),pendingIntent);
											}
	            							//alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis(),i*1000,pendingIntent);
						
	            						} else {
											Log.d(TAG,"Not starting TimeoutKiller");
										}
	            	
	            					} else {
										if (!ssid.equals("\"kfg\"")){
											firstConnect = true;
											numberOfSessions = 0;
											Log.d(TAG,"Different Wi-Fi name");
						
										} else {
											if (intent.getAction().equals("hu.kfg.wifimanager.MANUAL_LOGIN")){
												Log.d(TAG,"Different wifi with the name \"kfg\"!");
												showSuccessToast.postAtFrontOfQueue(new Runnable() {
														@Override
														public void run () {
															Toast.makeText(context,"A wifi is connected with the name \"kfg\", but it is not the real one!\nDo not log in here!",Toast.LENGTH_SHORT).show();
														}
													});
												return;
											}
											Log.d(TAG,"Received more than once...");
									
								}
	         				} 
						} else {
							firstConnect = true;
							Log.d(TAG,"Not connected!");
							if (++runTimes==3){
								return;
							}
							if (++numOfTries==1){
								SystemClock.sleep(4000);
								Log.d(TAG,"Retry now...");
								numOfTries = 0;
								onReceive(context,intent);
							}
								
						}
		  
		  }}).start();
	      
	   }
	
public boolean connect(final String password,final String username,final Context context,final Intent intent) {
	final String TAG = "KFGlogin";
	final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	if (Build.VERSION.SDK_INT >= 21 ){
		Log.d(TAG, "Modify default network");
		
		for (Network net : cm.getAllNetworks()) {
			if (cm.getNetworkInfo(net).getType() == ConnectivityManager.TYPE_WIFI) {
				Log.d(TAG, "Def. Network Changed: "+ ConnectivityManager.setProcessDefaultNetwork(net));
			}
		}
	}
	final SharedPreferences pref = PreferenceManager
		.getDefaultSharedPreferences(context);
	
	long time = pref.getLong("time",System.currentTimeMillis()-300000);
	if (!(Math.abs(System.currentTimeMillis()-time)>200000)&&(!intent.getAction().equals("hu.kfg.wifimanager.MANUAL_LOGIN"))){ 
		Log.d(TAG,"Last login was within 200s");
		new TimeoutKiller().connect("Karinthy%20Frigyes%20Gimnázium",context);
		return false;
	}
	if ((!intent.getAction().equals("hu.kfg.wifimanager.MANUAL_LOGIN"))&&is204PageAvailable(3000)){
		Log.d(TAG,"Google 204 checkpage available");
		new TimeoutKiller().connect("Karinthy%20Frigyes%20Gimnázium",context);
		return false;
	}
	if (!isLoginPageAvailable(8000)){
		Log.e(TAG,"Cannot reach captive portal! (DNS)");
		SystemClock.sleep(1000);
		WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = manager.getConnectionInfo();
		if (wifiInfo!=null&&wifiInfo.getSSID().equals("\"kfg\"")){
			notifyIfFailed(2, context);
		}
		return false;
	}
	String kfgserver = "http://wifi-gw.karinthy.hu/";
	final HttpParams httpParams = new BasicHttpParams();
    HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
	HttpConnectionParams.setSoTimeout(httpParams, 5000);
	
    HttpResponse response = null;
    HttpClient client = new DefaultHttpClient(httpParams);
	HttpContext hcon = new BasicHttpContext();
    /*HttpUriRequest request = new HttpGet(kfgserver);
	
    try {
        response = client.execute(request);
    } catch (IOException e3) {
		Log.e(TAG,"Cannot reach captive portal! (REQ)");
        e3.printStackTrace();
		SystemClock.sleep(1000);
		WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = manager.getConnectionInfo();
		if (wifiInfo!=null&&wifiInfo.getSSID().equals("\"kfg\"")){
			notifyIfFailed(2, context);
		}
		return false;
    }
	try {
    response.getEntity();
	} catch (Exception e){
		Log.d(TAG, "Entity error!");
		if (isFirstTry){
			SystemClock.sleep(2500L);
			isFirstTry = false;
			Log.d(TAG,"Retry now...");
			return connect(password,username,context,intent);
			
		} else {
			notifyIfFailed(2,context);
			return false;
		}
		
	}
    //Log.d(TAG, "Login form get: " + response.getStatusLine());
    
*/
    HttpPost post;
    post = new HttpPost(kfgserver);
	List<NameValuePair> params = new ArrayList<NameValuePair>();
    params.add(new BasicNameValuePair("username", username));
    params.add(new BasicNameValuePair("password", password));
    
    try {
        post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
		
	} catch (UnsupportedEncodingException e2) {
        e2.printStackTrace();
    }
    try {
		response = client.execute(post,hcon);
		
	} catch (Exception e1) {
		
		notifyIfFailed(1,context);
		Log.e(TAG,"Cannot execute the POST request");
		e1.printStackTrace();
		return false;
	}
	
	if (response.getStatusLine().getStatusCode()==200){
		//Received 200, everything is OK
		
		Log.d(TAG, "Login form get: " + response.getStatusLine());
		HttpUriRequest req = (HttpUriRequest) hcon.getAttribute( ExecutionContext.HTTP_REQUEST);
		//Let's check, whether the username/password was correct
		if (req.getURI().toASCIIString().equals("/")){
			notifyIfFailed(3,context);
			return false;
		} else {
			//Saving last login time and notifying other kfg apps
			pref.edit().putLong("time",System.currentTimeMillis()).commit();
			if (intent.getAction().equals("hu.kfg.wifimanager.MANUAL_LOGIN")){
				showSuccessToast.postAtFrontOfQueue(new Runnable() {
						@Override
						public void run () {
							Toast.makeText(context,"Login successful!",Toast.LENGTH_SHORT).show();
						}
					});
			}
			Intent broadcastintent = new Intent("hu.kfg.wifimanager.LOGGED_IN");
			context.sendBroadcast(broadcastintent);
			/*if (Build.VERSION.SDK_INT >= 23) {
				cm.EXTRA_CAPTIVE_PORTAL
			}*/
			NotificationManager notificationManager = 
				(NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
			notificationManager.cancelAll();
			
		}
		
		return true;
	} else if (response.getStatusLine().getStatusCode()%200>=100) {
		//Did NOT receive 2xx response, something unexpected happened
		notifyIfFailed(1,context);
		return false;
	} else {
		//Received 2xx, so the login was probably successful,
		//but we are not sure, the username/password can be incorrect
		Log.d(TAG, "Login form get: " + response.getStatusLine());
		notifyIfFailed(4,context);
		return true;
		
	}
}
public static int randInt(int min, int max) {
    Random rand = new Random();
    int randomNum = rand.nextInt((max - min) + 1) + min;
	return randomNum;
}


public static boolean is204PageAvailable(int timeout) {
	try
	{
		HTTPtester dnsRes = new HTTPtester(null);
		Thread t = new Thread(dnsRes);
		t.start();
		t.join(timeout);
		if (!(dnsRes.get()==null)) {
			return dnsRes.get().getStatusLine().getStatusCode()==204?true:false;
		}
		return false;
	}
	catch(Exception e)
	{ 
		return false;
	}
}

	public static boolean isLoginPageAvailable(int timeout) {
		try
		{
			HTTPtester dnsRes = new HTTPtester("http://wifi-gw.karinthy.hu/");
			Thread t = new Thread(dnsRes);
			t.start();
			t.join(timeout);
			if (!(dnsRes.get()==null)) {
				return true;
			}
			return false;
		}
		catch(Exception e)
		{ 
			return false;
		}
	}
	

public static void notifyIfFailed(int state,Context context){
	Intent intent = new Intent(context, MainActivity.class);
	Intent eintent = new Intent(Intent.ACTION_VIEW);
	eintent.setData(Uri.parse("http://wifi-gw.karinthy.hu/"));
	PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);
	PendingIntent epIntent = PendingIntent.getActivity(context, 0, eintent, 0);
	Notification.Builder n  = new Notification.Builder(context);
        n.setContentTitle("KFGwifi Error")
        .setContentText(state==1?"Login failed!":state==2?"Cannot reach login site!":state==3?"Wrong username or password":"The login was probably successful, but something unexpected happened")
        .setSmallIcon(R.drawable.ic_launcher)
        //.setContentIntent(pIntent)
        .setAutoCancel(true)
      .addAction(android.R.drawable.ic_menu_edit,"Change password", pIntent)
	  .setVibrate(new long[]{0,80,100,80})
	  .setOnlyAlertOnce(true)
      .addAction(android.R.drawable.ic_menu_more, "Login manually", epIntent).build();
	   
	if (Build.VERSION.SDK_INT>=21){
		n.setVisibility(Notification.VISIBILITY_PUBLIC);
	}
	NotificationManager notificationManager = 
		(NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
	if (state!=4||Build.VERSION.SDK_INT<16) {
	notificationManager.notify(0, n.build()); 
	} else {
		Notification notification = new Notification.BigTextStyle(n)
            .bigText("The login was probably successful, but something unexpected happened").build();
		notificationManager.notify(0,notification); 
	}
}

	public String intToIp(int i) {

		return ((i & 0xFF ) + "." +
			((i >> 8 ) & 0xFF) + "." +
			((i >> 16 ) & 0xFF) + "." +
			(( i >> 24) & 0xFF)) ;
	}
	


	private static class HTTPtester implements Runnable {
		HttpResponse response;
		String domain;

		public HTTPtester(String domain) {
			this.response = null;
			if (domain==null){
				this.domain = "http://clients1.google.com/generate_204";
			} else {
				this.domain = domain;
			}
		}

		public void run() {
			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
			HttpConnectionParams.setSoTimeout(httpParams, 5000);
			HttpClient client = new DefaultHttpClient(httpParams);
			HttpUriRequest request = new HttpGet(domain);
			
			try {
				set(client.execute(request));
			} catch (IOException e3) {}
		}
		public synchronized void set(HttpResponse response) {
			this.response = response;
		}
		public synchronized HttpResponse get() {
			return response;
		}
	}}
