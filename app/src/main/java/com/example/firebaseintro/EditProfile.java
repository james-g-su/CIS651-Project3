package com.example.firebaseintro;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class EditProfile extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private EditText displayname, phonenumber;
    private static final int REQUEST_FOR_CAMERA=0011;
    private static final int OPEN_FILE=0012;
    private Uri imageUri=null;
    private DatabaseReference usersRef;
    private ImageView profileImage;
    private String currentPhotoPath;
    private final ActivityResultLauncher<Intent> mTakePicture = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::onActivityResult);
    ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    // Handle the returned Uri
                    imageUri = uri;
                    uploadImage();
                }
            });
    private void onActivityResult(ActivityResult result) {

        if (result.getData() == null) {
            Toast.makeText(this, "Couldn't capture the image. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        uploadImage();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        phonenumber=findViewById(R.id.phoneNumberText);
        displayname=findViewById(R.id.displayNameText);
        profileImage=findViewById(R.id.profileImage);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("Users/"+currentUser.getUid());
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                phonenumber.setText(dataSnapshot.child("phone").getValue().toString());
                displayname.setText(dataSnapshot.child("displayname").getValue().toString());
                if(dataSnapshot.child("profilePicture").exists())
                {
                    Picasso.get().load(dataSnapshot.child("profilePicture").getValue().toString()).transform(new CircleTransform()).into(profileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    private void uploadImage(){
        FirebaseStorage storage= FirebaseStorage.getInstance();
        final String fileNameInStorage= UUID.randomUUID().toString();
        String path="images/"+ fileNameInStorage+".jpg";
        final StorageReference imageRef=storage.getReference(path);
        imageRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(final Uri uri) {
                        usersRef.child("profilePicture").setValue(uri.toString()).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Picasso.get().load(uri.toString()).transform(new CircleTransform()).into(profileImage);
                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(EditProfile.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(EditProfile.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void uploadProfilePhoto(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.popup, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }
    public void Save(View view) {
        if(displayname.getText().toString().equals("") || phonenumber.getText().toString().equals(""))
        {
            Toast.makeText(this, "Please enter your display name and phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        usersRef.child("phone").setValue(phonenumber.getText().toString());
        usersRef.child("displayname").setValue(displayname.getText().toString());
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        finish();
    }
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
    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            // Error occurred while creating the File
            Toast.makeText(EditProfile.this, "Failed to create file", Toast.LENGTH_SHORT).show();
            return;
        }
        // Continue only if the File was successfully created
        imageUri = FileProvider.getUriForFile(this,
                "com.example.firebaseintro.fileprovider",
                photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            mTakePicture.launch(takePictureIntent);
        } else {
            Toast.makeText(this, "Couldn't found a camera Activity.", Toast.LENGTH_SHORT).show();
        }
    }
    private void checkPermissions(){
        takePhoto();
    }
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.take_photo) {
            checkPermissions();
            return true;
        } else if (itemId == R.id.upload) {
            mGetContent.launch("image/*");
            return true;
        }
        return false;
    }
}