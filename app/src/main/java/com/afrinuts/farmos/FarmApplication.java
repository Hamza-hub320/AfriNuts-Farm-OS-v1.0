package com.afrinuts.farmos;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;

public class FarmApplication extends Application {

    private static final String TAG = "FarmApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed: " + e.getMessage());
        }
    }
}