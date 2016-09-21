package com.android.gary.common.utils;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;


/**
 * 点击事件判断工具类
 * 
 * @author yuanhang <729741121@qq.com>
 * 
 */
public class ClickUtils {

    public static long lastClickTime;

    public static long intervalTime = 500L;// 默认间隔时间


    /**
     * 点击过快判断
     * 
     * @return 是否点击过快
     */
    public static boolean isFastClick() {
        return isFastClick(intervalTime);
    }

    /**
     * 点击过快判断
     * 
     * @param intervalTime 间隔时间
     * 
     * @return 是否点击过快
     */
    public static boolean isFastClick(long intervalTime) {
        long time = System.currentTimeMillis();
        long timeD = time - lastClickTime;
        if (0 < timeD && timeD < intervalTime) {// 2次间隔事件小于规定值,认为点击过快
            // LogUtils.i("isFastClick", "isFastClick");
            return true;
        }
        lastClickTime = time;// 记录上次点击时间
        return false;
    }

    /**
     * 双击退出程序
     * 
     * @param activity
     * @param text
     */
    public static void doubleClickExit(final Activity activity, CharSequence text) {
        doubleClick(activity, text, new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.onBackPressed();// 调用ActivityonBackPressed处理逻辑
            }
        });
    }

    /**
     * 双击事件
     * 
     * @param context
     * @param text
     * @param onClickListener
     */
    public static void doubleClick(Context context, CharSequence text, OnClickListener onClickListener) {
        if ((System.currentTimeMillis() - lastClickTime) > 3000) {// 3秒内算一次有效双击
            // ToastUtils.show(context, text);
            lastClickTime = System.currentTimeMillis();
        } else {
            if (onClickListener != null) {
                onClickListener.onClick(null);// 回调
            }
        }
    }


}
