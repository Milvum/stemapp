package com.milvum.stemapp.ballotexchange;

import android.content.Context;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.milvum.stemapp.BuildConfig;
import com.milvum.stemapp.utils.Constants;
import com.milvum.stemapp.model.Cipher;
import com.milvum.stemapp.model.MixingToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.concurrent.Future;

/**
 * .
 *
 * To be used as a placeholder for the Whisper protocol.
 * The purpose of this class is to send a correctly formatted HTTP request
 *   to the endpoint that the mixer provides for redeeming signed mixing tokens for Ether + a ballot
 */
public class HttpRedeemClient {
    private static final String TAG = "HttpRedeemClient";

    private final RequestQueue requestQueue;

    public HttpRedeemClient(Context context) {
        requestQueue = Volley.newRequestQueue(context);
    }

    public Future<JSONObject> sendRequest(String url, int requestMethod, String payload) {
        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("payload", payload);
        } catch(JSONException e) {
            Log.wtf(TAG, "JSONException when creating a request for " + url + " with payload " + payload, e);
        }

        // @TODO Remove this in production! We don't want to log the voting ballot address!
        Log.d(TAG, "Sending redeem request to " + url +
                " with parameters: " + jsonParams.toString());

        RequestFuture<JSONObject> future = RequestFuture.newFuture();

        JsonRequest<JSONObject> request = new JsonRequest<JSONObject>(requestMethod, url, jsonParams.toString(), future, future) {
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                String jsonString;
                try {
                    jsonString = new String(response.data,
                            HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
                } catch (UnsupportedEncodingException e) {
                    return Response.error(new ParseError(e));
                }

                return Response.success(safeJSONParse(jsonString),
                        HttpHeaderParser.parseCacheHeaders(response));
            }

            private JSONObject safeJSONParse(String jsonString) {
                try {
                    return new JSONObject(jsonString);
                } catch (JSONException e) {
                    return new JSONObject();
                }
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy( Constants.BEG_TIMEOUT_SECONDS * 1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);

        return future;
    }

    /**
     * @return a Future that (when .get() is called) returns null if the request
     *   was successful (2xx status code) or throws an error if it wasn't (4xx or 5xx)
     */
    public Future<JSONObject> sendRedeemRequest(MixingToken mixingToken, BigInteger signature) {
        String payload = mixingToken.toString() + "-" + signature.toString(16);

        return sendRequest(BuildConfig.REDEEM_ADDRESS, Request.Method.POST, payload);
    }

    public Future<JSONObject> beg(String address) {
        return sendRequest(BuildConfig.BEG_ADDRESS, Request.Method.POST, address);
    }
}
