package com.bzsx.password;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 内存单例，持有当前会话的主密码。
 * 替代 Intent 明文传递，所有页面通过 SessionManager.getInstance().getMasterPassword() 获取。
 */
public class SessionManager {

    private static final String PREF_NAME = "master_password_pref";
    private static final String KEY_CACHED_PASSWORD = "cached_master_password";

    private static SessionManager instance;
    private String masterPassword;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * 设置主密码（登录成功后调用一次）
     */
    public void setMasterPassword(String masterPassword, Context context) {
        this.masterPassword = masterPassword;
        // 仍缓存到 SP 供 AutofillService 使用
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CACHED_PASSWORD, masterPassword)
                .apply();
    }

public String getMasterPassword(Context context) {
    if (masterPassword == null) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        masterPassword = prefs.getString(KEY_CACHED_PASSWORD, null);
    }
    return masterPassword;
}

}