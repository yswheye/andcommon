package com.android.gary.common.synchttp;

import org.json.JSONException;
import org.json.JSONObject;

public class ResponseCallback {

    public String responseBody;
    public Integer stateCode;
    public String lastModified;
    public String eTeg;

    public String getJsonStr(JSONObject jsonResponseBody) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("stateCode", stateCode);
        obj.put("lastModified", lastModified);
        obj.put("eTeg", eTeg);
        obj.put("responseBody", jsonResponseBody);
        return obj.toString();
    }

    public String getJsonStr() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("stateCode", stateCode);
        obj.put("lastModified", lastModified);
        obj.put("eTeg", eTeg);
        obj.put("responseBody", responseBody);
        return obj.toString();
    }


}
