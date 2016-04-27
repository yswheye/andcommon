package com.android.gary.common.synchttp;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.SyncBasicHttpContext;
import org.apache.http.util.EntityUtils;

import android.os.Build;

import com.android.gary.common.utils.LogUtil;
import com.android.gary.common.utils.StringUtils;

public class HttpClientUtil {

    private static final String TAG = "HttpClientUtil";
    private static HttpClientUtil mInstence;

    private static final int DEFAULT_MAX_CONNECTIONS = 10;
    private static final int DEFAULT_SOCKET_TIMEOUT = 10 * 1000;
    private static final int DEFAULT_MAX_RETRIES = 5;
    private static final int RETRY_SLEEP_TIME_MILLIS = 1500;
    private static final int DEFAULT_SOCKET_BUFFER_SIZE = 8192;
    private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    private static final String ENCODING_GZIP = "gzip";

    private static int maxConnections = DEFAULT_MAX_CONNECTIONS;
    private static int socketTimeout = DEFAULT_SOCKET_TIMEOUT;

    private final DefaultHttpClient httpClient;
    private final HttpContext httpContext;
    private final Map<String, String> clientHeaderMap;

    public static HttpClientUtil getInstence() {
        if (mInstence == null) {
            mInstence = new HttpClientUtil();
        }
        return mInstence;
    }

    public HttpClientUtil() {
        BasicHttpParams httpParams = new BasicHttpParams();

        ConnManagerParams.setTimeout(httpParams, socketTimeout);
        ConnManagerParams.setMaxConnectionsPerRoute(httpParams,
                new ConnPerRouteBean(maxConnections));
        ConnManagerParams.setMaxTotalConnections(httpParams, DEFAULT_MAX_CONNECTIONS);

        HttpConnectionParams.setSoTimeout(httpParams, socketTimeout);
        HttpConnectionParams.setConnectionTimeout(httpParams, socketTimeout);
        HttpConnectionParams.setTcpNoDelay(httpParams, true);

        HttpConnectionParams.setSocketBufferSize(httpParams, DEFAULT_SOCKET_BUFFER_SIZE);

        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setUserAgent(httpParams, "GeakWatch2");

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(httpParams, schemeRegistry);

        httpContext = new SyncBasicHttpContext(new BasicHttpContext());
        httpClient = new DefaultHttpClient(cm, httpParams);
        httpClient.addRequestInterceptor(new HttpRequestInterceptor() {
            @Override
            public void process(HttpRequest request, HttpContext context) {
                if (!request.containsHeader(HEADER_ACCEPT_ENCODING)) {
                    request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
                }
                for (String header : clientHeaderMap.keySet()) {
                    request.addHeader(header, clientHeaderMap.get(header));
                }
            }
        });

        httpClient.addResponseInterceptor(new HttpResponseInterceptor() {
            @Override
            public void process(HttpResponse response, HttpContext context) {
                final HttpEntity entity = response.getEntity();
                if (entity == null) {
                    return;
                }
                final Header encoding = entity.getContentEncoding();
                if (encoding != null) {
                    for (HeaderElement element : encoding.getElements()) {
                        if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
                            response.setEntity(new InflatingEntity(response.getEntity()));
                            break;
                        }
                    }
                }
            }
        });

