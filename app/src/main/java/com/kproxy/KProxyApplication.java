package com.kproxy;

import android.app.Application;

import java.io.DataOutputStream;
import java.io.File;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KProxyApplication extends Application {

    private static KProxyApplication instance;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Random random = new Random();

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initializeAntiBanOnStart();
        clearPreviousTraces();
    }

    public static KProxyApplication getInstance() {
        return instance;
    }

    private void initializeAntiBanOnStart() {
        executor.execute(() -> {
            try {
                Process process = Runtime.getRuntime().exec("sh");
                DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
                
                StringBuilder script = new StringBuilder();
                script.append("# K.Proxy Anti-Ban Initialization\n");
                
                String[] models = {"Pixel 5", "Pixel 6", "Galaxy S21", "OnePlus 9", "Xiaomi 11"};
                String[] manufacturers = {"Google", "Samsung", "OnePlus", "Xiaomi", "Huawei"};
                
                String randomModel = models[random.nextInt(models.length)];
                String randomManufacturer = manufacturers[random.nextInt(manufacturers.length)];
                
                script.append("setprop ro.product.model \"").append(randomModel).append("\"\n");
                script.append("setprop ro.product.manufacturer \"").append(randomManufacturer).append("\"\n");
                
                script.append("logcat -c\n");
                script.append("dmesg -c > /dev/null 2>&1\n");
                script.append("rm -rf /data/local/tmp/.kproxy_* 2>/dev/null\n");
                script.append("sync\n");
                script.append("exit\n");
                
                outputStream.writeBytes(script.toString());
                outputStream.flush();
                process.waitFor();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void clearPreviousTraces() {
        executor.execute(() -> {
            try {
                File traceDir = new File("/data/local/tmp/");
                if (traceDir.exists() && traceDir.isDirectory()) {
                    File[] files = traceDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            String name = file.getName();
                            if (name.startsWith(".kproxy_") || name.startsWith(".speed_") ||
                                name.startsWith(".jump_") || name.startsWith(".sens_") ||
                                name.startsWith(".switch_") || name.startsWith(".hitbox_") ||
                                name.startsWith(".headshot_") || name.startsWith(".gold_")) {
                                file.delete();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}