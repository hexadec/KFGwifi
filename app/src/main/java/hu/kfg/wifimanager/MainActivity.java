package hu.kfg.wifimanager;

import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.content.*;
import android.util.Log;
import android.util.Base64;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.app.*;
import android.widget.*;
import hu.hexadecimal.textsecure.EncryptUtils;
import android.text.Html;
import android.view.Gravity;
import android.net.*;
import android.net.wifi.*;

import java.util.List;


public class MainActivity extends PreferenceActivity {
	
	private String decrypted_password = "";
	static boolean inRange = false;
	private static WifiManager mWifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
        final Preference un = (Preference) findPreference("username");
    	final Preference pw = (Preference) findPreference("password");
    	final Preference al = (Preference) findPreference("autologin");
    	final Preference to = (Preference) findPreference("timeout");
		final Preference st = (Preference) findPreference("stamina");
		final Preference ml = (Preference) findPreference("manual_login");
		final Preference ab = (Preference) findPreference("about");
		final Preference ol = (Preference) findPreference("open_login_page");
		final Preference se = (Preference) findPreference("security");
		final Preference co = (Preference) findPreference("connect");

		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		registerReceiver(mWifiScanReceiver,
				new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		mWifiManager.startScan();
	
    	final SharedPreferences settings = getSharedPreferences("hu.kfg.wifimanager_preferences", MODE_PRIVATE);
        
        final SharedPreferences.Editor prefEditorr = settings.edit();
			prefEditorr.putString("password","");
			prefEditorr.putBoolean("loggedin",false);
			try {
			KFGreceiver.firstConnect = true;
			KFGreceiver.numOfTries = 0;
			} catch (Exception e){}
			
		
		if (!settings.getBoolean("startupdialog",false)){
			AlertDialog.Builder adb =new AlertDialog.Builder(this);
			adb.setTitle("Attention!");
			adb.setMessage(R.string.initial_info);
			adb.setPositiveButton("OK, I understood",null);
			adb.setCancelable(false);
			adb.show();
			prefEditorr.putBoolean("startupdialog",true);
			
		}	
		prefEditorr.commit();
		
    	final EditTextPreference username = (EditTextPreference) un;
    	final EditTextPreference password = (EditTextPreference) pw;
    	final CheckBoxPreference autologin = (CheckBoxPreference) al;
    	final CheckBoxPreference timeout = (CheckBoxPreference) to;
		final CheckBoxPreference stamina = (CheckBoxPreference) st;
		if (settings.getString("b64","").equals("")){
			password.getEditText().setHint("[no password yet]");
		} else {
		password.getEditText().setHint("[not modified]");
		}
		
		password.getEditText().setTypeface(android.graphics.Typeface.MONOSPACE);
		if (!autologin.isChecked()){
			un.setEnabled(false);
			pw.setEnabled(false);
			st.setEnabled(false);
			to.setEnabled(false);
			ml.setEnabled(false);
			se.setEnabled(false);
			//stamina.setChecked(false);
			//timeout.setChecked(false);
		}
		
		autologin.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
			public boolean onPreferenceChange(Preference p1, Object o){
				if (!autologin.isChecked()){
					un.setEnabled(true);
					pw.setEnabled(true);
					st.setEnabled(true);
					to.setEnabled(true);
					ml.setEnabled(true);
					se.setEnabled(true);
				} else {
					un.setEnabled(false);
					pw.setEnabled(false);
					st.setEnabled(false);
					to.setEnabled(false);
					ml.setEnabled(false);
					se.setEnabled(false);
					//stamina.setChecked(false);
					//timeout.setChecked(false);
				}
				return true;
			}
		});
		username.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
				public boolean onPreferenceChange(Preference p1, Object o){
					if (o.toString().equals("")){
						Toast.makeText(getApplicationContext(),"Enter username!",Toast.LENGTH_SHORT).show();
						return false;
					}
					String pwd = settings.getString("b64","");
					if (!pwd.equals("")&&!pwd.equals(null)){
						final SharedPreferences.Editor prefEditor = settings.edit();
						
					if (!username.getText().equals("")&&!username.getText().equals(null)){
						decrypted_password = new EncryptUtils().cryptThreedog(new EncryptUtils().base64decode(pwd),true,username.getText());
						prefEditor.putString("b64",new EncryptUtils().base64encode(new EncryptUtils().cryptThreedog(decrypted_password,false,o.toString())));
						//Log.d("KFG",decrypted_password);
						
					}
						prefEditor.commit();
					
					}
					return true;
				}
			});
		
		password.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
				public boolean onPreferenceChange(Preference p1, Object o){
					if (o.toString().equals("")){
						return false;
					}
					if (settings.getString("username","").equals("")){
						Toast.makeText(getApplicationContext(),"Enter username first!",Toast.LENGTH_SHORT).show();
						return false;
					}
					final SharedPreferences.Editor prefEditor = settings.edit();
					try{
					
					String base64 = new EncryptUtils().base64encode(new EncryptUtils().cryptThreedog(o.toString(),false,username.getText()));
					prefEditor.putString("b64",base64);
					prefEditor.commit();
					Toast.makeText(MainActivity.this,"Password changed!",Toast.LENGTH_SHORT).show();
						password.getEditText().setHint("[not modified]");
					} catch (Exception e){
						
					}
					return false;
				}
				});
				
		ml.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
				public boolean onPreferenceClick(Preference p1){
					long time = settings.getLong("manual_login_time",System.currentTimeMillis()-300000);
					if (!(Math.abs(System.currentTimeMillis()-time)>2000)){
						return true;
					}
					settings.edit().putLong("manual_login_time",System.currentTimeMillis()).commit();
					WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
					if (settings.getString("b64","").equals("")){
						Toast.makeText(getApplicationContext(),"Please enter your username and password",Toast.LENGTH_SHORT).show();
						return true;
					}
					if (manager.isWifiEnabled()) {
						WifiInfo wifiInfo = manager.getConnectionInfo();
						if (wifiInfo!=null&&wifiInfo.getSSID()!=null&&wifiInfo.getSSID().equals("\"kfg\"")){
							Toast.makeText(getApplicationContext(),"Attempt to login...",Toast.LENGTH_SHORT).show();
							Intent intent = new Intent("hu.kfg.wifimanager.MANUAL_LOGIN");
							sendBroadcast(intent);
						} else {
							Toast.makeText(getApplicationContext(),"\"kfg\" is not connected",Toast.LENGTH_SHORT).show();
						}
					} else {			
						Toast.makeText(getApplicationContext(),"WiFi is not enabled",Toast.LENGTH_SHORT).show();
					}
						
					
					return true;
				}
			});
		ab.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
				public boolean onPreferenceClick(Preference p1){
					android.content.pm.PackageInfo pInfo = null;
					try {
					pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
					} catch (Exception e){}
					String version = pInfo.versionName;
					AlertDialog.Builder adb =new AlertDialog.Builder(MainActivity.this);
					adb.setTitle("About");
					adb.setPositiveButton("Ok",null);
					adb.setCancelable(true);
					TextView messageText = new TextView(MainActivity.this);
					messageText.setText(Html.fromHtml("<b>Developer and copyright:</b><br/>Cseh András<br/><b>App version:</b><br/>"+version+"<br/><b>EncryptUtils version:</b><br/>"+EncryptUtils.version));
					messageText.setGravity(Gravity.CENTER_HORIZONTAL);
					messageText.setTextAppearance(MainActivity.this,android.R.style.TextAppearance_Medium);
					adb.setView(messageText);
					adb.show();
					return true;
					
				}
			});
		ol.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
				public boolean onPreferenceClick(Preference p1){
					Intent eintent = new Intent(Intent.ACTION_VIEW);
					eintent.setData(Uri.parse("http://wifi-gw.karinthy.hu/"));
					startActivity(eintent);
					return true;

				}
			});
			
    }

	//WIP
	//Todo do in background
	static boolean connectTokfg(Context context) {
		WifiManager wifiManager = (WifiManager)context.getSystemService(WIFI_SERVICE);              
		int netId = -1;

		for (WifiConfiguration tmp : wifiManager.getConfiguredNetworks()) 
			if (tmp.SSID.equals( "\""+"kfg"+"\"")) 
			{
				netId = tmp.networkId;
				if (inRange||checkScanResults()) {
					return wifiManager.enableNetwork(netId, true);
				} else {
					//TODO kfg not in range
					return false;
				}
				
			}
		//Network not found, todo add
		return false;
		
	}

	private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context c, Intent intent) {
			if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
				checkScanResults();
			}
		}
	};

	private static boolean checkScanResults() {
		List<ScanResult> mScanResults = mWifiManager.getScanResults();
		for (ScanResult res:mScanResults) {
			Log.d("test",res.toString());
			if (res.toString().equals("\"kfg\"")) { return inRange = true;}
		}
		return false;
	}
	
	
}
