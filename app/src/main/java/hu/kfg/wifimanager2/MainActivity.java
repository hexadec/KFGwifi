package hu.kfg.wifimanager2;

import android.os.Bundle;
import android.os.SystemClock;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.content.*;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.Log;
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

		PreferenceManager.setDefaultValues(this, R.xml.prefs, false);
        final Preference un = findPreference("username");
    	final Preference pw = findPreference("password");
    	final Preference al = findPreference("autologin");
    	final Preference to = findPreference("timeout");
		final Preference st = findPreference("stamina");
		final Preference ml = findPreference("manual_login");
		final Preference ab = findPreference("about");
		final Preference ol = findPreference("open_login_page");
		final Preference se = findPreference("security");
		final Preference co = findPreference("connect");
		final Preference xp = findPreference("xposed");
		final PreferenceCategory ma = (PreferenceCategory)findPreference("manual");

		mWifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		registerReceiver(mWifiScanReceiver,
				new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		if (mWifiManager.isWifiEnabled()) {
			mWifiManager.startScan();
		}

    	final SharedPreferences settings = getSharedPreferences("hu.kfg.wifimanager2_preferences", MODE_PRIVATE);
        
        final SharedPreferences.Editor prefEditorr = settings.edit();
			prefEditorr.putString("password","");
			prefEditorr.putBoolean("loggedin",false);
			try {
			KFGreceiver.firstConnect = true;
			KFGreceiver.numOfTries = 0;
			} catch (Exception e){}
			
		
		if (!settings.getBoolean("startupdialog",false)){
			AlertDialog.Builder adb =new AlertDialog.Builder(this);
			adb.setTitle(R.string.attention);
			adb.setMessage(R.string.initial_info);
			adb.setPositiveButton("OK",null);
			adb.setCancelable(false);
			adb.show();
			prefEditorr.putBoolean("startupdialog",true);
			
		}	
		prefEditorr.commit();
		
    	final EditTextPreference username = (EditTextPreference) un;
    	final EditTextPreference password = (EditTextPreference) pw;
    	final CheckBoxPreference autologin = (CheckBoxPreference) al;
		if (settings.getString("b64","").equals("")){
			password.getEditText().setHint(getString(R.string.no_pwd_yet));
		} else {
		password.getEditText().setHint(getString(R.string.pwd_not_modified));
		}
		
		password.getEditText().setTypeface(android.graphics.Typeface.MONOSPACE);
		if (!autologin.isChecked()){
			un.setEnabled(false);
			pw.setEnabled(false);
			st.setEnabled(false);
			to.setEnabled(false);
			ml.setEnabled(false);
			se.setEnabled(false);
		}
		if (!settings.getBoolean("has_xposed",false)) {
			ma.removePreference(xp);
		}
		
		autologin.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
			public boolean onPreferenceChange(Preference p1, Object o){
				boolean enable = !autologin.isChecked();
					un.setEnabled(enable);
					pw.setEnabled(enable);
					st.setEnabled(enable);
					to.setEnabled(enable);
					ml.setEnabled(enable);
					se.setEnabled(enable);
				return true;
			}
		});

		username.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
				public boolean onPreferenceChange(Preference p1, Object o){
					if (o.toString().equals("")){
						Toast.makeText(getApplicationContext(),R.string.enter_username,Toast.LENGTH_SHORT).show();
						return false;
					}
					String pwd = settings.getString("b64","");
					if (!pwd.equals("")&&!pwd.equals(null)){
						final SharedPreferences.Editor prefEditor = settings.edit();
						
					if (username.getText()!=null&&!username.getText().equals("")){
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
						Toast.makeText(getApplicationContext(),R.string.enter_username_first,Toast.LENGTH_SHORT).show();
						return false;
					}
					final SharedPreferences.Editor prefEditor = settings.edit();
					try{
					
					String base64 = new EncryptUtils().base64encode(new EncryptUtils().cryptThreedog(o.toString(),false,username.getText()));
					prefEditor.putString("b64",base64);
					prefEditor.commit();
					Toast.makeText(MainActivity.this,R.string.password_changed,Toast.LENGTH_SHORT).show();
						password.getEditText().setHint(getString(R.string.pwd_not_modified));
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
					WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
					if (settings.getString("b64","").equals("")||settings.getString("username","").equals("")){
						Toast.makeText(getApplicationContext(),R.string.enter_username_password2,Toast.LENGTH_SHORT).show();
						return true;
					}
					if (manager.isWifiEnabled()) {
						WifiInfo wifiInfo = manager.getConnectionInfo();
						if (wifiInfo!=null&&wifiInfo.getSSID()!=null&&wifiInfo.getSSID().equals("\"kfg\"")){
							Toast.makeText(getApplicationContext(),R.string.attempt_to_login,Toast.LENGTH_SHORT).show();
							Intent intent = new Intent("hu.kfg.wifimanager.MANUAL_LOGIN");
							sendBroadcast(intent);
						} else {
							Toast.makeText(getApplicationContext(),R.string.kfg_isnot_connected,Toast.LENGTH_SHORT).show();
						}
					} else {			
						Toast.makeText(getApplicationContext(),R.string.wifi_not_enabled,Toast.LENGTH_SHORT).show();
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
		co.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						if (connectTokfg(MainActivity.this)) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(MainActivity.this,R.string.connected,Toast.LENGTH_SHORT).show();
								}
							});
						} else {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(MainActivity.this,R.string.couldnot_connect,Toast.LENGTH_SHORT).show();
								}
							});
						}
					}
				}).start();

				return true;
			}
		});
			
    }

	@Override
	public void onDestroy() {
		unregisterReceiver(mWifiScanReceiver);
		super.onDestroy();
	}

	//WIP
	//Todo do in background
	static boolean connectTokfg(Context context) {
		WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(WIFI_SERVICE);
		int netId = -1;
		wifiManager.setWifiEnabled(true);
		SystemClock.sleep(1000);
		for(int i = 0;i < 10; i++) {
			SystemClock.sleep(500);
			if (wifiManager.isWifiEnabled()) break;
		}
		if (!wifiManager.isWifiEnabled()) return false;
		mWifiManager.startScan();
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

	private static final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
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
			Log.d("Available networks:",res.toString());
			if (res.SSID.equals("kfg")) return inRange = true;
		}
		return inRange = false;
	}
	
	
}
