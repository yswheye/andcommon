package com.android.gary.common.asynchttp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.SyncBasicHttpContext;

import android.util.Log;

import com.android.gary.common.synchttp.RetryHandler;
import com.android.gary.common.utils.LogUtil;

public class AsyncHttpClient {

    public static final String LOG_TAG = "AsyncHttpClient";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_RANGE = "Content-Range";
    public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
    public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    public static final String ENCODING_GZIP = "gzip";
    public static final int DEFAULT_MAX_CONNECTIONS = 10;
    public static final int DEFAULT_SOCKET_TIMEOUT = 10000;
    public static final int DEFAULT_MAX_RETRIES = 5;
    public static final int DEFAULT_RETRY_SLEEP_TIME_MILLIS = 1500;
    public static final int DEFAULT_SOCKET_BUFFER_SIZE = 8192;
    private int maxConnections = 10;
    private int connectTimeout = 10000;
    private int responseTimeout = 10000;
    private final DefaultHttpClient httpClient;
    private final HttpContext httpContext;
    private ExecutorService threadPool;
    private final Map<String, String> clientHeaderMap;
    private boolean isUrlEncodingEnabled = true;

    public AsyncHttpClient() {
        this(false, 80, 443);
    }

    public AsyncHttpClient(int httpPort) {
        this(false, httpPort, 443);
    }

    public AsyncHttpClient(int httpPort, int httpsPort) {
        this(false, httpPort, httpsPort);
    }

    public AsyncHttpClient(boolean fixNoHttpResponseException, int httpPort,
                           int httpsPort) {
        this(getDefaultSchemeRegistry(fixNoHttpResponseException, httpPort,
                httpsPort));
    }

