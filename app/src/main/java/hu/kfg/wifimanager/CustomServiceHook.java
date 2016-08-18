package hu.kfg.wifimanager;

import de.robv.android.xposed.IXposedHookZygoteInit;
import com.android.server.XAService;
import de.robv.android.xposed.*;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import android.os.*;

public class CustomServiceHook implements IXposedHookLoadPackage {
   public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		
				
	/*public final class CustomServiceHook implements IXposedHookZygoteInit {
		@Override
		public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
			*/if (!lpparam.packageName.equals("android")){return;}
			//startupParam.lpparam
			//lpparam.classLoader.loadClass("com.android.server.am.ActivityManagerService");
			//XposedBridge.log("wait for inject");
			//SystemClock.sleep(5000);
	   		if (Build.VERSION.SDK_INT<17) {XposedBridge.log("SDK version lower than 17, quitting..."); return;}
			XposedBridge.log("injecting now");
			XAService.inject(lpparam.classLoader.loadClass("com.android.server.am.ActivityManagerService"));
		}
       
}
