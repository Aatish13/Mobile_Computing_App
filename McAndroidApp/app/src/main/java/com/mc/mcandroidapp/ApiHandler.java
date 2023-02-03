package com.mc.mcandroidapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.GnssAntennaInfo;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class ApiHandler {
    private static final String TAG = "API";
    private String BaseURL = "http://192.168.0.36:3000/";
    private String URL = "http://192.168.0.36:3000/send-image/";

    public StringRequest uploadImage(Bitmap bitmap, String category, Response.Listener<String> response, Response.ErrorListener errorResponse) {
        Log.i(TAG, "Inside ApiHandler.uploadImage");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos); // bm is the bitmap object
        byte[] b = baos.toByteArray();
        String encodedImage = android.util.Base64.encodeToString(b, Base64.DEFAULT);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, response
                , errorResponse) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> imageParams = new HashMap<String, String>();
                imageParams.put("image", encodedImage);
                imageParams.put("category", category);
                return imageParams;
            }
        };

        Log.i(TAG, "uploadImage " + stringRequest);
        return stringRequest;
    }

    public StringRequest identifyImage(Bitmap bitmap, Response.Listener<String> response, Response.ErrorListener errorResponse) {
        Log.i(TAG, "Inside ApiHandler.uploadImage");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos); // bm is the bitmap object
        byte[] b = baos.toByteArray();
        String encodedImage = android.util.Base64.encodeToString(b, Base64.DEFAULT);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, BaseURL + "identify-image/", response
                , errorResponse) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> imageParams = new HashMap<String, String>();
                imageParams.put("image", encodedImage);
                return imageParams;
            }
        };

        Log.i(TAG, "uploadImage " + stringRequest);
        return stringRequest;
    }
}
