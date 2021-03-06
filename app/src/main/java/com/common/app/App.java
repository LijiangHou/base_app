package com.common.app;

import java.util.Iterator;
import java.util.List;

import com.common.app.base.manager.DeployManager;
import com.common.app.base.manager.Manager;
import com.common.app.base.utils.EventUtils;
import com.common.app.event.ExitAppEvent;
import com.common.utils.AppLog;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.text.TextUtils;

/**
 * Created by houlijiang on 16/1/19.
 * 
 * 进行各种初始化
 */
public class App extends Application {

    private static final String TAG = "App";
    // 程序版本
    private final String PREF_VERSION_TYPE = "version_type";

    private static Application mInstance;

    private SharedPreferences mSharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance=this;
        mSharedPreferences = getSharedPreferences("app", Context.MODE_PRIVATE);
        // 初始化TX基础模块
        Manager.initForProcess(this, getVersionType());
        // 只能一个进程初始化的使用下面的方法
        int pid = android.os.Process.myPid();
        String processAppName = getAppName(pid);
        // 如果启动多个进程，非主进程这里会返回空
        if (TextUtils.isEmpty(processAppName)) {
            AppLog.d(TAG, "processAppName is null, in service process");
        } else {
            AppLog.d(TAG, "processAppName:" + processAppName + " in main process");
            // 初始化deploy manager等各种manager
            Manager.initForMain(this);

            AppMain.getInstance(this).appStart();
        }

    }

    @Override
    public void onTerminate() {
        AppMain.getInstance(this).appStop();
        super.onTerminate();
    }

    public static Application getInstance(){
        return mInstance;
    }

    private String getAppName(int pID) {
        String processName;
        ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List l = am.getRunningAppProcesses();
        Iterator i = l.iterator();
        PackageManager pm = this.getPackageManager();
        while (i.hasNext()) {
            ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo) (i.next());
            try {
                if (info.pid == pID) {
                    CharSequence c =
                        pm.getApplicationLabel(pm.getApplicationInfo(info.processName, PackageManager.GET_META_DATA));
                    processName = info.processName;
                    return processName;
                }
            } catch (Exception e) {
                AppLog.e(TAG, "e:" + e.getLocalizedMessage());
            }
        }
        return null;
    }

    private DeployManager.EnvironmentType getVersionType() {
        int version = DeployManager.EnvironmentType.TYPE_ONLINE.getValue();
        // 这个在gradle里配置的，有限使用那个配置
        if (!BuildConfig.IS_ONLINE) {
            version = mSharedPreferences.getInt(PREF_VERSION_TYPE, DeployManager.EnvironmentType.TYPE_TEST.getValue());
        }
        return DeployManager.EnvironmentType.valueOf(version);
    }

    @SuppressLint("CommitPrefEdits")
    public void setVersionType(DeployManager.EnvironmentType type) {
        mSharedPreferences.edit().putInt(PREF_VERSION_TYPE, type.getValue()).commit();
        // 退出程序
        EventUtils.postEvent(new ExitAppEvent());
    }

}
