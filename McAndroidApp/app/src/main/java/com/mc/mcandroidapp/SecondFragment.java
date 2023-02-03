package com.mc.mcandroidapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.Object;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.mc.mcandroidapp.databinding.FragmentSecondBinding;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.apache.commons.lang3.SerializationUtils;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;
    RequestQueue requestQueue;
    private String Tag = "NearBy";
    private ConnectionsClient mConnectionsClient ;
    private Strategy STRATEGY = Strategy.P2P_STAR;
    private boolean mIsConnecting = false;
    public static String PACKAGE_NAME;
    public List<String> responseData;
    public List<String> otherEndpoint;
    public TextView outputTextView;
    /** True if we are discovering. */
    private boolean mIsDiscovering = false;

    /** True if we are advertising. */
    private boolean mIsAdvertising = false;

    private String myCodeName =MainActivity.CodeName;
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentSecondBinding.inflate(inflater, container, false);
        mConnectionsClient =  FirstFragment.mConnectionsClient;
        PACKAGE_NAME = "MyServiceMc";//getContext().getPackageName();
        otherEndpoint = new ArrayList<>();
        responseData = new ArrayList<>();
        outputTextView = binding.outputTextView;
        outputTextView.append("Looking For Devices .........");
        binding.uploadButton.setEnabled(false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getContext(), R.array.category, android.R.layout.simple_spinner_item);
        requestQueue = Volley.newRequestQueue(getContext());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        //binding.imgCategorySpinner.setAdapter(adapter);
        binding.uploadImage.setImageBitmap(FirstFragment.bitmap);
        startAdvertising();
        binding.uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ObjectOutputStream out = null;

                    for(int i=1; i <= FirstFragment.chunkedImages.size();i++){
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        FirstFragment.chunkedImages.get(i-1).compress(Bitmap.CompressFormat.JPEG, 10, baos); // bm is the bitmap object
                        ImageData ig = new ImageData();
                        ig.part=i;
                        ig.img = baos.toByteArray();
                        byte[] b = SerializationUtils.serialize(ig);
                        String DataToSend = "This is working enjoy";
                        mConnectionsClient.sendPayload(otherEndpoint.get(i%otherEndpoint.size()),Payload.fromBytes(b));
                        //mConnectionsClient.sendPayload(otherEndpoint,Payload.fromBytes(DataToSend.getBytes()));
                        Log.d(Tag,"PayloadSend at "+otherEndpoint.get(i%otherEndpoint.size()));
                        outputTextView.append("\n"+"PayloadSend at "+otherEndpoint.get(i%otherEndpoint.size()));

                    }

            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {
                    Log.d(Tag,"Data Recived");
                    byte[] b = payload.asBytes();
                    String res = new String(b);
                    responseData.add(res);
                    Log.d(Tag,"Res = "+res);
                    outputTextView.append("\nDevice "+s+" predicted part "+res.split(",")[1]+" as "+res.split(",")[0]);
                    if(responseData.size()==4){
                        CharSequence text = "Image Predicted as ";
                        Log.d(Tag,"Image Predicted");
                        Integer predictedClass = combineOutput();
                        saveTempBitmap(FirstFragment.bitmap,predictedClass.toString());
                        outputTextView.append("\n"+text +predictedClass);
                        mConnectionsClient.stopAdvertising();
                        mConnectionsClient.stopDiscovery();
                        mConnectionsClient.stopAllEndpoints();
                    }
                }

                @Override
                public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
                    Log.d(Tag,"onPayloadTransferUpdate");

                }
            };

    public Integer combineOutput(){
        int[] freq = {0,0,0,0,0,0,0,0,0,0};
        for(int i=1; i <= responseData.size();i++){
            freq[Integer.parseInt(responseData.get(i-1).split(",")[0])]++;
        }
        int maxf = -1;
        int maxi = -1;
        for (int i =0 ;i<10;i++){
            if(maxf<freq[i]){
                maxf = freq[i];
                maxi = i;
            }
        }
        return maxi;
    }
    public void saveTempBitmap(Bitmap bitmap,String folder) {
        if (isExternalStorageWritable()) {
            saveImage(bitmap,folder);
        }else{
        }
    }

    private void saveImage(Bitmap finalBitmap,String Folder) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/McOutput_"+Folder);
        Log.d(Tag,"Dir created"+myDir.mkdirs());
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fname = "digit_"+ timeStamp +".jpg";

        File file = new File(myDir, fname);
        if (file.exists()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    Log.d(Tag,String.format(
                                    "onConnectionInitiated(endpointId=%s, endpointName=%s)",
                                    endpointId, connectionInfo.getEndpointName()));

                    if (!otherEndpoint.contains(endpointId))
                    {
                        mConnectionsClient.acceptConnection(endpointId,payloadCallback);

                    }
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    Log.d(Tag,String.format("onConnectionResponse(endpointId=%s, result=%s)", endpointId, result.getStatus()));
//                    Log.d(Tag,"stopAdvertising,stopDiscovery");
                    // We're no longer connecting
                    mIsConnecting = false;
                    otherEndpoint.add(endpointId);
                    binding.uploadButton.setEnabled(true);
                    Log.d(Tag,"EndConnectionResult");
                    outputTextView.append("\n"+String.format("Edge Device Connected(endpointId=%s)", endpointId));

                    if (!result.getStatus().isSuccess()) {
                        Log.d(Tag,String.format(
                                        "Connection failed. Received status %s."));
                        return;
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    Log.d(Tag,"Unexpected disconnection from endpoint " + endpointId);
                    return;
                }
            };

    protected void startAdvertising() {
        mIsAdvertising = true;
        final String localEndpointName =myCodeName ;

        AdvertisingOptions.Builder advertisingOptions = new AdvertisingOptions.Builder();
        advertisingOptions.setStrategy(STRATEGY);

        mConnectionsClient
                .startAdvertising(
                        localEndpointName,
                        PACKAGE_NAME,
                        mConnectionLifecycleCallback,
                        advertisingOptions.build())
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                Log.d(Tag,"Now advertising endpoint " + localEndpointName);


                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIsAdvertising = false;
                                Log.e(Tag,"startAdvertising() failed.", e);
                            }
                        });
    }


}