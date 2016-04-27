package com.android.gary.common.synchttp;

import android.content.Context;

public class ConnectException extends Exception {


    private static final long serialVersionUID = 1L;

    public static final int ERROR_TYPE_CONNECT = 0;
    public static final int ERROR_SELF_DEF = 1;
    public static final int ERROR_TYPE_WATCH = 2;

    public static final int ERROR_CODE_SUCCESS = 200;
    public static final int ERROR_CODE_NO_NET = 1001;
    public static final int ERROR_CODE_DNS = 1002;
    public static final int ERROR_CODE_SOCKET = 1003;
    public static final int ERROR_CODE_SOCKETTOMEOUT = 1004;
    public static final int ERROR_CODE_IO = 1005;
    public static final int ERROR_CODE_UNKONW = 1006;
    public static final int ERROR_CODE_JSON_ERROR = 1007;

    public static final int ERROR_CODE_404 = 404;
    public static final int ERROR_CODE_500 = 500;

    public static final int ERROR_WATCH_SYNC_ERROR = 2001;

    private int errorType;

    private int errorCode;

    private String reason;

    private ResponseCallback responseCallback;

    /**
     * Constructor.
     */
    public ConnectException(int errorType, int errorCode, String reason) {
        super(reason);
        this.errorType = errorType;
        this.errorCode = errorCode;
        this.reason = reason;
        // TODO Auto-generated constructor stub
    }

    public ConnectException(int errorType, int errorCode) {
        super();
        this.errorType = errorType;
        this.errorCode = errorCode;
        // TODO Auto-generated constructor stub
    }

    public String getErrorMessage(Context context) {
        if (errorType == ERROR_SELF_DEF) {
            return reason;
        }

        return "请求失败";
    }

    public int getErrorCode() {
        return errorCode;
    }

    public ResponseCallback getResponseCallback() {
        return responseCallback;
    }

    public void setResponseCallback(ResponseCallback responseCallback) {
        this.responseCallback = responseCallback;
    }

}
