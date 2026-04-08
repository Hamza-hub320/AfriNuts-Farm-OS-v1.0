package com.afrinuts.farmos.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.afrinuts.farmos.MainActivity;
import com.afrinuts.farmos.ui.auth.LoginActivity;
import com.afrinuts.farmos.utils.SessionManager;

public abstract class BaseActivity extends AppCompatActivity {

    protected SessionManager sessionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        // Check role-specific access
        if (!checkRoleAccess()) {
            String message = getAccessDeniedMessage();
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            navigateToMain();
        }
    }

    protected abstract boolean checkRoleAccess();

    protected String getAccessDeniedMessage() {
        return "You don't have permission to access this section";
    }

    protected void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}