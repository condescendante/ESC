package com.example.godhelpme;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

// Add ground overlay to map
public class MapsActivity extends AppCompatActivity implements OnSeekBarChangeListener, OnMapReadyCallback, GoogleMap.OnGroundOverlayClickListener {

    private static final int TRANSPARENCY_MAX = 100;

    private static final int BEARING_MOVE = 17;

    // This is the exact latlng for SUTD (Don't change it)
    private static LatLng SUTD = new LatLng(1.34, 103.962);

    //private static final LatLng NEAR_SUTD = new LatLng(SUTD.latitude - 0.001, SUTD.longitude - 0.025);

    private final List<BitmapDescriptor> images = new ArrayList<BitmapDescriptor>();

    private GroundOverlay groundOverlay;

    private GroundOverlay groundOverlayRotated;

    private SeekBar transparencyBar;

    private SeekBar rotationBar;

    private int currentEntry = 0;

    FirebaseStorage firebaseStorage;
    StorageReference storageReference;
    StorageReference ref;

    EditText latInput;
    EditText lngInput;
    Button Confirm;
    Button Clear;
    Button Download;
    Button Upload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Create transparency bar to adjust opacity of floor plan
        transparencyBar = findViewById(R.id.transparencySeekBar);
        transparencyBar.setMax(TRANSPARENCY_MAX);
        transparencyBar.setProgress(0);

        // Create rotation bar
        rotationBar = findViewById(R.id.rotationSeekBar);
        rotationBar.setMax(BEARING_MOVE);
        rotationBar.setProgress(0);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        // Register a listener to respond to clicks on GroundOverlays.
        map.setOnGroundOverlayClickListener(this);

        // LatLng References entered by user
        latInput = findViewById(R.id.LatInput);
        lngInput = findViewById(R.id.LngInput);
        Confirm = findViewById(R.id.EnterLatLng);
        Clear = findViewById(R.id.ClearImages);
        Download = findViewById(R.id.downloadImage);

        Confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SUTD = new LatLng(Double.parseDouble(latInput.getText().toString()), Double.parseDouble(lngInput.getText().toString()));
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(SUTD, 25));

                // SUTD Floorplan Overlay (Adjustable through transparency bar)
                // Currently, the bottom left corner is used as the anchor
                groundOverlay = map.addGroundOverlay(new GroundOverlayOptions()
                        .image(images.get(currentEntry)).anchor(0, 1)
                        .bearing(0)
                        .position(SUTD, 86f, 65f));
            }
        });

        Clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                groundOverlay.remove();
            }
        });

        Download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                download();

            }
        });

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(SUTD, 18));

        // Remove any existing images
        images.clear();

        // TODO: Download images from firebase and convert them into bitmap format
        // Add bitmap images to the images array

        images.add(BitmapDescriptorFactory.fromResource(R.drawable.sutdmap));
        images.add(BitmapDescriptorFactory.fromResource(R.drawable.download));


        // Night Fiesta Overlay (Features --> Rotated, clickable to adjust transparency)
        /*groundOverlayRotated = map.addGroundOverlay(new GroundOverlayOptions()
                .image(images.get(1)).anchor(0, 1)
                .position(NEAR_SUTD, 4300f, 3025f)
                .bearing(30)
                .clickable(((CheckBox) findViewById(R.id.toggleClickability)).isChecked()));*/

        // SUTD Floorplan Overlay (Adjustable through transparency bar)
        /*groundOverlay = map.addGroundOverlay(new GroundOverlayOptions()
                .image(images.get(currentEntry)).anchor(0, 1)
                .position(SUTD, 86f, 65f));*/

        transparencyBar.setOnSeekBarChangeListener(this);
        rotationBar.setOnSeekBarChangeListener(this);

        // Override the default content description on the view, for accessibility mode.
        // Ideally this string would be localised.
        map.setContentDescription("Google Map with ground overlay.");
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (groundOverlay != null && seekBar == transparencyBar) {
            groundOverlay.setTransparency((float) progress / (float) TRANSPARENCY_MAX);
        }
        if (groundOverlay != null && seekBar == rotationBar) {
            groundOverlay.setBearing((float) progress * (float) BEARING_MOVE);
        }
    }

    // Download image from firebase
    // If future authentication is required for firebase, go to RULES setting in firebase and change == to != NULL
    public void download(){
        storageReference = firebaseStorage.getInstance().getReference();
        ref = storageReference.child("SUTD MAP.png");

        ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                String url = uri.toString();
//                try {
//                    URL url2 = new URL(url);
//                    Bitmap image2 = BitmapFactory.decodeStream(url2.openConnection().getInputStream());
//                    images.add(BitmapDescriptorFactory.fromBitmap(image2));
//                } catch(IOException e) {
//                    System.out.println("123456789");
//                }
                downloadFile(MapsActivity.this, "SUTD MAP", ".png", DIRECTORY_DOWNLOADS, url);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    // Download manager to retrieve file from firebase
    public void downloadFile(Context context, String fileName, String fileExtension, String destinationDirectory, String url){
//        if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
//                ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
//                ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
//            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123);
//            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.INTERNET}, 123);
//            Toast.makeText(MapsActivity.this, "Need Permission to access storage for Downloading Image", Toast.LENGTH_LONG).show();
//        } else {
//            Toast.makeText(MapsActivity.this, "Downloading Image...", Toast.LENGTH_LONG).show();
//            Glide.with(MapsActivity.this)
//                    .using(new FirebaseImageLoader()) // <== ADD THIS
//                    .load(storageReference)
//                    .signature(new StringSignature(localFile.length() + "@" + localFile.lastModified()))
//                    .into(images.get(i));

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(url);
            DownloadManager.Request request = new DownloadManager.Request(uri);

//        try {
//            Bitmap image = getBitmapFormUri(downloadFile(), uri);
//            images.add(BitmapDescriptorFactory.fromBitmap(image));
//        } catch(IOException e) {
//            System.out.println("123456789");
//        }

            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(context, destinationDirectory, fileName + fileExtension);

            downloadManager.enqueue(request);
        }



//    public static Bitmap getBitmapFormUri(Activity ac, Uri uri) throws FileNotFoundException, IOException {
//        InputStream input = ac.getContentResolver().openInputStream(uri);
//        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
//        onlyBoundsOptions.inJustDecodeBounds = true;
//        onlyBoundsOptions.inDither = true;//optional
//        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
//        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
//        input.close();
//        int originalWidth = onlyBoundsOptions.outWidth;
//        int originalHeight = onlyBoundsOptions.outHeight;
//        if ((originalWidth == -1) || (originalHeight == -1))
//            return null;
//        //Image resolution is based on 480x800
//        float hh = 800f;//The height is set as 800f here
//        float ww = 480f;//Set the width here to 480f
//        //Zoom ratio. Because it is a fixed scale, only one data of height or width is used for calculation
//        int be = 1;//be=1 means no scaling
//        if (originalWidth > originalHeight && originalWidth > ww) {//If the width is large, scale according to the fixed size of the width
//            be = (int) (originalWidth / ww);
//        } else if (originalWidth < originalHeight && originalHeight > hh) {//If the height is high, scale according to the fixed size of the width
//            be = (int) (originalHeight / hh);
//        }
//        if (be <= 0)
//            be = 1;
//        //Proportional compression
//        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
//        bitmapOptions.inSampleSize = be;//Set scaling
//        bitmapOptions.inDither = true;//optional
//        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
//        input = ac.getContentResolver().openInputStream(uri);
//        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
//        input.close();
//
//        return compressImage(bitmap);//Mass compression again
//    }
//
//    public static Bitmap compressImage(Bitmap image) {
//
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//Quality compression method, here 100 means no compression, store the compressed data in the BIOS
//        int options = 100;
//        while (baos.toByteArray().length / 1024 > 100) {  //Cycle to determine if the compressed image is greater than 100kb, greater than continue compression
//            baos.reset();//Reset the BIOS to clear it
//            //First parameter: picture format, second parameter: picture quality, 100 is the highest, 0 is the worst, third parameter: save the compressed data stream
//            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//Here, the compression options are used to store the compressed data in the BIOS
//            options -= 10;//10 less each time
//        }
//        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//Store the compressed data in ByteArrayInputStream
//        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//Generate image from ByteArrayInputStream data
//        return bitmap;
//    }

    // Change to another image
    public void switchImage(View view) {
        currentEntry = (currentEntry + 1) % images.size();
        groundOverlay.setImage(images.get(currentEntry));
    }

    /**
     * Toggles the visibility between 100% and 50% when a {@link GroundOverlay} is clicked.
     */
    @Override
    public void onGroundOverlayClick(GroundOverlay groundOverlay) {
        // Toggle transparency value between 0.0f and 0.5f. Initial default value is 0.0f.
        groundOverlayRotated.setTransparency(0.5f - groundOverlayRotated.getTransparency());
    }

    /**
     * Toggles the clickability of the smaller, rotated overlay based on the state of the View that
     * triggered this call.
     * This callback is defined on the CheckBox in the layout for this Activity.
     */
    public void toggleClickability(View view) {
        if (groundOverlayRotated != null) {
            groundOverlayRotated.setClickable(((CheckBox) view).isChecked());
        }
    }
}