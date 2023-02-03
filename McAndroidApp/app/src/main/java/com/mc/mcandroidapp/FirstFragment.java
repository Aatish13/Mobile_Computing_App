package com.mc.mcandroidapp;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.mc.mcandroidapp.databinding.FragmentFirstBinding;
import com.mc.mcandroidapp.ml.Model1;
import com.mc.mcandroidapp.ml.Model2;
import com.mc.mcandroidapp.ml.Model3;
import com.mc.mcandroidapp.ml.Model4;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.lang3.*;

public class FirstFragment extends Fragment {

  private FragmentFirstBinding binding;
  private static final int pic_id = 123;
  private Uri imageUri;
  private String imageurl;
  public static Bitmap bitmap;
  ImageView click_image_id;
  public static ArrayList<Bitmap> chunkedImages;
  private String Tag = "NearBy";
  public static ConnectionsClient mConnectionsClient;
  private Strategy STRATEGY = Strategy.P2P_STAR;
  private boolean mIsConnecting = false;
  public static String PACKAGE_NAME;
  private String myCodeName = MainActivity.CodeName;
  public String otherEndpoint;
  private boolean mIsDiscovering = false;

  /** True if we are advertising. */
  private boolean mIsAdvertising = false;

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    binding = FragmentFirstBinding.inflate(inflater, container, false);
    mConnectionsClient = Nearby.getConnectionsClient(getContext());
    PACKAGE_NAME = "MyServiceMc"; // getContext().getPackageName();

