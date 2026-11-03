package com.openpanel.app;

import android.content.Context;
import android.content.pm.PackageManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ConfigManager {

    public static final String[] SUPPORTED_PACKAGES = {
            "com.dts.freefiremax",
            "com.dts.freefireth",
            "com.garena.game.ffi"
    };

    public static String detectInstalledGame(Context context) {
        PackageManager pm = context.getPackageManager();
        for (String pkg : SUPPORTED_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0);
                return pkg;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        // Default to MAX if not detected
        return SUPPORTED_PACKAGES[0];
    }

    public static String getGameDisplayName(String pkg) {
        switch (pkg) {
            case "com.dts.freefiremax":
                return "Free Fire MAX";
            case "com.dts.freefireth":
                return "Free Fire Standard";
            case "com.garena.game.ffi":
                return "Free Fire (Garena)";
            default:
                return pkg;
        }
    }

    public static String getConfigFilePath(String packageName) {
        return "/storage/emulated/0/Android/data/" + packageName + "/files/localConfig.json";
    }

    public static String getKeyStatusPath(String packageName) {
        return "/storage/emulated/0/Android/data/" + packageName + "/files/key_status.txt";
    }

    public static boolean isConfigInstalled(String packageName) {
        String path = getConfigFilePath(packageName);
        ShizukuHelper.CommandResult result = ShizukuHelper.execCommandWithOutput("[ -f \"" + path + "\" ] && echo 'EXISTS'");
        return result.isSuccess() && result.stdout.contains("EXISTS");
    }

    public static boolean installConfig(Context context, String packageName) {
        try {
            // 1. Read default config from res/raw/localconfig.json
            InputStream is = context.getResources().openRawResource(R.raw.localconfig);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String configContent = new String(buffer, StandardCharsets.UTF_8);

            // 2. Write to local cache temp file
            File tempFile = new File(context.getCacheDir(), "config_tmp.json");
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(configContent.getBytes(StandardCharsets.UTF_8));
            fos.close();

            // 3. Prepare target paths
            String targetDir = "/storage/emulated/0/Android/data/" + packageName + "/files";
            String targetConfig = targetDir + "/localConfig.json";
            String targetKeyStatus = targetDir + "/key_status.txt";

            // 4. Execute privileged file copy via Shizuku
            String cmd = "mkdir -p \"" + targetDir + "\" && " +
                    "cp -f \"" + tempFile.getAbsolutePath() + "\" \"" + targetConfig + "\" && " +
                    "chmod 777 \"" + targetConfig + "\"";

            ShizukuHelper.CommandResult copyResult = ShizukuHelper.execCommandWithOutput(cmd);
            if (!copyResult.isSuccess()) {
                return false;
            }

            // 5. Write status file so HUD displays ONLINE status permanently
            String statusPayload = "STATUS: ONLINE\n" +
                    "EXPIRES: PERMANENT / UNLIMITED\n" +
                    "SELLER: OPEN SOURCE CLEAN\n" +
                    "FEATURES: AIM SILENT • LOCATION • BYPASS\n";

            File tempStatus = new File(context.getCacheDir(), "status_tmp.txt");
            FileOutputStream fosStatus = new FileOutputStream(tempStatus);
            fosStatus.write(statusPayload.getBytes(StandardCharsets.UTF_8));
            fosStatus.close();

            String statusCmd = "cp -f \"" + tempStatus.getAbsolutePath() + "\" \"" + targetKeyStatus + "\" && " +
                    "chmod 777 \"" + targetKeyStatus + "\"";
            ShizukuHelper.execCommandWithOutput(statusCmd);

            return isConfigInstalled(packageName);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean removeConfig(String packageName) {
        String targetDir = "/storage/emulated/0/Android/data/" + packageName + "/files";
        String targetConfig = targetDir + "/localConfig.json";
        String targetKeyStatus = targetDir + "/key_status.txt";

        String cmd = "rm -f \"" + targetConfig + "\" && rm -f \"" + targetKeyStatus + "\"";
        ShizukuHelper.CommandResult result = ShizukuHelper.execCommandWithOutput(cmd);
        return result.isSuccess();
    }
}