        httpClient.setHttpRequestRetryHandler(new RetryHandler(DEFAULT_MAX_RETRIES,RETRY_SLEEP_TIME_MILLIS));
        clientHeaderMap = new HashMap<String, String>();
    }

    private static class InflatingEntity extends HttpEntityWrapper {
        public InflatingEntity(HttpEntity wrapped) {
            super(wrapped);
        }

        @Override
        public InputStream getContent() throws IOException {
            return new GZIPInputStream(wrappedEntity.getContent());
        }

        @Override
        public long getContentLength() {
            return -1;
        }
    }

    public ResponseCallback post(String url, String params,
                                 ResponseCallback oldResponseCallback) throws ConnectException ,Exception{
        HttpEntityEnclosingRequestBase request = new HttpPost(url);
        LogUtil.d("========the params is:"+params);
        LogUtil.d("========the url is:"+url);
        if (params != null)
            request.setEntity(getEntity(params));
        return sendRequest(httpClient, httpContext, request, null, oldResponseCallback);
    }

    public UrlEncodedFormEntity getEntity(String params) throws Exception{
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("", params));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
        return entity;
    }

    public ResponseCallback post(String url, RequestParams params,
                                 ResponseCallback oldResponseCallback) throws ConnectException {
        LogUtil.d("the url is ====="+url);
        HttpEntityEnclosingRequestBase request = new HttpPost(url);
        if (params != null)
            request.setEntity(paramsToEntity(params));
        return sendRequest(httpClient, httpContext, request, null, oldResponseCallback);
    }

    public synchronized boolean post(String url, RequestParams params) {
        HttpEntityEnclosingRequestBase request = new HttpPost(url);
        if (params != null)
            request.setEntity(paramsToEntity(params));
        return sendRequest(httpClient, httpContext, request, null);
    }

    protected boolean sendRequest(DefaultHttpClient client, HttpContext httpContext,
                                  HttpUriRequest uriRequest, String contentType) {
        if (contentType != null) {
            uriRequest.addHeader("Content-Type", contentType);
        }

        try {
            HttpResponse response = client.execute(uriRequest, httpContext);
            StatusLine status = response.getStatusLine();
            // HttpEntity entity = null;
            HttpEntity temp = response.getEntity();
            if (temp != null) {
                // entity = new BufferedHttpEntity(temp);
                // String responseBody = EntityUtils.toString(entity, "UTF-8");
            }
            if (status.getStatusCode() == 200) {
                LogUtil.d(TAG, "status.getStatusCode() == "+status.getStatusCode());
                return true;
            } else {
                LogUtil.d(TAG, "status.getStatusCode() == "+status.getStatusCode());
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public ResponseCallback get(String url, ResponseCallback oldResponseCallback)
            throws ConnectException {

        HttpRequestBase request = new HttpGet(url);
        return sendRequest(httpClient, httpContext, request, null, oldResponseCallback);
    }

    public ResponseCallback get(String url)
            throws ConnectException {
        HttpRequestBase request = new HttpGet(url);
        LogUtil.i(TAG, "the url === "+url);
        return sendRequest(httpClient, httpContext, request, null, null);
    }
    
    public ResponseCallback get(String url, Map<String, String> params)
            throws ConnectException {
        if (!url.endsWith("?")) {
            url = url + "?";
        }
        String url2 = url + getUrlString(params);
        HttpRequestBase request = new HttpGet(url2);
//        LogUtil.i("_webapp", "the url === "+url2);
        return sendRequest(httpClient, httpContext, request, null, null);
    }

    protected ResponseCallback sendRequest(DefaultHttpClient client, HttpContext httpContext,
                                           HttpUriRequest uriRequest, String contentType, ResponseCallback oldResponseCallback)
            throws ConnectException {
        if (contentType != null) {
            uriRequest.addHeader("Content-Type", contentType);
        }
        if (oldResponseCallback != null && !StringUtils.isBlank(oldResponseCallback.lastModified)) {
            uriRequest.addHeader("If-Modified-Since", oldResponseCallback.lastModified);
            LogUtil.d(TAG, "addHandler If-Modified-Since:" + oldResponseCallback.lastModified);
        }
        if (oldResponseCallback != null && !StringUtils.isBlank(oldResponseCallback.eTeg)) {
            uriRequest.addHeader("If-None-Match", oldResponseCallback.eTeg);
            LogUtil.d(TAG, "addHandler Eteg:" + oldResponseCallback.eTeg);
        }

        uriRequest.addHeader("X_GEAKWATCH_SN", Build.SERIAL);

        ConnectException exception = null;
        ResponseCallback responseCallback = new ResponseCallback();
        try {
            HttpResponse response = client.execute(uriRequest, httpContext);
            StatusLine status = response.getStatusLine();
            //Header[] handlers = response.getAllHeaders();
            responseCallback.stateCode = status.getStatusCode();
            Header lmh = response.getFirstHeader("Last-Modified");
            if (lmh != null) {
                responseCallback.lastModified = lmh.getValue();
            }
            Header eth = response.getFirstHeader("ETag");
            if (eth != null) {
                responseCallback.eTeg = eth.getValue();
            }

            LogUtil.d(TAG, "stateCode == " + responseCallback.stateCode);
            LogUtil.d(TAG, "lastModified == " + responseCallback.lastModified);
            LogUtil.d(TAG, "Etag == " + responseCallback.eTeg);

            if (status.getStatusCode() == 200) {
                HttpEntity entity = null;
                HttpEntity temp = response.getEntity();
                if (temp != null) {
                    entity = new BufferedHttpEntity(temp);
                    String responseBody = EntityUtils.toString(entity, "UTF-8");
                    responseCallback.responseBody = responseBody;
                    LogUtil.d(TAG, "responseBody== " + responseCallback.responseBody);
                    return responseCallback;
                }
            } else if (status.getStatusCode() == 304) {
                return responseCallback;
            } else if (status.getStatusCode() < 500 && status.getStatusCode() >= 400) {
                exception = new ConnectException(ConnectException.ERROR_TYPE_CONNECT,
                        ConnectException.ERROR_CODE_404);
            } else {
                exception = new ConnectException(ConnectException.ERROR_TYPE_CONNECT,
                        ConnectException.ERROR_CODE_500);
            }

        } catch (UnknownHostException e) {// dns错误
            e.printStackTrace();
            exception = new ConnectException(ConnectException.ERROR_TYPE_CONNECT,
                    ConnectException.ERROR_CODE_DNS);

        } catch (SocketException e) {// 连接错误
            e.printStackTrace();
            exception = new ConnectException(ConnectException.ERROR_TYPE_CONNECT,
                    ConnectException.ERROR_CODE_SOCKET);
        } catch (SocketTimeoutException e) {// 连接超时
            e.printStackTrace();
            exception = new ConnectException(ConnectException.ERROR_TYPE_CONNECT,
                    ConnectException.ERROR_CODE_SOCKETTOMEOUT);
        } catch (IOException e) {// 服务器未响应
            e.printStackTrace();
            exception = new ConnectException(ConnectException.ERROR_TYPE_CONNECT,
                    ConnectException.ERROR_CODE_IO);
        } catch (Exception e) {
            e.printStackTrace();
            exception = new ConnectException(ConnectException.ERROR_TYPE_CONNECT,
                    ConnectException.ERROR_CODE_UNKONW);
        }

        if (exception == null) {
            exception = new ConnectException(ConnectException.ERROR_TYPE_CONNECT,
                    ConnectException.ERROR_CODE_UNKONW);
        }
        throw exception;
    }

    private HttpEntity paramsToEntity(RequestParams params) {
        HttpEntity entity = null;
        if (params != null) {
            entity = params.getEntity();
        }
        return entity;
    }

    public static String getHostIp(String host) {
        try {
            InetAddress rs = InetAddress.getByName(host);
            return rs.getHostAddress();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
    private String getUrlString(Map<String,String> map){
        StringBuilder builder = new StringBuilder();
        if(map!=null){
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key=entry.getKey();
                String value=entry.getValue();
                builder.append("&").append(key).append("=").append(value);
            }
        }else{
            return null;
        }
        return builder.toString().substring(1);
    }

}
