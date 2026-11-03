package com.openpanel.app;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.IBinder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import rikka.shizuku.Shizuku;

public class ShizukuHelper {

    public static final int SHIZUKU_PERMISSION_REQUEST_CODE = 1001;

    public static boolean isShizukuAvailable() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean hasShizukuPermission() {
        if (!isShizukuAvailable()) {
            return false;
        }
        try {
            if (Shizuku.getVersion() < 11) {
                return false;
            }
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void requestPermission(int requestCode) {
        if (isShizukuAvailable()) {
            try {
                Shizuku.requestPermission(requestCode);
            } catch (Throwable ignored) {
            }
        }
    }

    public static CommandResult execCommandWithOutput(String command) {
        if (!hasShizukuPermission()) {
            return new CommandResult(-1, "", "Shizuku permission not granted");
        }

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        int exitCode = -1;

        try {
            Process process;
        try {
            java.lang.reflect.Method m = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
            m.setAccessible(true);
            process = (Process) m.invoke(null, new String[]{"sh", "-c", command}, null, null);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to invoke Shizuku newProcess", t);
        }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                stdout.append(line).append("\n");
            }
            reader.close();

            BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errReader.readLine()) != null) {
                stderr.append(line).append("\n");
            }
            errReader.close();

            exitCode = process.waitFor();
        } catch (Exception e) {
            stderr.append(e.getMessage());
        }

        return new CommandResult(exitCode, stdout.toString().trim(), stderr.toString().trim());
    }

    public static boolean execCommands(List<String> commands) {
        for (String cmd : commands) {
            CommandResult result = execCommandWithOutput(cmd);
            if (result.exitCode != 0) {
                return false;
            }
        }
        return true;
    }

    public static List<String> readFile(String filePath) {
        List<String> lines = new ArrayList<>();
        CommandResult result = execCommandWithOutput("cat \"" + filePath + "\"");
        if (result.exitCode == 0 && !result.stdout.isEmpty()) {
            String[] split = result.stdout.split("\n");
            for (String s : split) {
                lines.add(s.trim());
            }
        }
        return lines;
    }

    public static class CommandResult {
        public final int exitCode;
        public final String stdout;
        public final String stderr;

        public CommandResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }
}
