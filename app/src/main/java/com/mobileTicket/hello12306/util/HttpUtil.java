package com.mobileTicket.hello12306.util;


import android.support.annotation.AnyThread;
import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpUtil {

    private final static ExecutorService executorService = Executors.newCachedThreadPool();

    @AnyThread
    public static void jsonPost(final String requestUrl, final JSONObject jsonParam, final OnRequestResult requestResult) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                StringBuilder sb = new StringBuilder();
                HttpURLConnection urlConnection = null;
                try {
                    URL url = new URL(requestUrl);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setUseCaches(false);
                    urlConnection.setConnectTimeout(10000);
                    urlConnection.setReadTimeout(10000);
                    urlConnection.setRequestProperty("Content-Type", "application/json");
                    urlConnection.connect();
                    OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
                    out.write(jsonParam.toString());
                    out.close();
                    int HttpResult = urlConnection.getResponseCode();
                    if (HttpResult == HttpURLConnection.HTTP_OK) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(
                                urlConnection.getInputStream(), "utf-8"));
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        br.close();
                        if (requestResult != null) {
                            requestResult.onSuccess(sb.toString());
                        }
                    } else {
                        if (requestResult != null) {
                            requestResult.onFailure(HttpResult, urlConnection.getResponseMessage(), null);
                        }
                    }
                } catch (IOException e) {
                    if (requestResult != null) {
                        requestResult.onFailure(-1, e.getMessage(), e);
                    }
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }
        };
        executorService.execute(runnable);
    }

    public interface OnRequestResult {
        void onSuccess(String result);

        void onFailure(int code, String result, @Nullable Throwable throwable);
    }
}
