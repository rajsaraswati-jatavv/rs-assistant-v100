package com.rsassistant;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 200;

    private PreviewView previewView;
    private TextView gestureText, faceText;
    private ImageButton btnFlash, btnCapture, btnSwitch;

    private Camera camera;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private FaceDetector faceDetector;
    private boolean flashEnabled = false;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        initViews();
        initFaceDetector();
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (checkCameraPermission()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }
    }

    private void initViews() {
        previewView = findViewById(R.id.previewView);
        gestureText = findViewById(R.id.gestureText);
        faceText = findViewById(R.id.faceText);
        btnFlash = findViewById(R.id.btnFlash);
        btnCapture = findViewById(R.id.btnCapture);
        btnSwitch = findViewById(R.id.btnSwitch);

        btnFlash.setOnClickListener(v -> toggleFlash());
        btnCapture.setOnClickListener(v -> capturePhoto());
        btnSwitch.setOnClickListener(v -> switchCamera());
    }

    private void initFaceDetector() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .enableTracking()
                .build();

        faceDetector = FaceDetection.getClient(options);
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startCamera() {
        ProcessCameraProvider.getInstance(this).addListener(() -> {
            try {
                cameraProvider = ProcessCameraProvider.getInstance(this).get();
                bindCameraUseCases();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to start camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;
        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private void analyzeImage(ImageProxy imageProxy) {
        try {
            InputImage inputImage = InputImage.fromMediaImage(
                    imageProxy.getImage(),
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            Task<List<Face>> result = faceDetector.process(inputImage)
                    .addOnSuccessListener(faces -> {
                        runOnUiThread(() -> updateFaceInfo(faces));
                    })
                    .addOnFailureListener(e -> {
                        // Handle failure
                    })
                    .addOnCompleteListener(task -> imageProxy.close());

        } catch (Exception e) {
            imageProxy.close();
        }
    }

    private void updateFaceInfo(List<Face> faces) {
        if (faces.isEmpty()) {
            faceText.setText("No face detected");
            gestureText.setText("");
            return;
        }

        Face face = faces.get(0);

        StringBuilder info = new StringBuilder();
        info.append("Face detected");

        // Check smile
        Float smileProb = face.getSmilingProbability();
        if (smileProb != null && smileProb > 0.7f) {
            info.append(" | Smiling 😊");
        }

        // Check eyes
        Float leftEyeOpen = face.getLeftEyeOpenProbability();
        Float rightEyeOpen = face.getRightEyeOpenProbability();

        if (leftEyeOpen != null && rightEyeOpen != null) {
            if (leftEyeOpen < 0.2f && rightEyeOpen < 0.2f) {
                info.append(" | Eyes closed");
            } else if (leftEyeOpen < 0.3f) {
                info.append(" | Left wink 😉");
            } else if (rightEyeOpen < 0.3f) {
                info.append(" | Right wink 😉");
            }
        }

        // Check head orientation
        Float headEulerAngleY = face.getHeadEulerAngleY();
        if (headEulerAngleY != null) {
            if (headEulerAngleY > 30) {
                info.append(" | Looking right →");
            } else if (headEulerAngleY < -30) {
                info.append(" | Looking left ←");
            }
        }

        Float headEulerAngleX = face.getHeadEulerAngleX();
        if (headEulerAngleX != null) {
            if (headEulerAngleX > 20) {
                info.append(" | Looking down ↓");
            } else if (headEulerAngleX < -20) {
                info.append(" | Looking up ↑");
            }
        }

        faceText.setText(info.toString());
        gestureText.setText("Faces: " + faces.size());
    }

    private void toggleFlash() {
        if (camera != null) {
            flashEnabled = !flashEnabled;
            camera.getCameraControl().enableTorch(flashEnabled);
            Toast.makeText(this, flashEnabled ? "Flash ON" : "Flash OFF", Toast.LENGTH_SHORT).show();
        }
    }

    private void capturePhoto() {
        Toast.makeText(this, "Photo captured!", Toast.LENGTH_SHORT).show();
        // Implement photo capture
    }

    private void switchCamera() {
        lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK)
                ? CameraSelector.LENS_FACING_FRONT
                : CameraSelector.LENS_FACING_BACK;
        bindCameraUseCases();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (faceDetector != null) {
            faceDetector.close();
        }
    }
}
