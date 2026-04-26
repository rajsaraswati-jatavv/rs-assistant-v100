package com.rsassistant.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class RSAccessibilityService extends AccessibilityService {

    private static RSAccessibilityService instance;

    public static RSAccessibilityService getInstance() {
        return instance;
    }

    public static boolean isEnabled(Context context) {
        int enabled = Settings.Secure.getInt(
                context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0
        );
        if (enabled == 1) {
            String services = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            if (services != null) {
                return services.toLowerCase().contains(context.getPackageName().toLowerCase());
            }
        }
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Handle accessibility events if needed
    }

    @Override
    public void onInterrupt() {}

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        instance = null;
        return super.onUnbind(intent);
    }

    // === GESTURE METHODS ===

    public boolean clickAt(float x, float y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Path path = new Path();
            path.moveTo(x, y);
            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));
            return dispatchGesture(builder.build(), null, null);
        }
        return false;
    }

    public boolean swipe(float startX, float startY, float endX, float endY, long duration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Path path = new Path();
            path.moveTo(startX, startY);
            path.lineTo(endX, endY);
            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, duration));
            return dispatchGesture(builder.build(), null, null);
        }
        return false;
    }

    public boolean tap(float x, float y) {
        return clickAt(x, y);
    }

    public boolean longPress(float x, float y, long duration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Path path = new Path();
            path.moveTo(x, y);
            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, duration));
            return dispatchGesture(builder.build(), null, null);
        }
        return false;
    }

    public boolean pinch(float centerX, float centerY, float distance, boolean zoomOut) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Path path1 = new Path();
            Path path2 = new Path();

            float halfDistance = distance / 2;
            if (zoomOut) {
                path1.moveTo(centerX - halfDistance, centerY);
                path1.lineTo(centerX + halfDistance, centerY);
                path2.moveTo(centerX + halfDistance, centerY);
                path2.lineTo(centerX - halfDistance, centerY);
            } else {
                path1.moveTo(centerX + halfDistance, centerY);
                path1.lineTo(centerX - halfDistance, centerY);
                path2.moveTo(centerX - halfDistance, centerY);
                path2.lineTo(centerX + halfDistance, centerY);
            }

            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(new GestureDescription.StrokeDescription(path1, 0, 300));
            builder.addStroke(new GestureDescription.StrokeDescription(path2, 0, 300));
            return dispatchGesture(builder.build(), null, null);
        }
        return false;
    }

    // === SCROLL METHODS ===

    public boolean scrollUp() {
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        return swipe(screenWidth / 2f, screenHeight * 0.7f, screenWidth / 2f, screenHeight * 0.3f, 500);
    }

    public boolean scrollDown() {
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        return swipe(screenWidth / 2f, screenHeight * 0.3f, screenWidth / 2f, screenHeight * 0.7f, 500);
    }

    public boolean scrollLeft() {
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        return swipe(screenWidth * 0.8f, screenHeight / 2f, screenWidth * 0.2f, screenHeight / 2f, 500);
    }

    public boolean scrollRight() {
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        return swipe(screenWidth * 0.2f, screenHeight / 2f, screenWidth * 0.8f, screenHeight / 2f, 500);
    }

    // === GLOBAL ACTIONS ===

    public boolean goBack() {
        return performGlobalAction(GLOBAL_ACTION_BACK);
    }

    public boolean goHome() {
        return performGlobalAction(GLOBAL_ACTION_HOME);
    }

    public boolean openRecents() {
        return performGlobalAction(GLOBAL_ACTION_RECENTS);
    }

    public boolean openNotifications() {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
    }

    public boolean openQuickSettings() {
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
    }

    public boolean openPowerDialog() {
        return performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
    }

    public boolean lockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        }
        return false;
    }

    public boolean takeScreenshot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
        }
        return false;
    }

    // === NODE METHODS ===

    public AccessibilityNodeInfo findNodeByText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return null;
        java.util.List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        return (nodes != null && !nodes.isEmpty()) ? nodes.get(0) : null;
    }

    public AccessibilityNodeInfo findNodeById(String id) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return null;
        java.util.List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(id);
        return (nodes != null && !nodes.isEmpty()) ? nodes.get(0) : null;
    }

    public boolean clickNodeByText(String text) {
        AccessibilityNodeInfo node = findNodeByText(text);
        if (node != null) {
            return performClick(node);
        }
        return false;
    }

    public boolean clickNodeById(String id) {
        AccessibilityNodeInfo node = findNodeById(id);
        if (node != null) {
            return performClick(node);
        }
        return false;
    }

    private boolean performClick(AccessibilityNodeInfo node) {
        if (node == null) return false;

        if (node.isClickable()) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } else {
            // Find clickable parent
            AccessibilityNodeInfo parent = node.getParent();
            while (parent != null) {
                if (parent.isClickable()) {
                    return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
                parent = parent.getParent();
            }
        }
        return false;
    }

    public boolean typeText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;

        AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focused != null) {
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        }
        return false;
    }

    public boolean scrollableForward() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;

        java.util.List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId("android:id/list");
        if (nodes != null && !nodes.isEmpty()) {
            return nodes.get(0).performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        }
        return false;
    }

    public boolean scrollableBackward() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;

        java.util.List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId("android:id/list");
        if (nodes != null && !nodes.isEmpty()) {
            return nodes.get(0).performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        }
        return false;
    }
}
