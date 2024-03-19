package com.example.firebaseintro;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryDataEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserHome extends AppCompatActivity implements OnMapReadyCallback, ItemClickListener {

    SimpleDateFormat localDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private static final int REQUEST_FOR_LOCATION = 0012;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private final Uri imageUri = null;
    private RecyclerViewAdapter myRecyclerAdapter;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    FirebaseFirestore firestore_db = FirebaseFirestore.getInstance();
    private final DatabaseReference geo_fire_ref = database.getReference("/geo_loc");
    private final GeoFire geoFire = new GeoFire(geo_fire_ref);
    private GeoQuery geoQuery = null;
    String currentPhotoPath;
    private GoogleMap mMap;
    private final HashMap<String, PostModel> key_to_Post = new HashMap<>();
    private final List<String> keyList = new ArrayList<>();
    RecyclerView recyclerView;
    public static String TAG = "Project3";
    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::onActivityResult);

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void newLocation(Location lastLocation) {
        if (geoQuery != null)
            geoQuery.setCenter(new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));
        else {
            geoQuery = geoFire.queryAtLocation(new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()), 10);
            geoQuery.addGeoQueryDataEventListener(new GeoQueryDataEventListener() {
                @Override
                public void onDataEntered(DataSnapshot dataSnapshot, GeoLocation location) {
                    final String postKey = dataSnapshot.getKey();
                    if (key_to_Post.containsKey(postKey))
                        return;

                    firestore_db.collection("ImagePosts").document(postKey).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            Log.i(TAG, documentSnapshot.toString());
                            Marker temp = mMap.addMarker(new MarkerOptions().position(
                                    new LatLng(Double.parseDouble(documentSnapshot.get("lat").toString()),
                                            Double.parseDouble(documentSnapshot.get("lng").toString())))
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_grey)));
                            PostModel postModel = new PostModel(
                                    documentSnapshot.get("uid").toString(),
                                    documentSnapshot.get("description").toString(),
                                    documentSnapshot.get("url").toString(),
                                    documentSnapshot.getDate("timestamp").toString(),
                                    documentSnapshot.getId(),
                                    temp);
                            key_to_Post.put(documentSnapshot.getId(), postModel);
                            keyList.add(documentSnapshot.getId());
                            temp.setTag(documentSnapshot.getId());
                            myRecyclerAdapter.notifyItemInserted(keyList.size() -1);
                            recyclerView.scrollToPosition(keyList.size()-1);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, e.getMessage());
                        }
                    });
                    /*
                    database.getReference("ImagePosts/" + postKey).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Marker temp = mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(dataSnapshot.child("lat").getValue().toString()), Double.parseDouble(dataSnapshot.child("lng").getValue().toString())))
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_grey)));
                            PostModel postModel = new PostModel(dataSnapshot.child("uid").getValue().toString(),
                                    dataSnapshot.child("description").getValue().toString(),
                                    dataSnapshot.child("url").getValue().toString(),
                                    localDateFormat.format(new Date(Long.parseLong(dataSnapshot.child("timestamp").getValue().toString())))
                                    , dataSnapshot.getKey(), temp);
                            key_to_Post.put(postKey, postModel);
                            keyList.add(postKey);
                            temp.setTag(dataSnapshot.getKey());
                            myRecyclerAdapter.notifyItemInserted(keyList.size() - 1);
                            recyclerView.scrollToPosition(keyList.size() - 1);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    }); */


                }

                @Override
                public void onDataExited(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onDataMoved(DataSnapshot dataSnapshot, GeoLocation location) {

                }

                @Override
                public void onDataChanged(DataSnapshot dataSnapshot, GeoLocation location) {

                }

                @Override
                public void onGeoQueryReady() {

                }

                @Override
                public void onGeoQueryError(DatabaseError error) {

                }
            });
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_home);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        recyclerView = findViewById(R.id.recylcer_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        layoutManager.scrollToPosition(0);
        recyclerView.setLayoutManager(layoutManager);
        myRecyclerAdapter = new RecyclerViewAdapter(key_to_Post, keyList, this);
        recyclerView.setAdapter(myRecyclerAdapter);
        initializeLocationClient();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        //mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapFragment.getMapAsync(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.getMainLooper());
    }
    private void initializeLocationClient() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        /* 20 secs */
        long MAX_UPDATE_DELAY_INTERVAL = 1000 * 1000;
        /* 10 secs */
        long UPDATE_INTERVAL = 10 * 1000;
        /* 2 sec */
        long FASTEST_INTERVAL = 2000;
        mLocationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
                .setMaxUpdateDelayMillis(MAX_UPDATE_DELAY_INTERVAL)
                .build();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Location lastLocation = locationResult.getLastLocation();

                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude())).zoom(12).build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                newLocation(lastLocation);
            }
        };
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        int itemId = item.getItemId();
        if (itemId == R.id.signout) {
            mAuth.signOut();
            finish();
            return true;
        } else if (itemId == R.id.newUser) {
            createTestEntry();
            return true;
        } else if (itemId == R.id.edit_profile) {
            startActivity(new Intent(this, EditProfile.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createTestEntry() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference usersRef = database.getReference("Users");
        String pushKey = usersRef.push().getKey();
        usersRef.child(pushKey).setValue(new User("Test Display Name",
                "Test Email", "Test Phone"));
    }

    public void uploadNewPhoto(View view) {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(this, "This feature requires camera.", Toast.LENGTH_SHORT).show();
            return;
        }
        requestPermissions();
    }
    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            // Error occurred while creating the File
            Toast.makeText(UserHome.this, "Failed to create file", Toast.LENGTH_SHORT).show();
            return;
        }
        // Continue only if the File was successfully created
        Uri photoURI = FileProvider.getUriForFile(this,
                "com.example.firebaseintro.fileprovider",
                photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            cameraActivityResultLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(this, "Couldn't found a camera Activity.", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestPermissions() {
        // https://developer.android.com/media/camera/camera-deprecated/photobasics
        // https://developer.android.com/training/permissions/requesting
        // Permission request logic
        /*
        List<String> permsToRequest = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permsToRequest.add(READ_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, CAMERA) != PackageManager.PERMISSION_GRANTED){
            permsToRequest.add(CAMERA);
        }
        if(!permsToRequest.isEmpty()){
            String[] perms = new String[permsToRequest.size()];
            perms = permsToRequest.toArray(perms);
            multiplePermissionLauncher.launch(perms);
        } else {
            takePhoto();
        }
        */
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            multiplePermissionLauncher.launch(new String[]{READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_VISUAL_USER_SELECTED});
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            multiplePermissionLauncher.launch(new String[]{READ_MEDIA_IMAGES, READ_MEDIA_VIDEO});
        } else {
            multiplePermissionLauncher.launch(new String[]{READ_EXTERNAL_STORAGE, CAMERA});
        }
        */
        // Alternatively perms can be requested using requestPermissons and overriding onRequestPermissionsResult
        // https://developer.android.com/training/permissions/requesting#principles
        // requestPermissions(new String[]{READ_EXTERNAL_STORAGE, CAMERA}, 10);
        takePhoto();
    }

    private void onActivityResult(Map<String, Boolean> isGranted) {
        Log.d("PERMISSIONS", "Launcher result: " + isGranted.toString());

        if (isGranted.containsValue(false)) {
            Toast.makeText(this, "At least one of the permissions was not granted, launching again...", Toast.LENGTH_SHORT).show();
            return;
        }
        takePhoto();
    }

    private void onActivityResult(ActivityResult result) {
        if (result.getData() == null) {
            Toast.makeText(this, "Couldn't capture the image. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, PhotoPreview.class);
        intent.putExtra("uri", currentPhotoPath);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_FOR_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.getMainLooper());
                } else {
                    Toast.makeText(this, "The app will not perform correctly without your permission to access the device location.", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                String key = marker.getTag().toString();
                for (int i=0; i<keyList.size(); i++)
                {
                    if(keyList.get(i).equals(key)){
                        recyclerView.scrollToPosition(i);
                        break;
                    }

                }
                //show the image
                Toast.makeText(UserHome.this, "Marker clicked", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_FOR_LOCATION);
            return;
        }
        mMap.setMyLocationEnabled(true);

    }

    @Override
    public void onItmeClick(LatLng latLng) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng).zoom(12).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }
}