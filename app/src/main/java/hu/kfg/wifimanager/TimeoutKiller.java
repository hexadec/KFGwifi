package hu.kfg.wifimanager;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
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
import org.apache.http.params.*;
import android.os.*;
import android.net.*;
import android.app.*;


public class TimeoutKiller extends BroadcastReceiver
{   

 @Override
 public void onReceive(final Context context, Intent intent)
  {
	 WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
     WifiInfo wifiInfo = wifiManager.getConnectionInfo();
     String ssid = wifiInfo.getSSID();
     SharedPreferences prefe = PreferenceManager
				.getDefaultSharedPreferences(context);
		final boolean timeout = prefe.getBoolean("timeout", false);
    if (!(timeout&&ssid.equals("\"kfg\""))) {
    	Log.d("TimeoutKiller", "Killer stopped");
    } else {
    	Log.d("TimeoutKiller","Killing timeout and resheduling alarm...");
		AlarmManager alarmManager=(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intente = new Intent(context, TimeoutKiller.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intente, 0);
		if (Build.VERSION.SDK_INT >= 19) {
			alarmManager.setExact(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+(KFGreceiver.randInt(90,210)*1000),pendingIntent);
		} else {
			alarmManager.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+(KFGreceiver.randInt(90,210)*1000),pendingIntent);
		}
		new Thread(new Runnable() {

				@Override
				public void run() {
    	connect("Karinthy%20Frigyes%20Gimnázium",context);
					
		}}).start();
    }
  }
 /**
 * Doing some network traffic, in order to avoid timeout (keep the connection alive)
 *
 */
 public void connect(String username,Context context) {
	 if (Build.VERSION.SDK_INT >= 21 ){
		 final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		 for (Network net : cm.getAllNetworks()) {
			 if (cm.getNetworkInfo(net).getType() == ConnectivityManager.TYPE_WIFI) {
				 ConnectivityManager.setProcessDefaultNetwork(net);
			 }
		 }
	 }
	    final String TAG = "TimeoutKiller";
	    HttpResponse response = null;
	    HttpClient client = new DefaultHttpClient();
	    HttpEntity entity;
	    HttpUriRequest request = new HttpGet("http://www.google.com/search?q="+username);
	  try {
	        response = client.execute(request);
	    } catch (Exception e3) {
	        e3.printStackTrace();
	    } 
		entity=null;
		try {
			
	    entity = response.getEntity();
		 Log.d(TAG,response.getStatusLine().toString());
		} catch (Exception e){
			Log.d(TAG,"Response error!");
			e.printStackTrace();
			
		}
		entity = null;
	    String yourServer = "http://www.google.com/";
	 	HttpPost post = new HttpPost(yourServer);

	    List<NameValuePair> params = new ArrayList<NameValuePair>();
	    params.add(new BasicNameValuePair("q", username));
	    
	    try {
	        post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
	    } catch (UnsupportedEncodingException e2) {
	        e2.printStackTrace();
	    }
		
	    try {
	        response = client.execute(post);
	    } catch (Exception e1) {
			Log.d(TAG,"Cannot execute the POST request");
	        e1.printStackTrace();
	    } 
		try {
	    entity = response.getEntity();
		 Log.d(TAG,response.getStatusLine().toString());
		} catch (Exception err){
			Log.d(TAG,"Response error!");
			err.printStackTrace();
		 
		}
		if (KFGreceiver.is204PageAvailable(3000)) {
		 NotificationManager notificationManager = 
			 (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
			 notificationManager.cancelAll();
		}
	}
}
