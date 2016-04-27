package com.android.gary.common.asynchttp;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.protocol.HttpContext;

import com.android.gary.common.utils.LogUtil;

public class AsyncHttpRequest implements Runnable {
    private final AbstractHttpClient client;
    private final HttpContext context;
    private final HttpUriRequest request;
    private final ResponseHandlerInterface responseHandler;
    private int executionCount;
    private boolean isCancelled;
    private boolean cancelIsNotified;
    private boolean isFinished;
    private boolean isRequestPreProcessed;

    public AsyncHttpRequest(AbstractHttpClient client, HttpContext context,
                            HttpUriRequest request, ResponseHandlerInterface responseHandler) {
        this.client = client;
        this.context = context;
        this.request = request;
        this.responseHandler = responseHandler;
    }

    public void onPreProcessRequest(AsyncHttpRequest request) {
    }

    public void onPostProcessRequest(AsyncHttpRequest request) {
    }

    public void run() {
        if (isCancelled()) {
            return;
        }

        if (!isRequestPreProcessed) {
            isRequestPreProcessed = true;
            onPreProcessRequest(this);
        }

        if (isCancelled()) {
            return;
        }

        if (responseHandler != null) {
            responseHandler.sendStartMessage();
        }

        if (isCancelled()) {
            return;
        }
        try {
            makeRequestWithRetries();
        } catch (IOException e) {
            if ((!isCancelled()) && (responseHandler != null))
                responseHandler.sendFailureMessage(0, null, null, e);
            else {
                LogUtil.e(
                        "AsyncHttpRequest",
                        "makeRequestWithRetries returned error, but handler is null",
                        e);
            }
        }

        if (isCancelled()) {
            return;
        }

        if (responseHandler != null) {
            responseHandler.sendFinishMessage();
        }

        if (isCancelled()) {
            return;
        }

        onPostProcessRequest(this);

        isFinished = true;
    }

    private void makeRequest() throws IOException {
        if (isCancelled()) {
            return;
        }

        if (request.getURI().getScheme() == null) {
            throw new MalformedURLException("No valid URI scheme was provided");
        }

        HttpResponse response = client.execute(request, context);

        if ((isCancelled()) || (responseHandler == null)) {
            return;
        }

        responseHandler.onPreProcessResponse(responseHandler, response);

        if (isCancelled()) {
            return;
        }

        responseHandler.sendResponseMessage(response);

        if (isCancelled()) {
            return;
        }

        responseHandler.onPostProcessResponse(responseHandler, response);
    }

    private void makeRequestWithRetries() throws IOException {
        boolean retry = true;
        IOException cause = null;
        HttpRequestRetryHandler retryHandler = client
                .getHttpRequestRetryHandler();
        try {
            while (retry) {
                try {
                    makeRequest();
                    return;
                } catch (UnknownHostException e) {
                    cause = new IOException("UnknownHostException exception: "
                            + e.getMessage());
                    retry = (executionCount > 0)
                            && (retryHandler.retryRequest(cause,
                            ++executionCount, context));
                } catch (NullPointerException e) {
                    cause = new IOException("NPE in HttpClient: "
                            + e.getMessage());
                    retry = retryHandler.retryRequest(cause, ++executionCount,
                            context);
                } catch (IOException e) {
                    if (isCancelled()) {
                        return;
                    }
                    cause = e;
                    retry = retryHandler.retryRequest(cause, ++executionCount,
                            context);
                }
                if ((retry) && (responseHandler != null))
                    ;
                responseHandler.sendRetryMessage(executionCount);
            }
        } catch (Exception e) {
            LogUtil.e("AsyncHttpRequest", "Unhandled exception origin cause", e);
            cause = new IOException("Unhandled exception: " + e.getMessage());
        }

        throw cause;
    }

    public boolean isCancelled() {
        if (isCancelled) {
            sendCancelNotification();
        }
        return isCancelled;
    }

    private synchronized void sendCancelNotification() {
        if ((!isFinished) && (isCancelled) && (!cancelIsNotified)) {
            cancelIsNotified = true;
            if (responseHandler != null)
                responseHandler.sendCancelMessage();
        }
    }
}
