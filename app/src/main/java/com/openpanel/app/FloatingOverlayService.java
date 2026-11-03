package com.openpanel.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.List;

public class FloatingOverlayService extends Service {

    private static final String CHANNEL_ID = "OpenPanelOverlayChannel";
    private static final int NOTIFICATION_ID = 2001;

    private WindowManager windowManager;
    private View overlayRootView;
    private WindowManager.LayoutParams params;

    private View layoutFloatingIcon;
    private View layoutFloatingMenu;
    private TextView tvStatusVal;
    private TextView tvExpiryVal;
    private TextView tvSellerVal;
    private TextView tvReasonVal;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private String targetGamePackage;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        targetGamePackage = ConfigManager.detectInstalledGame(this);
        initOverlayView();
        startStatusPolling();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Floating HUD Overlay",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ABCD PANEL HUD Active")
                .setContentText("Floating overlay is running over the game")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void initOverlayView() {
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        LayoutInflater inflater = LayoutInflater.from(this);
        overlayRootView = inflater.inflate(R.layout.floating_hud_pill, null);

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 40;
        params.y = 200;

        layoutFloatingIcon = overlayRootView.findViewById(R.id.layoutFloatingIcon);
        layoutFloatingMenu = overlayRootView.findViewById(R.id.layoutFloatingMenu);
        tvStatusVal = overlayRootView.findViewById(R.id.tvStatusVal);
        tvExpiryVal = overlayRootView.findViewById(R.id.tvExpiryVal);
        tvSellerVal = overlayRootView.findViewById(R.id.tvSellerVal);
        tvReasonVal = overlayRootView.findViewById(R.id.tvReasonVal);

        TextView tvCloseMenu = overlayRootView.findViewById(R.id.tvCloseMenu);
        Button btnHideOverlay = overlayRootView.findViewById(R.id.btnHideOverlay);

        tvCloseMenu.setOnClickListener(v -> collapseMenu());
        btnHideOverlay.setOnClickListener(v -> collapseMenu());

        setupTouchAndDrag();

        windowManager.addView(overlayRootView, params);
    }

    private void expandMenu() {
        layoutFloatingIcon.setVisibility(View.GONE);
        layoutFloatingMenu.setVisibility(View.VISIBLE);
    }

    private void collapseMenu() {
        layoutFloatingMenu.setVisibility(View.GONE);
        layoutFloatingIcon.setVisibility(View.VISIBLE);
    }

    private void setupTouchAndDrag() {
        layoutFloatingIcon.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long touchStartTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        touchStartTime = System.currentTimeMillis();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayRootView, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        long duration = System.currentTimeMillis() - touchStartTime;
                        float diffX = Math.abs(event.getRawX() - initialTouchX);
                        float diffY = Math.abs(event.getRawY() - initialTouchY);
                        // Click threshold (short duration and little displacement)
                        if (duration < 250 && diffX < 15 && diffY < 15) {
                            expandMenu();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void startStatusPolling() {
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                updateKeyStatus();
                pollHandler.postDelayed(this, 5000);
            }
        };
        pollHandler.post(pollRunnable);
    }

    private void updateKeyStatus() {
        if (!ShizukuHelper.hasShizukuPermission()) {
            tvStatusVal.setText("SHIZUKU DISCONNECTED");
            return;
        }

        String statusPath = ConfigManager.getKeyStatusPath(targetGamePackage);
        List<String> lines = ShizukuHelper.readFile(statusPath);

        if (!lines.isEmpty()) {
            for (String line : lines) {
                if (line.startsWith("STATUS:")) {
                    tvStatusVal.setText(line.substring(7).trim());
                } else if (line.startsWith("EXPIRES:")) {
                    tvExpiryVal.setText(line.substring(8).trim());
                } else if (line.startsWith("SELLER:")) {
                    tvSellerVal.setText(line.substring(7).trim());
                }
            }
        } else {
            tvStatusVal.setText("ONLINE / ACTIVE");
            tvExpiryVal.setText("PERMANENT (NO KEY)");
            tvSellerVal.setText("CLEAN OPEN SOURCE");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
        if (overlayRootView != null && windowManager != null) {
            windowManager.removeView(overlayRootView);
        }
    }
}
