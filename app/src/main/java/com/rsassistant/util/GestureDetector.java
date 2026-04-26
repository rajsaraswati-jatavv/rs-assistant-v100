package com.rsassistant.util;

import android.content.Context;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

public class GestureDetector {

    public interface GestureCallback {
        void onGestureDetected(String gesture);
        void onError(String error);
    }

    private final FaceDetector faceDetector;
    private GestureCallback callback;

    public GestureDetector() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();

        faceDetector = FaceDetection.getClient(options);
    }

    public void setCallback(GestureCallback callback) {
        this.callback = callback;
    }

    public void processImage(InputImage image) {
        faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    for (Face face : faces) {
                        // Check for smile
                        Float smileProb = face.getSmilingProbability();
                        if (smileProb != null && smileProb > 0.8f) {
                            notifyGesture("smile");
                        }

                        // Check for eyes open/closed
                        Float leftEyeOpenProb = face.getLeftEyeOpenProbability();
                        Float rightEyeOpenProb = face.getRightEyeOpenProbability();

                        if (leftEyeOpenProb != null && rightEyeOpenProb != null) {
                            if (leftEyeOpenProb < 0.2f && rightEyeOpenProb < 0.2f) {
                                notifyGesture("both_eyes_closed");
                            } else if (leftEyeOpenProb < 0.2f) {
                                notifyGesture("left_wink");
                            } else if (rightEyeOpenProb < 0.2f) {
                                notifyGesture("right_wink");
                            }
                        }

                        // Check head orientation
                        Float headEulerAngleX = face.getHeadEulerAngleX();
                        Float headEulerAngleY = face.getHeadEulerAngleY();
                        Float headEulerAngleZ = face.getHeadEulerAngleZ();

                        if (headEulerAngleY != null) {
                            if (headEulerAngleY > 30) {
                                notifyGesture("head_turn_right");
                            } else if (headEulerAngleY < -30) {
                                notifyGesture("head_turn_left");
                            }
                        }

                        if (headEulerAngleX != null) {
                            if (headEulerAngleX > 20) {
                                notifyGesture("head_down");
                            } else if (headEulerAngleX < -20) {
                                notifyGesture("head_up");
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    private void notifyGesture(String gesture) {
        if (callback != null) {
            callback.onGestureDetected(gesture);
        }
    }

    public void close() {
        faceDetector.close();
    }
}
