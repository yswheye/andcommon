package com.android.gary.common.asynchttp;

import android.text.TextUtils;

import java.util.Iterator;
import java.util.Map;

public class HttpEngine {
    /**
     * url and para separator
     **/
    public static final String URL_AND_PARA_SEPARATOR = "?";
    /**
     * parameters separator
     **/
    public static final String PARAMETERS_SEPARATOR = "&";
    /**
     * paths separator
     **/
    public static final String PATHS_SEPARATOR = "/";
    /**
     * equal sign
     **/
    public static final String EQUAL_SIGN = "=";

    private static AsyncHttpClient mHttpClient = new AsyncHttpClient();

    static {
        mHttpClient.setTimeout(30 * 1000);
    }

    public static void get(String url, ResponseHandlerInterface handler) {
        mHttpClient.get(url, handler);
    }

	/*public static void post(String url, RequestParams params,
            ResponseHandlerInterface handler) {
		LogUtil.d("url:" + url + " params:" + params);
		mHttpClient.post(url, params, handler);
	}*/

    /**
     * join url and paras
     * <p/>
     * <pre>
     * getUrlWithParas(null, {(a, b)})                            =   "?a=b";
     * getUrlWithParas("www.baidu.com", {})                       =   "baidu.com";
     * getUrlWithParas("www.baidu.com", {(a, b), (i, j)})         =   "baidu.com?a=b&i=j";
     * getUrlWithParas("www.baidu.com", {(a, b), (i, j), (c, d)}) =   "baidu.com?a=b&i=j&c=d";
     * </pre>
     *
     * @param url      url
     * @param parasMap paras map, key is para name, value is para value
     * @return if url is null, process it as empty string
     */
    public static String getUrlWithParas(String url,
                                         Map<String, String> parasMap) {
        StringBuilder urlWithParas = new StringBuilder(
                TextUtils.isEmpty(url) ? "" : url);
        String paras = joinParas(parasMap);
        if (!TextUtils.isEmpty(paras)) {
            urlWithParas.append(URL_AND_PARA_SEPARATOR).append(paras);
        }
        return urlWithParas.toString();
    }

    /**
     * join paras
     *
     * @param parasMap paras map, key is para name, value is para value
     * @return join key and value with {@link #EQUAL_SIGN}, join keys with
     * {@link #PARAMETERS_SEPARATOR}
     */
    public static String joinParas(Map<String, String> parasMap) {
        if (parasMap == null || parasMap.size() == 0) {
            return null;
        }

        StringBuilder paras = new StringBuilder();
        Iterator<Map.Entry<String, String>> ite = parasMap.entrySet()
                .iterator();
        while (ite.hasNext()) {
            Map.Entry<String, String> entry = (Map.Entry<String, String>) ite
                    .next();
            paras.append(entry.getKey()).append(EQUAL_SIGN)
                    .append(entry.getValue());
            if (ite.hasNext()) {
                paras.append(PARAMETERS_SEPARATOR);
            }
        }
        return paras.toString();
    }

}
