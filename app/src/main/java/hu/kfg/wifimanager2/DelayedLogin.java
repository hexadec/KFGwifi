package hu.kfg.wifimanager2;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import hu.hexadecimal.textsecure.EncryptUtils;

public class DelayedLogin extends BroadcastReceiver {

    final String TAG = "KFGDelayedLogin";

    @Override
    public void onReceive(final Context context,final Intent intent) {
        //Cancel all other alarms
        AlarmManager alarmManager1 = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent1 = new Intent(context, DelayedLogin.class);
        PendingIntent pendingIntent1 = PendingIntent.getBroadcast(context, 10, intent1, 0);
        alarmManager1.cancel(pendingIntent1);


        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        SharedPreferences pref = PreferenceManager
                .getDefaultSharedPreferences(context);
        final boolean autologin = pref.getBoolean("autologin", false);
        final boolean security = pref.getBoolean("security", true);
        if (!(autologin&&ssid.equals("\"kfg\"")&&(!security||KFGreceiver.isRealKfgWifi(wifiManager.getDhcpInfo())))) {
            return;
        }
        Log.d(TAG,"DelayedLogin started...");
        if (security) {
            String[] macAddress = context.getResources().getStringArray(R.array.macAddresses);
            boolean match = false;
            for (String macAddress1 : macAddress) {
                if (macAddress1.equals(wifiInfo.getBSSID())||macAddress1.toLowerCase().equals(wifiInfo.getBSSID())) match = true;
            }
            if (Build.VERSION.SDK_INT>=21) {
                if (!(wifiInfo.getFrequency()<2600)) {
                    if (intent.getAction().equals("hu.kfg.wifimanager.MANUAL_LOGIN")) {
                        Log.d(TAG, "Different wifi with the name \"kfg\"! -- FREQ");
                        KFGreceiver.showSuccessToast.postAtFrontOfQueue(new Runnable() {
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
                KFGreceiver.showSuccessToast.postAtFrontOfQueue(new Runnable() {
                    @Override
                    public void run () {
                        Toast.makeText(context,context.getString(R.string.not_real_kfg),Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }
        }
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
            KFGreceiver.notifyIfFailed(1,context,11);
        }
        int i = KFGreceiver.randInt(90, 180);
            try {
                Thread.sleep(67+i);
            } catch (InterruptedException e) {
                Log.d(TAG,"Thread sleep failed");
            }
        int connect = connect(txt,username,context,intent);
        switch (connect) {
            case 1:
                break;
            case 0:
                if (timeout) {
                    Log.d(TAG,"Starting TimeoutKiller...");
                    AlarmManager alarmManager=(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    Intent intente = new Intent(context, TimeoutKiller.class);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intente, PendingIntent.FLAG_UPDATE_CURRENT);
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
                if (intent.getIntExtra("runtimes", 1)<5) {
                    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    Intent intente = new Intent(context, DelayedLogin.class);
                    intente.putExtra("runtimes", intent.getIntExtra("runtimes", 0)+1);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 10, intente, PendingIntent.FLAG_CANCEL_CURRENT);
                    if (Build.VERSION.SDK_INT >= 19) {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + ((3+intent.getIntExtra("runtimes", 1)>2?3:0) * 1000), pendingIntent);
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + ((3+intent.getIntExtra("runtimes", 1)>2?3:0) * 1000), pendingIntent);
                    }
                } else {
                    KFGreceiver.notifyIfFailed(connect>-20?1:2,context,connect);
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


    }

    public static int connect(final String password,final String username,final Context context,final Intent intent) {
        final String TAG = "KFGlogin";
        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= 21 ){
            Log.d(TAG, "Modify default network");

            for (Network net : cm.getAllNetworks()) {
                if (cm.getNetworkInfo(net).getType() == ConnectivityManager.TYPE_WIFI) {
                    if (Build.VERSION.SDK_INT>=23) Log.d(TAG, "Def. Network Changed -- API23: "+ cm.bindProcessToNetwork(net));
                    Log.d(TAG, "Def. Network Changed: "+ ConnectivityManager.setProcessDefaultNetwork(net));
                }
            }
        }
        final SharedPreferences pref = PreferenceManager
                .getDefaultSharedPreferences(context);

        long time = pref.getLong("time",System.currentTimeMillis()-600000);
        if ((System.currentTimeMillis()-time<200000)&&(!intent.getAction().equals("hu.kfg.wifimanager.MANUAL_LOGIN"))){
            //No need to log in again
            Log.d(TAG,"Last login was within 200s");
            new TimeoutKiller().connect("Karinthy%20Frigyes%20Gimnázium",context);
            return 1;
        }
        if ((!intent.getAction().equals("hu.kfg.wifimanager.MANUAL_LOGIN"))&&KFGreceiver.is204PageAvailable(3000)){
            //No need to log in again
            Log.d(TAG,"Google 204 checkpage available");
            new TimeoutKiller().connect("Karinthy%20Frigyes%20Gimnázium",context);
            return 1;
        }
        if (!KFGreceiver.isLoginPageAvailable(8000)){
            Log.e(TAG,"Cannot reach captive portal! (DNS)");
            return -20;
        }
        String kfgserver = "http://wifi-gw.karinthy.hu/";
        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 8000);
        HttpConnectionParams.setSoTimeout(httpParams, 8000);

        HttpResponse response;
        HttpClient client = new DefaultHttpClient(httpParams);
        HttpContext hcon = new BasicHttpContext();
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
            Log.e(TAG,"Cannot execute the POST request");
            e1.printStackTrace();
            return -21;
        }

        if (response.getStatusLine().getStatusCode()==200){
            //Received 200, everything is OK

            Log.d(TAG, "Login form get: " + response.getStatusLine());
            HttpUriRequest req = (HttpUriRequest) hcon.getAttribute( ExecutionContext.HTTP_REQUEST);
            //Let's check, whether the username/password was correct
            if (req.getURI().toASCIIString().equals("/")){
                //Wasn't, notifying user
                KFGreceiver.notifyIfFailed(3,context,30);
                return -30;
            } else {
                //Saving last login time and notifying other kfg apps
                pref.edit().putLong("time",System.currentTimeMillis()).commit();
                if (intent.getAction().equals("hu.kfg.wifimanager.MANUAL_LOGIN")){
                    KFGreceiver.showSuccessToast.postAtFrontOfQueue(new Runnable() {
                        @Override
                        public void run () {
                            Toast.makeText(context,"Login successful!",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                Intent broadcastintent = new Intent("hu.kfg.wifimanager.LOGGED_IN");
                context.sendBroadcast(broadcastintent);
                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
                notificationManager.cancelAll();

            }

            return 0;
        } else if (response.getStatusLine().getStatusCode()%200>=100) {
            //Did NOT receive 2xx response, something unexpected happened
            Log.d(TAG, "Login form get: " + response.getStatusLine());
            return -10;
        } else {
            //Received 2xx, so the login was probably successful,
            //but we are not sure, the username/password can be incorrect
            Log.d(TAG, "Login form get: " + response.getStatusLine());
            KFGreceiver.notifyIfFailed(4,context,40);
            return -40;

        }
    }
}
