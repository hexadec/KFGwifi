package hu.kfg.wifimanager2;

import com.android.server.XAService;
import de.robv.android.xposed.*;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.*;
import android.util.Log;

import java.net.InetAddress;


public class CustomServiceHook implements IXposedHookLoadPackage {
   public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
	   if (Build.VERSION.SDK_INT<=19) {
		   if (!lpparam.packageName.equals("android")) {
			   return;
		   }
		   if (Build.VERSION.SDK_INT < 17) {
			   XposedBridge.log("SDK version lower than 17, quitting...");
			   return;
		   }
		   if (Build.VERSION.SDK_INT >= 18) {
			   Log.d("CustomServiceHook", " API 18-19 mode");
			   XposedBridge.log("KFGwifi: API 18-19 mode");
			   XposedHelpers.findAndHookMethod("com.android.server.ConnectivityService", lpparam.classLoader, "captivePortalCheckCompleted", NetworkInfo.class, "boolean", new XC_MethodHook() {
				   @Override
				   protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					   WifiManager manager = (WifiManager) AndroidAppHelper.currentApplication().getSystemService(Context.WIFI_SERVICE);
					   SystemClock.sleep(500);
					   if (((NetworkInfo) param.args[0]).getType() == ConnectivityManager.TYPE_WIFI && manager.isWifiEnabled()) {
						   WifiInfo wifiInfo = manager.getConnectionInfo();
						   if (wifiInfo != null && wifiInfo.getSSID() != null && wifiInfo.getSSID().equals("\"kfg\"")) {
							   XposedBridge.log("isCaptivePortal chkcompleted: " + param.args[1]);
							   param.args[1] = false;
						   }
					   }
				   }
			   });
		   }
		   Log.d("CustomServiceHook", " API 17-19 mode");
		   XposedBridge.log("KFGwifi: API 17-19 mode");
		   XposedHelpers.findAndHookMethod("android.net.CaptivePortalTracker", lpparam.classLoader, "isCaptivePortal", InetAddress.class,new XC_MethodHook() {
			   @Override
			   protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				   WifiManager manager = (WifiManager) AndroidAppHelper.currentApplication().getSystemService(Context.WIFI_SERVICE);
				   SystemClock.sleep(500);
				   if (manager.isWifiEnabled()) {
					   WifiInfo wifiInfo = manager.getConnectionInfo();
					   if (wifiInfo!=null&&wifiInfo.getSSID()!=null&&wifiInfo.getSSID().equals("\"kfg\"")){
						   XposedBridge.log("isCaptivePortal: "+param.getResult());
						   param.setResult(false);
					   }
				   }
			   }
		   });


	   } else if (Build.VERSION.SDK_INT<23&&Build.VERSION.SDK_INT>=21) {
		   Log.d("CustomServiceHook"," API 21-22 mode");
		   if (!lpparam.packageName.equals("android")) {
			   return;
		   }
		   XposedBridge.log("injecting now");
		   XAService.inject(lpparam.classLoader.loadClass("com.android.server.am.ActivityManagerService"));
	   } else {
		   if (!lpparam.packageName.equals("android")) {
			   return;
		   }
		   Log.d("CustomServiceHook"," API 23+ mode");
		   XposedBridge.log("KFGwifi: API 23+ mode");
		   XposedHelpers.findAndHookMethod("com.android.server.connectivity.NetworkMonitor", lpparam.classLoader, "isCaptivePortal", new XC_MethodHook() {
			   @Override
			   protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				   WifiManager manager = (WifiManager) AndroidAppHelper.currentApplication().getSystemService(Context.WIFI_SERVICE);
				   SystemClock.sleep(200);
				   if (manager.isWifiEnabled()) {
					   WifiInfo wifiInfo = manager.getConnectionInfo();
					   if (wifiInfo!=null&&wifiInfo.getSSID()!=null&&wifiInfo.getSSID().equals("\"kfg\"")){
						   XposedBridge.log("isCaptivePortal: "+((int)param.getResult()));
						   param.setResult(204);
					   }
				   }
			   }
		   });
		   XposedHelpers.findAndHookMethod("android.net.NetworkAgent", lpparam.classLoader, "networkStatus","int", new XC_MethodHook() {
			   @Override
			   protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				   WifiManager manager = (WifiManager) AndroidAppHelper.currentApplication().getSystemService(Context.WIFI_SERVICE);
				   SystemClock.sleep(200);
				   if (manager.isWifiEnabled()) {
					   WifiInfo wifiInfo = manager.getConnectionInfo();
					   if (wifiInfo!=null&&wifiInfo.getSSID()!=null&&wifiInfo.getSSID().equals("\"kfg\"")){
						   XposedBridge.log("networkValidated: "+(param.args[0]));
						   param.args[0] = 1;
					   }
				   }
			   }
		   });
		   //Tell NetworkAgentInfo this network is validated and has no captive portal
		   //Probably this and the next one works
		   XposedHelpers.findAndHookMethod("com.android.server.ConnectivityService", lpparam.classLoader, "updateCapabilities","com.android.server.connectivity.NetworkAgentInfo", NetworkCapabilities.class,new XC_MethodHook() {
			   @Override
			   protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				   if (((NetworkInfo)XposedHelpers.getObjectField(param.args[0],"networkInfo")).getType()==ConnectivityManager.TYPE_WIFI) {
					   WifiManager manager = (WifiManager) AndroidAppHelper.currentApplication().getSystemService(Context.WIFI_SERVICE);
					   SystemClock.sleep(200);
					   if (!manager.isWifiEnabled()) {
						   return;
					   }
					   WifiInfo wifiInfo = manager.getConnectionInfo();
					   if ((wifiInfo!=null&&wifiInfo.getSSID()!=null&&wifiInfo.getSSID().equals("\"kfg\""))){
						   XposedBridge.log("portalDetected: "+(XposedHelpers.getBooleanField(param.args[0],"lastCaptivePortalDetected")));
						   XposedBridge.log("validated: "+(XposedHelpers.getBooleanField(param.args[0],"lastCaptivePortalDetected")));
						   XposedHelpers.setBooleanField(param.args[0],"lastCaptivePortalDetected",false);
						   XposedHelpers.setBooleanField(param.args[0],"lastValidated",true);
					   }
				   }
			   }
		   });
		   //Tell NetworkAgentInfo this network has no restrictions
		   XposedHelpers.findAndHookMethod("android.net.NetworkCapabilities", lpparam.classLoader,"hasCapability", "int",new XC_MethodHook() {
			   @Override
			   protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				   WifiManager manager = (WifiManager) AndroidAppHelper.currentApplication().getSystemService(Context.WIFI_SERVICE);
				   SystemClock.sleep(200);
				   if (manager.isWifiEnabled()) {
					   WifiInfo wifiInfo = manager.getConnectionInfo();
					   if (wifiInfo!=null&&wifiInfo.getSSID()!=null&&wifiInfo.getSSID().equals("\"kfg\"")){
						   if ((int)param.args[0]==13){
							   XposedBridge.log("hasCapability: "+(boolean)param.getResult());
							   param.setResult(true);
						   }
					   }
				   }
			   }
		   });
	   }
		}
       
}