    return binding.getRoot();
  }

  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    startDiscovery();
    binding.cameraButton.setOnClickListener(
        v -> {
          ContentValues values = new ContentValues();
          values.put(MediaStore.Images.Media.TITLE, "New Picture");
          values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
          imageUri =
              this.getActivity()
                  .getContentResolver()
                  .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
          Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
          intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
          startActivityForResult(intent, pic_id);
        });
    click_image_id = binding.clickImage;
  }

  public String getRealPathFromURI(Uri contentUri) {
    String[] proj = {MediaStore.Images.Media.DATA};
    Cursor cursor = this.getActivity().managedQuery(contentUri, proj, null, null, null);
    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
    cursor.moveToFirst();
    return cursor.getString(column_index);
  }

  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case pic_id:
        if (requestCode == pic_id)
          if (resultCode == this.getActivity().RESULT_OK) {
            try {
              imageurl = getRealPathFromURI(imageUri);
              Bitmap rotatedBitmap = rotateImage(setReducedImageSize());
              bitmap = rotatedBitmap;
              Bitmap cropthis = rotatedBitmap;
              click_image_id.setImageBitmap(bitmap);
              splitImage(convertToBlackWhite(cropthis));



            } catch (Exception e) {
              e.printStackTrace();
            }
            NavHostFragment.findNavController(FirstFragment.this)
                .navigate(R.id.action_FirstFragment_to_SecondFragment);
          }
    }
  }

  public Bitmap convertToBlackWhite(Bitmap bmp) {
    int width = bmp.getWidth();
    int height = bmp.getHeight();
    int[] pixels = new int[width * height];
    bmp.getPixels(pixels, 0, width, 0, 0, width, height);

    int alpha = 0xFF << 24; // ?bitmap?24?
    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        int grey = pixels[width * i + j];

        int red = ((grey & 0x00FF0000) >> 16);
        int green = ((grey & 0x0000FF00) >> 8);
        int blue = (grey & 0x000000FF);

        grey = (int) (red * 0.3 + green * 0.59 + blue * 0.11);
        grey = alpha | (grey << 16) | (grey << 8) | grey;
        pixels[width * i + j] = grey;
      }
    }
    Bitmap newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
    newBmp.setPixels(pixels, 0, width, 0, 0, width, height);
    return newBmp;
  }

  public Bitmap setReducedImageSize() {
    int targetImageViewWidth = click_image_id.getWidth();
    int targetImageViewHeight = click_image_id.getHeight();

    BitmapFactory.Options bmo = new BitmapFactory.Options();
    bmo.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(imageurl, bmo);
    int cameraImageWidth = bmo.outWidth;
    int cameraImageHeight = bmo.outHeight;

    int scaleF =
        Math.min(
            cameraImageWidth / targetImageViewWidth, cameraImageHeight / targetImageViewHeight);
    bmo.inSampleSize = scaleF;
    bmo.inJustDecodeBounds = false;
    return BitmapFactory.decodeFile(imageurl, bmo);
  }

  public Bitmap rotateImage(Bitmap bitmap) {
    ExifInterface ei = null;

    try {
      ei = new ExifInterface(imageurl);
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    int orientation =
        ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

    Matrix matrix = new Matrix();
    switch (orientation) {
      case ExifInterface.ORIENTATION_ROTATE_90:
        matrix.setRotate(90);
        break;

      case ExifInterface.ORIENTATION_ROTATE_180:
        matrix.setRotate(180);
        break;
      default:
    }
    Bitmap rotatedBitmap =
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    Bitmap cropthis = rotatedBitmap;
    splitImage(cropthis);
    return rotatedBitmap;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  private void splitImage(Bitmap bitmap) {

    int rows, cols;
    int chunkNumbers = 4;
    int chunkHeight, chunkWidth;
    chunkedImages = new ArrayList<Bitmap>(chunkNumbers);
    Bitmap scaledBitmap =
        Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);
    rows = cols = (int) Math.sqrt(chunkNumbers);
    chunkHeight = bitmap.getHeight() / rows;
    chunkWidth = bitmap.getWidth() / cols;
    int imgSeq = 0;
    int yCoord = 0;
    for (int x = 0; x < rows; x++) {
      int xCoord = 0;
      for (int y = 0; y < cols; y++) {
        Bitmap img = Bitmap.createBitmap(scaledBitmap, xCoord, yCoord, chunkWidth, chunkHeight);

        chunkedImages.add(img);
        imgSeq += 1;
        xCoord += chunkWidth;
      }
      yCoord += chunkHeight;
    }

  }

  public String getPrediction(Bitmap bitmap, int x) {
    try {

      Bitmap img = Bitmap.createScaledBitmap(bitmap, 14, 14, false);
      // Creates inputs for reference.
      TensorBuffer inputFeature0 =
          TensorBuffer.createFixedSize(new int[] {1, 14, 14, 1}, DataType.FLOAT32);
      TensorImage tI = new TensorImage(DataType.FLOAT32);
      tI.load(img);
      ByteBuffer by = ByteBuffer.allocateDirect(4 * 14 * 14);
      inputFeature0.loadBuffer(by);
      TensorBuffer output = null;
      int index = -1;
      float max = -999;

      if (x == 1) {
        Model1 model1 = Model1.newInstance(getActivity().getApplicationContext());
        Model1.Outputs outputs = model1.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
        output = outputFeature0;
        model1.close();
      } else if (x == 2) {
        Model2 model2 = Model2.newInstance(getActivity().getApplicationContext());
        Model2.Outputs outputs = model2.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
        output = outputFeature0;
        model2.close();
      } else if (x == 3) {
        Model3 model3 = Model3.newInstance(getActivity().getApplicationContext());
        Model3.Outputs outputs = model3.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
        output = outputFeature0;
        model3.close();
      } else if (x == 4) {
        Model4 model4 = Model4.newInstance(getActivity().getApplicationContext());
        Model4.Outputs outputs = model4.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
        output = outputFeature0;
        model4.close();
      }
      if (output.getFloatArray().length <= 0) {
        throw new IllegalArgumentException("The array is empty");
      }
      for (int i = 0; i < output.getFloatArray().length; i++)
        if (output.getFloatArray()[i] > max) {
          max = output.getFloatArray()[i];
          index = i;
        }
      return String.valueOf(index);

    } catch (IOException e) {
      e.printStackTrace();
    }
    return "error";
  }

  protected void startDiscovery() {

    binding.firstOutputTextView.append("\nStarted looking for Parent");
    final String localEndpointName = myCodeName;
    mIsDiscovering = true;
    DiscoveryOptions.Builder discoveryOptions = new DiscoveryOptions.Builder();
    discoveryOptions.setStrategy(STRATEGY);
    mConnectionsClient
        .startDiscovery(PACKAGE_NAME, endpointDiscoveryCallback, discoveryOptions.build())
        .addOnSuccessListener(
            new OnSuccessListener<Void>() {
              @Override
              public void onSuccess(Void unusedResult) {
                Log.d(Tag, "startDiscovering() Started. at endpoint = " + localEndpointName);
              }
            })
        .addOnFailureListener(
            new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
                mIsDiscovering = false;
                Log.d(Tag, "startDiscovering() failed.", e);
              }
            });
  }

  private EndpointDiscoveryCallback endpointDiscoveryCallback =
      new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(
            @NonNull String endpointId, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
          if (PACKAGE_NAME.equals(discoveredEndpointInfo.getServiceId())) {
            mConnectionsClient.requestConnection(
                myCodeName, endpointId, mConnectionLifecycleCallback);

            binding.firstOutputTextView.append("\nParent Found at EndpointId = "+endpointId);
            Log.d(Tag, "endpointDiscoveryCallback found ");
          }
        }

        @Override
        public void onEndpointLost(@NonNull String s) {}
      };

  private final PayloadCallback payloadCallback =
      new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {
          Log.d(Tag, "Data Recived");
          byte[] b = payload.asBytes();

          binding.firstOutputTextView.append("\nImage Recived to Predict");
          ImageData imgData = (ImageData) SerializationUtils.deserialize(b);
          Bitmap img = BitmapFactory.decodeByteArray(imgData.img, 0, imgData.img.length);
          binding.clickImage.setImageBitmap(img);
          String imgClass = "";
          imgClass = getPrediction(img, imgData.part) + "," + imgData.part;
          mConnectionsClient.sendPayload(
              otherEndpoint, Payload.fromBytes(imgClass.getBytes(StandardCharsets.UTF_8)));

          binding.firstOutputTextView.append("\nImage Classified and set back to parent App");
          Log.d(Tag, "Image Set");

        }

        @Override
        public void onPayloadTransferUpdate(
            @NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
          Log.d(Tag, "onPayloadTransferUpdate");
        }
      };

  private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
      new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
          Log.d(
              Tag,
              String.format(
                  "onConnectionInitiated(endpointId=%s, endpointName=%s)",
                  endpointId, connectionInfo.getEndpointName()));
          mConnectionsClient.acceptConnection(endpointId, payloadCallback);
        }

        @Override
        public void onConnectionResult(String endpointId, ConnectionResolution result) {
          Log.d(
              Tag,
              String.format(
                  "onConnectionResponse(endpointId=%s, result=%s)",
                  endpointId, result.getStatus()));
          mConnectionsClient.stopAdvertising();
          mConnectionsClient.stopDiscovery();
          //                    Log.d(Tag,"stopAdvertising,stopDiscovery");
          // We're no longer connecting
          mIsConnecting = false;
          otherEndpoint = endpointId;
          Log.d(Tag, "EndConnectionResult");
          if (!result.getStatus().isSuccess()) {
            Log.d(Tag, String.format("Connection failed. Received status %s."));
            return;
          }
        }

        @Override
        public void onDisconnected(String endpointId) {
          Log.d(Tag, "Unexpected disconnection from endpoint " + endpointId);
          startDiscovery();
          return;
        }
      };
}
