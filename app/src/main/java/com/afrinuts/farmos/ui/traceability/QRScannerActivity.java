package com.afrinuts.farmos.ui.traceability;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.repository.BatchTraceabilityRepository;
import com.afrinuts.farmos.utils.BatchNumberUtils;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;  // ✅ Use DecoratedBarcodeView

public class QRScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 1001;

    // ✅ Change from BarcodeView to DecoratedBarcodeView
    private DecoratedBarcodeView barcodeView;
    private CaptureManager captureManager;
    private TextView tvStatus;
    private Button btnCancel;
    private ImageButton btnScannerBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        // ✅ Find the view as DecoratedBarcodeView
        barcodeView = findViewById(R.id.barcodeView);
        tvStatus = findViewById(R.id.tvStatus);
        btnCancel = findViewById(R.id.btnCancel);
        btnScannerBack = findViewById(R.id.btnScannerBack);

        btnCancel.setOnClickListener(v -> finish());
        btnScannerBack.setOnClickListener(v -> finish());

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        } else {
            startScanning();
        }
    }

    private void startScanning() {
        if (barcodeView == null) {
            Toast.makeText(this, "Scanner view not initialized", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ✅ Use DecoratedBarcodeView with CaptureManager
        captureManager = new CaptureManager(this, barcodeView);
        captureManager.initializeFromIntent(getIntent(), null);
        captureManager.decode();

        barcodeView.setStatusText("Scan QR Code");

        barcodeView.decodeSingle(result -> {
            String scanResult = result.getText();
            handleScanResult(scanResult);
        });
    }

    private void handleScanResult(String scanResult) {
        String batchNumber = BatchNumberUtils.extractBatchNumber(scanResult);

        if (batchNumber != null) {
            tvStatus.setText("Found: " + batchNumber);
            tvStatus.setTextColor(getColor(android.R.color.holo_green_light));
            checkBatchExists(batchNumber);
        } else {
            tvStatus.setText("Invalid QR Code");
            tvStatus.setTextColor(getColor(android.R.color.holo_red_light));
            Toast.makeText(this, "Invalid QR code. Please scan a valid AfriNuts traceability QR.", Toast.LENGTH_LONG).show();

            barcodeView.postDelayed(() -> {
                tvStatus.setText("Scan QR Code");
                tvStatus.setTextColor(getColor(android.R.color.white));
                if (barcodeView != null) {
                    barcodeView.resume();
                }
            }, 2000);
        }
    }

    private void checkBatchExists(String batchNumber) {
        new Thread(() -> {
            try {
                BatchTraceabilityRepository repository =
                        new BatchTraceabilityRepository(this, 1);

                boolean exists = repository.getByBatchNumber(batchNumber) != null;

                runOnUiThread(() -> {
                    if (exists) {
                        Intent intent = new Intent(this, TraceabilityDetailActivity.class);
                        intent.putExtra(TraceabilityDetailActivity.EXTRA_BATCH_NUMBER, batchNumber);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Batch not found in traceability system", Toast.LENGTH_SHORT).show();
                        barcodeView.postDelayed(() -> {
                            tvStatus.setText("Scan QR Code");
                            tvStatus.setTextColor(getColor(android.R.color.white));
                            if (barcodeView != null) {
                                barcodeView.resume();
                            }
                        }, 2000);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error checking batch: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    barcodeView.resume();
                });
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (captureManager != null) {
            captureManager.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (captureManager != null) {
            captureManager.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (captureManager != null) {
            captureManager.onDestroy();
        }
    }
}
