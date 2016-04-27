
package com.android.gary.common.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class to parse json data, like opt method in JSONObject.
 *
 * @author zhangdaisong
 */
public final class JsonUtils {

    /**
     * Log tag.
     */
    private static final String TAG = "JsonUtils";

    /**
     * If print log.
     */
    private static final boolean DEBUG = false;

    /**
     * Get string value from JSONObject with specify key. Default value is empty
     * string.
     *
     * @param jsonObject
     * @param key
     * @return
     */
    public static String getString(JSONObject jsonObject, String key) {
        return getString(jsonObject, key, "");
    }

    /**
     * Get string value from JSONObject with specify key. Default value is null.
     *
     * @param jsonObject
     * @param key
     * @return
     */
    public static String getStringNull(JSONObject jsonObject, String key) {
        return getString(jsonObject, key, null);
    }

    /**
     * Get string value from JSONObject with specify key.
     *
     * @param jsonObject
     * @param key
     * @param defaultValue
     * @return
     */
    public static String getString(JSONObject jsonObject, String key, String defaultValue) {
        String value = defaultValue;

        try {
            value = jsonObject.getString(key);

            if (value == null || value.equals("null")) {
                value = defaultValue;

                logD("get string value: " + key + "=null");
            } else {
                logD("get string value: " + key + "='" + value + "'");
            }
        } catch (JSONException e) {
            logD("get string value failed! key is " + key, e);
        }

        return value;
    }

    /**
     * Get int value from JSONObject with specify key. Default value is 0.
     *
     * @param jsonObject
     * @param key
     * @return
     */
    public static int getInt(JSONObject jsonObject, String key) {
        return getInt(jsonObject, key, 0);
    }

    /**
     * Get int value from JSONObject with specify key.
     *
     * @param jsonObject
     * @param key
     * @param defaultValue
     * @return
     */
    public static int getInt(JSONObject jsonObject, String key, int defaultValue) {
        int value = defaultValue;

        try {
            value = jsonObject.getInt(key);

            logD("get int value: " + key + "='" + value + "'");
        } catch (JSONException e) {
            logD("get int value failed! key is " + key, e);
        }

        return value;
    }

    /**
     * Get long value from JSONObject with specify key. Default value is 0.
     *
     * @param jsonObject
     * @param key
     * @return
     */
    public static long getLong(JSONObject jsonObject, String key) {
        return getLong(jsonObject, key, 0);
    }

    /**
     * Get long value from JSONObject with specify key.
     *
     * @param jsonObject
     * @param key
     * @param defaultValue
     * @return
     */
    public static long getLong(JSONObject jsonObject, String key, long defaultValue) {
        long value = defaultValue;

        try {
            value = jsonObject.getLong(key);

            logD("get int value: " + key + "='" + value + "'");
        } catch (JSONException e) {
            logD("get int value failed! key is " + key, e);
        }

        return value;
    }

    /**
     * Get float value from JSONObject with specify key. Default value is 0.
     *
     * @param jsonObject
     * @param key
     * @return
     */
    public static float getFloat(JSONObject jsonObject, String key) {
        return getFloat(jsonObject, key, 0);
    }

    /**
     * Get float value from JSONObject with specify key.
     *
     * @param jsonObject
     * @param key
     * @param defaultValue
     * @return
     */
    public static float getFloat(JSONObject jsonObject, String key, float defaultValue) {
        float value = defaultValue;

        try {
            value = (float) jsonObject.getDouble(key);

            logD("get float value: " + key + "='" + value + "'");
        } catch (JSONException e) {
            logD("get float value failed! key is " + key, e);
        }

        return value;
    }

    /**
     * Get boolean value from JSONObject with specify key. Default value is
     * false.
     *
     * @param jsonObject
     * @param key
     * @return
     */
    public static boolean getBoolean(JSONObject jsonObject, String key) {
        return getBoolean(jsonObject, key, false);
    }

    /**
     * Get boolean value from JSONObject with specify key.
     *
     * @param jsonObject
     * @param key
     * @param defaultValue
     * @return
     */
    public static boolean getBoolean(JSONObject jsonObject, String key, boolean defaultValue) {
        boolean value = defaultValue;

        try {
            value = jsonObject.getBoolean(key);

            logD("get boolean value: " + key + "='" + value + "'");
        } catch (JSONException e) {
            logD("get boolean value failed! key is " + key, e);
        }

        return value;
    }

    /**
     * Get JSONArray value from JSONObject with specify key.
     *
     * @param jsonObject
     * @param key
     * @param defaultValue
     * @return
     */
    public static JSONArray getJSONArray(JSONObject jsonObject, String key, JSONArray defaultValue) {
        JSONArray value = defaultValue;

        try {
            value = jsonObject.getJSONArray(key);
            logD("get getJSONArray value: " + key + "='" + value + "'");
        } catch (JSONException e) {
            logD("get getJSONArray value failed! key is " + key, e);
        }

        return value;
    }

    /**
     * Get JSONObject value from JSONObject with specify key.
     *
     * @param jsonObject
     * @param key
     * @param defaultValue
     * @return
     */
    public static JSONObject getJSONObject(JSONObject jsonObject, String key,
                                           JSONObject defaultValue) {
        JSONObject value = defaultValue;

        try {
            value = jsonObject.getJSONObject(key);
            logD("get JSONObject value: " + key + "='" + value + "'");
        } catch (JSONException e) {
            logD("get JSONObject value failed! key is " + key, e);
        }

        return value;
    }

    /**
     * Get index JSONObject value from JSONArray.
     *
     * @param jsonArray
     * @param index
     * @param defaultValue
     * @return
     */
    public static JSONObject getIndexJSONObject(JSONArray jsonArray, int index,
                                                JSONObject defaultValue) {
        JSONObject value = defaultValue;

        try {
            value = jsonArray.getJSONObject(index);
            logD("get Index JSONObject value: " + index + "='" + value + "'");
        } catch (JSONException e) {
            logD("get Index JSONObject value failed! index is " + index, e);
        }

        return value;
    }

    /**
     * Get index JSONArray value from JSONArray.
     *
     * @param jsonArray
     * @param index
     * @param defaultValue
     * @return
     */
    public static JSONArray getIndexJSONArray(JSONArray jsonArray, int index, JSONArray defaultValue) {
        JSONArray value = defaultValue;

        try {
            value = jsonArray.getJSONArray(index);
            logD("get Index JSONArray value: " + index + "='" + value + "'");
        } catch (JSONException e) {
            logD("get Index JSONArray value failed! index is " + index, e);
        }

        return value;
    }

    /**
     * Get index String value from JSONArray.
     *
     * @param jsonArray
     * @param index
     * @param defaultValue
     * @return
     */
    public static String getIndexString(JSONArray jsonArray, int index, String defaultValue) {
        String value = defaultValue;

        try {
            value = jsonArray.getString(index);
            logD("get Index String value: " + index + "='" + value + "'");
        } catch (JSONException e) {
            logD("get Index String value failed! index is " + index, e);
        }

        return value;
    }

    /**
     * @param msg
     */
    private static void logD(String msg) {
        if (DEBUG) {
            android.util.Log.d(TAG, msg);
        }
    }

    /**
     * @param msg
     * @param e
     */
    private static void logD(String msg, Exception e) {
        if (DEBUG) {
            android.util.Log.d(TAG, msg, e);
        }
    }
}
