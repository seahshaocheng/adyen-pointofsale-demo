package com.example.adyenpointofsale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ScannerActivity extends AppCompatActivity {
    private static final String TAG = "MLKit Barcode";
    private static final int PERMISSION_CODE = 1001;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private PreviewView previewView;
    private CameraSelector cameraSelector;
    private ProcessCameraProvider cameraProvider;
    private Preview previewUseCase;
    private ImageAnalysis analysisUseCase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        previewView = findViewById(R.id.previewView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCamera();
    }

    public void startCamera() {
        if(ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            setupCamera();
        } else {
            getPermissions();
        }
    }

    private void getPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{CAMERA_PERMISSION}, PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (requestCode == PERMISSION_CODE) {
            setupCamera();
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setupCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        int lensFacing = CameraSelector.LENS_FACING_BACK;
        cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindAllCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "cameraProviderFuture.addListener Error", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindAllCameraUseCases() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            bindPreviewUseCase();
            bindAnalysisUseCase();
        }
    }

    private void bindPreviewUseCase() {
        if (cameraProvider == null) {
            return;
        }

        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }

        Preview.Builder builder = new Preview.Builder();
        builder.setTargetRotation(getRotation());

        previewUseCase = builder.build();
        previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());

        try {
            cameraProvider
                    .bindToLifecycle(this, cameraSelector, previewUseCase);
        } catch (Exception e) {
            Log.e(TAG, "Error when bind preview", e);
        }
    }

    private void bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return;
        }

        if (analysisUseCase != null) {
            cameraProvider.unbind(analysisUseCase);
        }

        Executor cameraExecutor = Executors.newSingleThreadExecutor();

        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        builder.setTargetRotation(getRotation());

        analysisUseCase = builder.build();
        analysisUseCase.setAnalyzer(cameraExecutor, this::analyze);

        try {
            cameraProvider
                    .bindToLifecycle(this, cameraSelector, analysisUseCase);
        } catch (Exception e) {
            Log.e(TAG, "Error when bind analysis", e);
        }
    }

    protected int getRotation() throws NullPointerException {
        return previewView.getDisplay().getRotation();
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyze(@NonNull ImageProxy image) {
        if (image.getImage() == null) return;

        InputImage inputImage = InputImage.fromMediaImage(
                image.getImage(),
                image.getImageInfo().getRotationDegrees()
        );

        BarcodeScanner barcodeScanner = BarcodeScanning.getClient();

        barcodeScanner.process(inputImage)
                .addOnSuccessListener(this::onSuccessListener)
                .addOnFailureListener(e -> Log.e(TAG, "Barcode process failure", e))
                .addOnCompleteListener(task -> image.close());
    }

    private static boolean isStringValidJson(String str){
        Gson gson = new Gson();
        try {
            gson.fromJson(str, Object.class);
            return true;
        } catch(com.google.gson.JsonSyntaxException exp) {
            return false;
        }
    }

    private void onSuccessListener(List<Barcode> barcodes) {
        if (barcodes.size() > 0) {
            Bundle extras = getIntent().getExtras();
           try {
               String value = extras.getString("source");
               Log.i("type",value);
               Gson gson = new Gson();
                String barCodeRawData = barcodes.get(0).getDisplayValue();

                if (isStringValidJson(barCodeRawData)) {
                    switch (value) {
                        case "configuration":
                            ConfigurationData barcodeData = gson.fromJson(barCodeRawData, ConfigurationData.class);
                            Log.i("type","config");

                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            SharedPreferences.Editor editor = prefs.edit();
                            //Saving Company Account
                            editor.putString("company_account",barcodeData.company_account);
                            editor.apply();

                            //Saving Merchant Account
                            editor.putString("merchant_account",barcodeData.merchant_account);
                            editor.apply();

                            //Saving API Key
                            editor.putString("api_key",barcodeData.apiKey);
                            editor.apply();

                            //Saving POS ID
                            editor.putString("pos_id",barcodeData.POSID);
                            editor.apply();

                            //Saving Currency
                            editor.putString("tender_currency",barcodeData.currency);
                            editor.apply();

                            //Saving reference prefix
                            editor.putString("reference_prefix",barcodeData.reference_prefix);
                            editor.apply();

                            //Saving Cryto Value
                            editor.putString("crypto_version",barcodeData.localCryptoVersion);
                            editor.apply();

                            //Saving local key_identifier
                            editor.putString("key_identifier",barcodeData.localKeyIdentifier);
                            editor.apply();

                            //Saving local passphrase value
                            editor.putString("key_phrase",barcodeData.localKeyPhrase);
                            editor.apply();

                            //saving key version value
                            editor.putString("key_version", barcodeData.localkeyVersion);
                            editor.apply();

                            //Saving Local IP Address
                            editor.putString("localIP", barcodeData.localIpAddress);
                            editor.apply();

                            //Saving Paired Terminal
                            editor.putString("pairedTerminal",barcodeData.pairedTerminal);
                            editor.apply();

                            Toast.makeText(this, "Successfully loaded configuration:"+barcodeData.company_account, Toast.LENGTH_SHORT)
                                    .show();
                            Intent i = new Intent(ScannerActivity.this, DemoConfiguration.class);
                            startActivity(i);
                            break;
                        default:
                            Toast.makeText(this, barcodes.get(0).getDisplayValue(), Toast.LENGTH_SHORT)
                                    .show();
                    }
                }
            }catch(Exception e){
               e.printStackTrace();
           }
        }
    }
}