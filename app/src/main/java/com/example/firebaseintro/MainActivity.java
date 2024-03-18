package com.example.firebaseintro;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
    }
    @Override
    protected void onResume() {
        super.onResume();
        new CountDownTimer(3000, 1000) {
            public void onTick(long millisUntilFinished) {

            }
            public void onFinish() {
                if(currentUser==null){
                    Toast.makeText(MainActivity.this, "No user found", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MainActivity.this ,LoginSignup.class));
                    finish();
                }  else{
                    if(currentUser.isEmailVerified()) {
                        //Toast.makeText(MainActivity.this, "User already signed in", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, UserHome.class));
                        finish();
                    }
                    else{
                        Toast.makeText(MainActivity.this, "Please verify your email and login.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, LoginSignup.class));
                        finish();
                    }
                }
            }
        }.start();
    }
}