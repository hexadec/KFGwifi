package hu.kfg.wifimanager;

import com.android.server.XAService;
import de.robv.android.xposed.*;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import android.os.*;

public class CustomServiceHook implements IXposedHookLoadPackage {
   public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
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
		}
       
}