    private static SchemeRegistry getDefaultSchemeRegistry(
            boolean fixNoHttpResponseException, int httpPort, int httpsPort) {
        if (fixNoHttpResponseException) {
            LogUtil.d(LOG_TAG,
                    "Beware! Using the fix is insecure, as it doesn't verify SSL certificates.");
        }

        if (httpPort < 1) {
            httpPort = 80;
            LogUtil.d(LOG_TAG,
                    "Invalid HTTP port number specified, defaulting to 80");
        }

        if (httpsPort < 1) {
            httpsPort = 443;
            LogUtil.d(LOG_TAG,
                    "Invalid HTTPS port number specified, defaulting to 443");
        }
        SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory
                .getSocketFactory(), httpPort));
        schemeRegistry
                .register(new Scheme("https", sslSocketFactory, httpsPort));

        return schemeRegistry;
    }

    public AsyncHttpClient(SchemeRegistry schemeRegistry) {
        BasicHttpParams httpParams = new BasicHttpParams();

        ConnManagerParams.setTimeout(httpParams, connectTimeout);
        ConnManagerParams.setMaxConnectionsPerRoute(httpParams,
                new ConnPerRouteBean(maxConnections));
        ConnManagerParams.setMaxTotalConnections(httpParams, 10);

        HttpConnectionParams.setSoTimeout(httpParams, responseTimeout);
        HttpConnectionParams.setConnectionTimeout(httpParams, connectTimeout);
        HttpConnectionParams.setTcpNoDelay(httpParams, true);
        HttpConnectionParams.setSocketBufferSize(httpParams, 8192);

        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);

        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(
                httpParams, schemeRegistry);

        threadPool = getDefaultThreadPool();
        clientHeaderMap = new HashMap<String, String>();

        httpContext = new SyncBasicHttpContext(new BasicHttpContext());
        httpClient = new DefaultHttpClient(cm, httpParams);
        httpClient.addRequestInterceptor(new HttpRequestInterceptor() {
            public void process(HttpRequest request, HttpContext context) {
                if (!request.containsHeader("Accept-Encoding")) {
                    request.addHeader("Accept-Encoding", "gzip");
                }
                for (String header : clientHeaderMap.keySet()) {
                    if (request.containsHeader(header)) {
                        Header overwritten = request.getFirstHeader(header);
                        request.removeHeader(overwritten);
                    }
                    request.addHeader(header,
                            (String) clientHeaderMap.get(header));
                }
            }
        });
        httpClient.addResponseInterceptor(new HttpResponseInterceptor() {
            public void process(HttpResponse response, HttpContext context) {
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    return;
                }
                Header encoding = entity.getContentEncoding();
                if (encoding != null)
                    for (HeaderElement element : encoding.getElements())
                        if (element.getName().equalsIgnoreCase("gzip")) {
                            response.setEntity(new AsyncHttpClient.InflatingEntity(
                                    entity));
                            return;
                        }
            }
        });
        httpClient.addRequestInterceptor(new HttpRequestInterceptor() {
            public void process(HttpRequest request, HttpContext context)
                    throws HttpException, IOException {
                AuthState authState = (AuthState) context
                        .getAttribute("http.auth.target-scope");
                CredentialsProvider credsProvider = (CredentialsProvider) context
                        .getAttribute("http.auth.credentials-provider");

                HttpHost targetHost = (HttpHost) context
                        .getAttribute("http.target_host");

                if (authState.getAuthScheme() == null) {
                    AuthScope authScope = new AuthScope(targetHost
                            .getHostName(), targetHost.getPort());
                    Credentials creds = credsProvider.getCredentials(authScope);
                    if (creds != null) {
                        authState.setAuthScheme(new BasicScheme());
                        authState.setCredentials(creds);
                    }
                }
            }
        }, 0);

        httpClient.setHttpRequestRetryHandler(new RetryHandler(5, 1500));
    }

    public static void endEntityViaReflection(HttpEntity entity) {
        if (!(entity instanceof HttpEntityWrapper))
            return;
        try {
            Field f = null;
            Field[] fields = HttpEntityWrapper.class.getDeclaredFields();
            for (Field ff : fields) {
                if (ff.getName().equals("wrappedEntity")) {
                    f = ff;
                    break;
                }
            }
            if (f != null) {
                f.setAccessible(true);
                HttpEntity wrapped = (HttpEntity) f.get(entity);
                if (wrapped != null)
                    wrapped.consumeContent();
            }
        } catch (Throwable t) {
            LogUtil.e(LOG_TAG, "wrappedEntity consume", t);
        }
    }

    public int getTimeout() {
        return connectTimeout;
    }

    public void setTimeout(int value) {
        value = (value < 1000) ? 10000 : value;
        setConnectTimeout(value);
        setResponseTimeout(value);
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int value) {
        connectTimeout = ((value < 1000) ? 10000 : value);
        HttpParams httpParams = httpClient.getParams();
        ConnManagerParams.setTimeout(httpParams, connectTimeout);
        HttpConnectionParams.setConnectionTimeout(httpParams, connectTimeout);
    }

    public int getResponseTimeout() {
        return responseTimeout;
    }

    public void setResponseTimeout(int value) {
        responseTimeout = ((value < 1000) ? 10000 : value);
        HttpParams httpParams = httpClient.getParams();
        HttpConnectionParams.setSoTimeout(httpParams, responseTimeout);
    }

    protected ExecutorService getDefaultThreadPool() {
        return Executors.newCachedThreadPool();
    }

    public void get(String url, ResponseHandlerInterface responseHandler) {
        sendRequest(httpClient, httpContext,
                new HttpGet(getUrlWithQueryString(isUrlEncodingEnabled, url)),
                null, responseHandler);
    }

    public static String getUrlWithQueryString(boolean shouldEncodeUrl,
                                               String url) {
        if (url == null) {
            return null;
        }
        if (shouldEncodeUrl) {
            url = url.replace(" ", "%20");
        }
        return url;
    }

    protected void sendRequest(DefaultHttpClient client,
                               HttpContext httpContext, HttpUriRequest uriRequest,
                               String contentType, ResponseHandlerInterface responseHandler) {
        if (uriRequest == null) {
            throw new IllegalArgumentException(
                    "HttpUriRequest must not be null");
        }

        if (responseHandler == null) {
            throw new IllegalArgumentException(
                    "ResponseHandler must not be null");
        }

        if (responseHandler.getUseSynchronousMode()) {
            throw new IllegalArgumentException(
                    "Synchronous ResponseHandler used in AsyncHttpClient. You should create your response handler in a looper thread or use SyncHttpClient instead.");
        }

        if (contentType != null) {
            if ((uriRequest instanceof HttpEntityEnclosingRequestBase)
                    && (((HttpEntityEnclosingRequestBase) uriRequest)
                    .getEntity() != null))
                LogUtil.w(LOG_TAG,
                        "Passed contentType will be ignored because HttpEntity sets content type");
            else {
                uriRequest.setHeader("Content-Type", contentType);
            }
        }

        responseHandler.setRequestHeaders(uriRequest.getAllHeaders());
        responseHandler.setRequestURI(uriRequest.getURI());

        AsyncHttpRequest request = newAsyncHttpRequest(client, httpContext,
                uriRequest, responseHandler);
        threadPool.submit(request);
    }

    private static class InflatingEntity extends HttpEntityWrapper {
        InputStream wrappedStream;
        PushbackInputStream pushbackStream;
        GZIPInputStream gzippedStream;

        public InflatingEntity(HttpEntity wrapped) {
            super(wrapped);
        }

        public InputStream getContent() throws IOException {
            wrappedStream = wrappedEntity.getContent();
            pushbackStream = new PushbackInputStream(wrappedStream, 2);
            if (AsyncHttpClient.isInputStreamGZIPCompressed(pushbackStream)) {
                gzippedStream = new GZIPInputStream(pushbackStream);
                return gzippedStream;
            }
            return pushbackStream;
        }

        public long getContentLength() {
            return (wrappedEntity == null) ? 0L : wrappedEntity
                    .getContentLength();
        }

        public void consumeContent() throws IOException {
            AsyncHttpClient.silentCloseInputStream(wrappedStream);
            AsyncHttpClient.silentCloseInputStream(pushbackStream);
            AsyncHttpClient.silentCloseInputStream(gzippedStream);
            super.consumeContent();
        }
    }

    protected AsyncHttpRequest newAsyncHttpRequest(DefaultHttpClient client,
                                                   HttpContext httpContext, HttpUriRequest uriRequest,
                                                   ResponseHandlerInterface responseHandler) {
        return new AsyncHttpRequest(client, httpContext, uriRequest,
                responseHandler);
    }

    public static void silentCloseInputStream(InputStream is) {
        try {
            if (is != null)
                is.close();
        } catch (IOException e) {
            LogUtil.w(LOG_TAG, "Cannot close input stream", e);
        }
    }

    public static boolean isInputStreamGZIPCompressed(
            PushbackInputStream inputStream) throws IOException {
        if (inputStream == null) {
            return false;
        }
        byte[] signature = new byte[2];
        int readStatus = inputStream.read(signature);
        inputStream.unread(signature);
        int streamHeader = signature[0] & 0xFF | signature[1] << 8 & 0xFF00;
        return (readStatus == 2) && (35615 == streamHeader);
    }

    public static void silentCloseOutputStream(OutputStream os) {
        try {
            if(os != null) {
                os.close();
            }
        } catch (IOException var2) {
            Log.w("AsyncHttpClient", "Cannot close output stream", var2);
        }

    }
}
