package com.kong.greentea.application.activity;


import android.app.Activity;

import java.util.Stack;

public class ActivityManager {
    private static Stack<Activity> activityStack;
    private static ActivityManager instance;
    private ActivityManager() {
    }
    public static ActivityManager getActivityManager() {
        if (instance == null) {
            instance = new ActivityManager();
        }
        return instance;
    }

    private void popActivity(Activity activity) {
        if (activity != null) {
            activity.finish();
            activityStack.remove(activity);
            activity = null;
        }
    }

    private Activity currentActivity() {
        Activity activity = null;
        if(!activityStack.empty())
            activity= activityStack.lastElement();
        return activity;
    }

    private void pushActivity(Activity activity) {
        if (activityStack == null) {
            activityStack = new Stack<Activity>();
        }
        activityStack.add(activity);
    }

    /**
     * 退出整个应用时调用。当前activity放到最后finish
     */
    public void exitApp(Activity currentActivity) {
        activityStack.remove(currentActivity);
        while (true) {
            Activity activity = currentActivity();
            if (activity == null) {
                break;
            }
            popActivity(activity);
        }
        currentActivity.finish();
        System.exit(0);
    }

    /**
     * 在activity的oncreate时调用。
     * @param activity
     */
    public void onCreateActivity(Activity activity){
        pushActivity(activity);
    }

    /**
     * 在按物理返回键或者点击界面返回时调用，关掉自己
     * @param activity
     */
    public void closeActivity(Activity activity){
        popActivity(activity);
      //  System.gc();
    }

}
