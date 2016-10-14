package hu.kfg.wifimanager2;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
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
import android.os.SystemClock;
import android.widget.*;
import android.net.*;
import android.app.*;
import org.apache.http.protocol.*;
import android.os.*;
import hu.hexadecimal.textsecure.*;
import org.apache.http.params.*;


public class KFGreceiver extends BroadcastReceiver {
	int runTimes = 0;
	public static boolean firstConnect = true;
	public static boolean firstDHCP = true;
	public static short numberOfSessions = 0;
	public static int numOfTries = 0;

	final static String TAG = "KFGreceiver";
	protected static final Handler showSuccessToast = new Handler() {
		public void handleMessage(Message msg) {
		}
	};

	public KFGreceiver() {}
	
	   @Override
	   public void onReceive(final Context context, final Intent intent) {
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
//				   if (!user){
//					   Log.d(TAG,"WiFi not connected!");
//				   }
					return;
				} else{
				Log.d(TAG,user?"Screen unlock event":"WiFi connected!");
				}
			}
		   final boolean security = pref.getBoolean("security", true);
			new Thread(new Runnable() {

						@Override
						public void run() {
							Log.d(TAG,"Autologin-thread started... " + ++numberOfSessions);
							SystemClock.sleep(500);
							WifiManager mWifi =  ((WifiManager) context.getSystemService(Context.WIFI_SERVICE));
							if (mWifi.getConnectionInfo()!=null) {
								
		   						WifiInfo wifiInfo = mWifi.getConnectionInfo();
	           						String ssid = wifiInfo.getSSID();
								if (!ssid.equals("\"kfg\"")) {return;}
									
									DhcpInfo dhcp = mWifi.getDhcpInfo();
									int iii = 0;
									if (security) {
									try {
										
										iii = dhcp.ipAddress;
										if (dhcp==null) throw new Exception();
									} catch (Exception e) {
										Log.e(TAG,"DHCP is null!");
										if (!firstDHCP) {
											notifyIfFailed(1,context,-14);
										} else {
											SystemClock.sleep(5000);
											firstDHCP = false;
											onReceive(context,intent);
										}
										return;
									}
									}
								
									if (((!security||isRealKfgWifi(dhcp)))&&(firstConnect||intent.getAction().equals("hu.kfg.wifimanager.MANUAL_LOGIN")||intent.getAction().equals(Intent.ACTION_USER_PRESENT))) {
										firstConnect = false;
										if (security) {
										String[] macAddress = context.getResources().getStringArray(R.array.macAddresses);
										boolean match = false;
										for (String macAddress1 : macAddress) {
											if (macAddress1.equals(wifiInfo.getBSSID())||macAddress1.toLowerCase().equals(wifiInfo.getBSSID())) match = true;
										}
										if (Build.VERSION.SDK_INT>=21) {
											if (!(wifiInfo.getFrequency()<2600)) {
												firstConnect = true;
												numberOfSessions = 0;
												numOfTries = 0;
												if (intent.getAction().equals("hu.kfg.wifimanager.MANUAL_LOGIN")) {
													Log.d(TAG, "Different wifi with the name \"kfg\"! -- FREQ");
													showSuccessToast.postAtFrontOfQueue(new Runnable() {
														@Override
														public void run() {
															Toast.makeText(context, context.getString(R.string.not_real_kfg), Toast.LENGTH_SHORT).show();
														}
													});
												}
												return;
											}
										}
										if (!match){ 
											Log.d(TAG,"Different wifi with the name \"kfg\"! -- MAC -- "+wifiInfo.getBSSID());
											showSuccessToast.postAtFrontOfQueue(new Runnable() {
													@Override
													public void run () {
														Toast.makeText(context,context.getString(R.string.not_real_kfg),Toast.LENGTH_SHORT).show();
													}
												});
											return;
										}
										}
										Log.d(TAG,"kfg connected, login in progress...");
										final String username = pref.getString("username", "");
										final String password2 = pref.getString("b64", "");
										if (username.equals("")||password2.equals("")){
											Toast.makeText(context,context.getString(R.string.enter_username_password),Toast.LENGTH_SHORT).show();
											return;
										}
										final boolean timeout = pref.getBoolean("timeout", false);
										String txt = "";
										try{
											txt = new EncryptUtils().cryptThreedog(new EncryptUtils().base64decode(password2),true,username);
										} catch (Exception e){
											Log.e(TAG,"Decryption failed");
											notifyIfFailed(1,context,-15);
										}
	            						int i = randInt(90, 180);
										if (statech) {
										try {
											Thread.sleep(49+i);
										} catch (InterruptedException e) {
											Log.d(TAG,"Thread sleep failed");
										}
										}
										int connect = DelayedLogin.connect(txt,username,context,intent);
										switch (connect) {
											case 1:
												break;
											case 0:
												if (timeout) {
													Log.d(TAG,"Starting TimeoutKiller...");
													AlarmManager alarmManager=(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
													Intent intente = new Intent(context, TimeoutKiller.class);
													PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intente, 0);
													if (Build.VERSION.SDK_INT >= 19) {
														alarmManager.setExact(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+(i*1000),pendingIntent);
													} else {
														alarmManager.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+(i*1000),pendingIntent);
													}
												}
												break;
											case -10:
											case -11:
											case -12:
											case -20:
											case -21:
											case -22:
												Log.d(TAG,"Setting alarm for DelayedLogin");
													AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
													Intent intente = new Intent(context, DelayedLogin.class);
													intente.putExtra("forwarded_action",intent.getAction());
													PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 10, intente, PendingIntent.FLAG_UPDATE_CURRENT);
													if (Build.VERSION.SDK_INT >= 19) {
														alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (3 * 1000), pendingIntent);
													} else {
														alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (3 * 1000), pendingIntent);
													}

												break;
											case -30:
											case -31:
												break;
											case -40:
											case -41:
												break;
											default:
												break;

										}
	            	
	            					} else {
											firstConnect = true;
											numberOfSessions = 0;
											numOfTries = 0;
											if (intent.getAction().equals("hu.kfg.wifimanager.MANUAL_LOGIN")) {
												Log.d(TAG, "Different wifi with the name \"kfg\"!");
												showSuccessToast.postAtFrontOfQueue(new Runnable() {
													@Override
													public void run() {
														Toast.makeText(context, context.getString(R.string.not_real_kfg), Toast.LENGTH_SHORT).show();
													}
												});
												return;
											}
											Log.d(TAG,"Received more than once...");
	         				} 
						} else {
							firstConnect = true;
							Log.d(TAG,"Not connected!");
							if (++runTimes==3){
								return;
							}
							if (++numOfTries<=3){
								SystemClock.sleep(5000);
								Log.d(TAG,"Retry now...");
								numOfTries = 0;
								onReceive(context,intent);
							}
								
						}

		  
		  }}).start();
	      
	   }
	

	public static boolean isRealKfgWifi(DhcpInfo dhcp) {
		if (((intToIp(dhcp.gateway).startsWith("172."))&&(intToIp(dhcp.netmask).equals("255.255.0.0")))) {
			return true;
		} else {
			return false;
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
		return !(dnsRes.get()==null) && dnsRes.get().getStatusLine().getStatusCode()==204;
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
	

public static void notifyIfFailed(int state,Context context,int errorCode){
	Intent intent = new Intent(context, MainActivity.class);
	Intent eintent = new Intent(Intent.ACTION_VIEW);
	eintent.setData(Uri.parse("http://wifi-gw.karinthy.hu/"));
	PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);
	PendingIntent epIntent = PendingIntent.getActivity(context, 0, eintent, 0);
	Notification.Builder n  = new Notification.Builder(context);
        n.setContentTitle(context.getString(R.string.kfgwifi_error))
        .setContentText((state==1?context.getString(R.string.login_failed):state==2?context.getString(R.string.cannot_reach_login):state==3?context.getString(R.string.wrong_username_password):context.getString(R.string.login_prob_successful))+" ("+errorCode+")")
        .setSmallIcon(R.drawable.ic_launcher)
        //.setContentIntent(pIntent)
        .setAutoCancel(true)
	  .setVibrate(new long[]{0,50,110,50})
	  .setOnlyAlertOnce(true);

	   
	if (Build.VERSION.SDK_INT>=21){
		n.setVisibility(Notification.VISIBILITY_PUBLIC);
	}
	NotificationManager notificationManager = 
		(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	if (Build.VERSION.SDK_INT<16) {
		notificationManager.notify(0, n.getNotification());
	} else if (Build.VERSION.SDK_INT>=16&&state!=4) {
		n.addAction(android.R.drawable.ic_menu_edit, context.getString(R.string.change_password), pIntent);
		n.addAction(android.R.drawable.ic_menu_more, context.getString(R.string.login_manually), epIntent);
		notificationManager.notify(0, n.build());
	} else if (state==4&&Build.VERSION.SDK_INT>=16){
		Notification notification = new Notification.BigTextStyle(n)
            .bigText(context.getString(R.string.login_prob_successful)).build();
		notificationManager.notify(0,notification); 
	}
}

	public static String intToIp(int i) {

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
			HttpConnectionParams.setConnectionTimeout(httpParams, 8000);
			HttpConnectionParams.setSoTimeout(httpParams, 8000);
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
