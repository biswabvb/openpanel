package com.openpanel.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private static final int OVERLAY_PERMISSION_REQ_CODE = 2002;
    private static final int STORAGE_PERMISSION_REQ_CODE = 2003;

    // Navigation & Tabs
    private LinearLayout tabHome;
    private LinearLayout tabSettings;
    private TextView tvTabHomeLabel;
    private TextView tvTabSettingsLabel;
    private View layoutHomeTab;
    private View layoutSettingsTab;

    // Home UI Elements
    private Button btnHomeInject;
    private TextView tvHomeStatus;
    private TextView tvTargetGameInfo;
    private TextView tvTopStatus;

    // Settings UI Elements
    private TextView tvSetupStatusText;
    private Button btnInstallConfig;
    private Button btnRemoveConfig;
    private TextView tvShizukuStatus;
    private TextView tvOverlayStatus;
    private TextView tvStorageStatus;
    private Button btnCheckPermissions;

    private String targetGamePackage;

    private final Shizuku.OnRequestPermissionResultListener shizukuPermissionListener =
            new Shizuku.OnRequestPermissionResultListener() {
                @Override
                public void onRequestPermissionResult(int requestCode, int grantResult) {
                    runOnUiThread(() -> {
                        if (grantResult == PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(MainActivity.this, "Shizuku Permission Granted!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Shizuku Permission Denied", Toast.LENGTH_SHORT).show();
                        }
                        updatePermissionsAndStatus();
                    });
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        targetGamePackage = ConfigManager.detectInstalledGame(this);

        initViews();
        setupListeners();

        try {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener);
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionsAndStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener);
        } catch (Throwable ignored) {
        }
    }

    private void initViews() {
        tabHome = findViewById(R.id.tabHome);
        tabSettings = findViewById(R.id.tabSettings);
        tvTabHomeLabel = findViewById(R.id.tvTabHomeLabel);
        tvTabSettingsLabel = findViewById(R.id.tvTabSettingsLabel);
        layoutHomeTab = findViewById(R.id.layoutHomeTab);
        layoutSettingsTab = findViewById(R.id.layoutSettingsTab);

        btnHomeInject = findViewById(R.id.btnHomeInject);
        tvHomeStatus = findViewById(R.id.tvHomeStatus);
        tvTargetGameInfo = findViewById(R.id.tvTargetGameInfo);
        tvTopStatus = findViewById(R.id.tvTopStatus);

        tvSetupStatusText = findViewById(R.id.tvSetupStatusText);
        btnInstallConfig = findViewById(R.id.btnInstallConfig);
        btnRemoveConfig = findViewById(R.id.btnRemoveConfig);
        tvShizukuStatus = findViewById(R.id.tvShizukuStatus);
        tvOverlayStatus = findViewById(R.id.tvOverlayStatus);
        tvStorageStatus = findViewById(R.id.tvStorageStatus);
        btnCheckPermissions = findViewById(R.id.btnCheckPermissions);

        String gameName = ConfigManager.getGameDisplayName(targetGamePackage);
        tvTargetGameInfo.setText("Target: " + gameName + " (" + targetGamePackage + ")");
    }

    private void setupListeners() {
        // Tab switching
        tabHome.setOnClickListener(v -> switchTab(true));
        tabSettings.setOnClickListener(v -> switchTab(false));

        // Inject / Launch Button
        btnHomeInject.setOnClickListener(v -> onInjectClicked());

        // Install Config
        btnInstallConfig.setOnClickListener(v -> {
            if (!checkAndPromptPermissions()) return;
            Toast.makeText(this, "Installing config...", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                boolean success = ConfigManager.installConfig(MainActivity.this, targetGamePackage);
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(MainActivity.this, "Config Installed! Status: ONLINE", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Config installation failed", Toast.LENGTH_SHORT).show();
                    }
                    updateConfigStatus();
                });
            }).start();
        });

        // Remove Config
        btnRemoveConfig.setOnClickListener(v -> {
            if (!checkAndPromptPermissions()) return;
            Toast.makeText(this, "Removing config...", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                boolean success = ConfigManager.removeConfig(targetGamePackage);
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(MainActivity.this, "Config Removed! Status: OFFLINE", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to remove config", Toast.LENGTH_SHORT).show();
                    }
                    updateConfigStatus();
                });
            }).start();
        });

        btnCheckPermissions.setOnClickListener(v -> checkAndPromptPermissions());
    }

    private void switchTab(boolean showHome) {
        if (showHome) {
            layoutHomeTab.setVisibility(View.VISIBLE);
            layoutSettingsTab.setVisibility(View.GONE);
            tabHome.setBackgroundResource(R.drawable.pill_tab_active);
            tabSettings.setBackground(null);
            tvTabHomeLabel.setTextColor(ContextCompat.getColor(this, R.color.white));
            tvTabSettingsLabel.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        } else {
            layoutHomeTab.setVisibility(View.GONE);
            layoutSettingsTab.setVisibility(View.VISIBLE);
            tabSettings.setBackgroundResource(R.drawable.pill_tab_active);
            tabHome.setBackground(null);
            tvTabSettingsLabel.setTextColor(ContextCompat.getColor(this, R.color.white));
            tvTabHomeLabel.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void onInjectClicked() {
        if (!checkAndPromptPermissions()) {
            return;
        }

        // Start Floating HUD Service
        Intent overlayIntent = new Intent(this, FloatingOverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(overlayIntent);
        } else {
            startService(overlayIntent);
        }

        Toast.makeText(this, "Floating HUD Started! Launching Game...", Toast.LENGTH_SHORT).show();

        // Launch target game
        launchTargetGame();
    }

    private void launchTargetGame() {
        PackageManager pm = getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(targetGamePackage);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchIntent);
        } else {
            Toast.makeText(this, "Game (" + targetGamePackage + ") is not installed!", Toast.LENGTH_LONG).show();
        }
    }

    private boolean checkAndPromptPermissions() {
        // 1. Overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog();
            return false;
        }

        // 2. Storage permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showStoragePermissionDialog();
                return false;
            }
        }

        // 3. Shizuku availability & permission
        if (!ShizukuHelper.isShizukuAvailable()) {
            showShizukuNotRunningDialog();
            return false;
        }

        if (!ShizukuHelper.hasShizukuPermission()) {
            showShizukuPermissionDialog();
            return false;
        }

        return true;
    }

    private void updatePermissionsAndStatus() {
        boolean overlayOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
        boolean storageOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
        boolean shizukuRunning = ShizukuHelper.isShizukuAvailable();
        boolean shizukuPerm = ShizukuHelper.hasShizukuPermission();

        tvOverlayStatus.setText(overlayOk ? "• Floating Overlay: GRANTED" : "• Floating Overlay: NOT GRANTED");
        tvOverlayStatus.setTextColor(ContextCompat.getColor(this, overlayOk ? R.color.status_online : R.color.status_offline));

        tvStorageStatus.setText(storageOk ? "• All Files Access: GRANTED" : "• All Files Access: NOT GRANTED");
        tvStorageStatus.setTextColor(ContextCompat.getColor(this, storageOk ? R.color.status_online : R.color.status_offline));

        String shizukuText = "• Shizuku: ";
        if (!shizukuRunning) {
            shizukuText += "SERVICE NOT RUNNING";
            tvShizukuStatus.setTextColor(ContextCompat.getColor(this, R.color.status_offline));
        } else if (!shizukuPerm) {
            shizukuText += "PERMISSION NEEDED";
            tvShizukuStatus.setTextColor(ContextCompat.getColor(this, R.color.status_warning));
        } else {
            shizukuText += "CONNECTED & AUTHORIZED";
            tvShizukuStatus.setTextColor(ContextCompat.getColor(this, R.color.status_online));
        }
        tvShizukuStatus.setText(shizukuText);

        updateConfigStatus();
    }

    private void updateConfigStatus() {
        new Thread(() -> {
            boolean installed = false;
            if (ShizukuHelper.hasShizukuPermission()) {
                installed = ConfigManager.isConfigInstalled(targetGamePackage);
            }
            final boolean isInstalled = installed;
            runOnUiThread(() -> {
                if (isInstalled) {
                    tvSetupStatusText.setText(R.string.status_installed);
                    tvSetupStatusText.setBackgroundResource(R.drawable.status_pill_online);
                    tvSetupStatusText.setTextColor(ContextCompat.getColor(this, R.color.status_online));

                    tvTopStatus.setText("CONFIG ACTIVE");
                    tvTopStatus.setTextColor(ContextCompat.getColor(this, R.color.status_online));
                } else {
                    tvSetupStatusText.setText(R.string.status_setup_needed);
                    tvSetupStatusText.setBackgroundResource(R.drawable.status_pill_offline);
                    tvSetupStatusText.setTextColor(ContextCompat.getColor(this, R.color.status_offline));

                    tvTopStatus.setText("READY");
                    tvTopStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                }
            });
        }).start();
    }

    private void showOverlayPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.overlay_permission_title)
                .setMessage(R.string.overlay_permission_msg)
                .setPositiveButton(R.string.btn_grant, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void showStoragePermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.storage_permission_title)
                .setMessage(R.string.storage_permission_msg)
                .setPositiveButton(R.string.btn_grant, (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, STORAGE_PERMISSION_REQ_CODE);
                        } catch (Exception e) {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                            startActivityForResult(intent, STORAGE_PERMISSION_REQ_CODE);
                        }
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void showShizukuNotRunningDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.shizuku_not_running_title)
                .setMessage(R.string.shizuku_not_running_msg)
                .setPositiveButton(R.string.btn_open_shizuku, (dialog, which) -> {
                    PackageManager pm = getPackageManager();
                    Intent intent = pm.getLaunchIntentForPackage("moe.shizuku.privileged.api");
                    if (intent != null) {
                        startActivity(intent);
                    } else {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=moe.shizuku.privileged.api")));
                        } catch (Exception e) {
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")));
                        }
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void showShizukuPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.shizuku_permission_title)
                .setMessage(R.string.shizuku_permission_msg)
                .setPositiveButton(R.string.btn_grant, (dialog, which) -> {
                    ShizukuHelper.requestPermission(ShizukuHelper.SHIZUKU_PERMISSION_REQUEST_CODE);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}
