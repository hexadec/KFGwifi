package hu.kfg.wifimanager;

import com.android.server.XAService;
import de.robv.android.xposed.*;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.*;
import android.widget.Toast;

import java.util.List;


public class CustomServiceHook implements IXposedHookLoadPackage {
   public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
	   if (Build.VERSION.SDK_INT<23) {
		   if (!lpparam.packageName.equals("android")) {
			   return;
		   }
		   if (Build.VERSION.SDK_INT < 17) {
			   XposedBridge.log("SDK version lower than 17, quitting...");
			   return;
		   }
		   XposedBridge.log("injecting now");
		   XAService.inject(lpparam.classLoader.loadClass("com.android.server.am.ActivityManagerService"));
		   //Some codes to find the way to MM's captive portal system
				/*mConnectivityServiceHandler.sendMessage(obtainMessage(EVENT_NETWORK_TESTED,
                    NETWORK_TEST_RESULT_VALID, 0, mNetworkAgentInfo));

                    nai.asyncChannel.sendMessage(
                                android.net.NetworkAgent.CMD_REPORT_NETWORK_STATUS,
                                (valid ? NetworkAgent.VALID_NETWORK : NetworkAgent.INVALID_NETWORK),
                                0, null);
                                */
	   } else {
		   if (!lpparam.packageName.equals("android")) {
			   return;
		   }
		   XposedBridge.log("KFGwifi: API 23+ mode");
		   XposedHelpers.findAndHookMethod("com.android.server.connectivity.NetworkMonitor", lpparam.classLoader, "isCaptivePortal", new XC_MethodHook() {
			   @Override
			   protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				   WifiManager manager = (WifiManager) AndroidAppHelper.currentApplication().getSystemService(Context.WIFI_SERVICE);
				   SystemClock.sleep(500);
				   if (manager.isWifiEnabled()) {
					   WifiInfo wifiInfo = manager.getConnectionInfo();
					   if (wifiInfo!=null&&wifiInfo.getSSID()!=null&&wifiInfo.getSSID().equals("\"kfg\"")){
						   XposedBridge.log("isCaptivePortal: "+((int)param.getResult()));
						   param.setResult(204);
					   }
				   }
			   }
		   });
		   /*XposedHelpers.findAndHookMethod("com.android.server.connectivity.NetworkAgentInfo", lpparam.classLoader, "satisfies", NetworkRequest.class, new XC_MethodHook() {
			   @Override
			   protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				   WifiManager manager = (WifiManager) AndroidAppHelper.currentApplication().getSystemService(Context.WIFI_SERVICE);
				   SystemClock.sleep(500);
				   XposedBridge.log("satisfies: "+(param.getResult()));
				   if (manager.isWifiEnabled()) {
					   WifiInfo wifiInfo = manager.getConnectionInfo();
					   if (wifiInfo!=null&&wifiInfo.getSSID()!=null&&wifiInfo.getSSID().equals("\"kfg\"")){
						   param.setResult(true);
					   }
				   }
			   }
		   });*/
		   XposedHelpers.findAndHookMethod("android.net.NetworkAgent", lpparam.classLoader, "networkStatus","int", new XC_MethodHook() {
			   @Override
			   protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				   WifiManager manager = (WifiManager) AndroidAppHelper.currentApplication().getSystemService(Context.WIFI_SERVICE);
				   SystemClock.sleep(500);
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
				   WifiManager manager = (WifiManager) AndroidAppHelper.currentApplication().getSystemService(Context.WIFI_SERVICE);
				   SystemClock.sleep(500);
				   if (manager.isWifiEnabled()) {
					   WifiInfo wifiInfo = manager.getConnectionInfo();
					   if (wifiInfo!=null&&wifiInfo.getSSID()!=null&&wifiInfo.getSSID().equals("\"kfg\"")){
						   XposedBridge.log("portalDetected: "+(XposedHelpers.getBooleanField(param.args[0],"lastCaptivePortalDetected")));
						   XposedBridge.log("validated: "+(XposedHelpers.getBooleanField(param.args[0],"lastCaptivePortalDetected")));
						   XposedHelpers.setBooleanField(param.args[0],"lastCaptivePortalDetected",false);
						   XposedHelpers.setBooleanField(param.args[0],"lastValidated",true);
					   }
				   }
			   }
		   });
		   //Tell NetworkAgentInfo this network is has no restrictions
		   XposedHelpers.findAndHookMethod("android.net.NetworkCapabilities", lpparam.classLoader,"hasCapability", "int",new XC_MethodHook() {
			   @Override
			   protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				   WifiManager manager = (WifiManager) AndroidAppHelper.currentApplication().getSystemService(Context.WIFI_SERVICE);
				   SystemClock.sleep(500);
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
