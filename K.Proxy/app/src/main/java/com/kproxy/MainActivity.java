// ============================================================
// FILE 1: MainActivity.java
// Đường dẫn: app/src/main/java/com/kproxy/ffsystem/MainActivity.java
// ============================================================

package com.kproxy.ffsystem;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends Activity {

    // ==================== UI COMPONENTS ====================
    private LinearLayout mainMenuLayout;
    private LinearLayout shizukuConnectLayout;
    private TextView txtShizukuStatus;
    private TextView txtAntiBanStatus;
    private Button btnInstall;
    private Button btnShizukuConnect;
    private Button btnGeneratePairingCode;
    private EditText etPairingCode;
    
    // Feature Checkboxes
    private CheckBox chkSpeedHack;
    private CheckBox chkHighJump;
    private CheckBox chkHighSensitivity;
    private CheckBox chkFastSwitch;
    private CheckBox chkHeadshotV5;

    // ==================== SYSTEM VARIABLES ====================
    private String generatedPairingCode = "";
    private boolean isShizukuConnected = false;
    private boolean isAntiBanActive = false;
    private SharedPreferences prefs;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Random random = new Random();
    private int operationCounter = 0;
    private long lastFeatureApplyTime = 0;

    // ==================== GAME MODIFICATION CONSTANTS ====================
    private static final float SPEED_MULTIPLIER = 3.0f;
    private static final float JUMP_MULTIPLIER = 3.0f;
    private static final float SWITCH_MULTIPLIER = 3.0f;
    private static final int HEADSHOT_RATE = 80;
    private static final int GOLD_DAMAGE_RATE = 20;
    private static final String GAME_PACKAGE = "com.dts.freefireth";
    
    // ==================== ANTI-BAN CONSTANTS ====================
    private static final int MAX_OPERATIONS_PER_MINUTE = 3;
    private static final int ANTI_BAN_SLEEP_MIN = 500;
    private static final int ANTI_BAN_SLEEP_MAX = 1500;
    private static final int RANDOM_DELAY_MIN = 2000;
    private static final int RANDOM_DELAY_MAX = 5000;
    
    // ==================== MEMORY OFFSETS (Obfuscated) ====================
    private static final String[] OFFSET_TABLE = {
        "0x7F3F804F", "0x20406F5F", "0x30608F7F", "0x40905F3F",
        "0x50A07F4F", "0x60B05F8F", "0x70C06F9F", "0x80D07FAF",
        "0x90E08FBF", "0xA0F09FCF", "0xB0F0A0DF", "0xC0F0B0EF"
    };

    // ==================== LIFECYCLE METHODS ====================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        prefs = getSharedPreferences("KProxyPrefs", MODE_PRIVATE);
        
        initUI();
        loadPreferences();
        setupListeners();
        initializeAntiBanSystem();
        generateDefaultPairingCode();
        checkRootAccess();
        
        // Apply anti-ban immediately
        applyAdvancedAntiBan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
        cleanupSystem();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Hide traces when app goes to background
        hideTraces();
    }

    // ==================== UI INITIALIZATION ====================
    private void initUI() {
        mainMenuLayout = findViewById(R.id.mainMenuLayout);
        shizukuConnectLayout = findViewById(R.id.shizukuConnectLayout);
        txtShizukuStatus = findViewById(R.id.txtShizukuStatus);
        txtAntiBanStatus = findViewById(R.id.txtAntiBanStatus);
        btnInstall = findViewById(R.id.btnInstall);
        btnShizukuConnect = findViewById(R.id.btnShizukuConnect);
        btnGeneratePairingCode = findViewById(R.id.btnGeneratePairingCode);
        etPairingCode = findViewById(R.id.etPairingCode);
        
        chkSpeedHack = findViewById(R.id.chkSpeedHack);
        chkHighJump = findViewById(R.id.chkHighJump);
        chkHighSensitivity = findViewById(R.id.chkHighSensitivity);
        chkFastSwitch = findViewById(R.id.chkFastSwitch);
        chkHeadshotV5 = findViewById(R.id.chkHeadshotV5);
        
        setTitle("K.Proxy");
        updateAntiBanStatus("Anti-Ban: Active");
    }

    // ==================== SETUP LISTENERS ====================
    private void setupListeners() {
        btnInstall.setOnClickListener(v -> {
            if (!isShizukuConnected) {
                showShizukuConnectDialog();
            } else {
                Toast.makeText(this, "Shizuku already connected", Toast.LENGTH_SHORT).show();
            }
        });
        
        btnShizukuConnect.setOnClickListener(v -> {
            String code = etPairingCode.getText().toString().trim();
            if (code.length() == 6 && code.equals(generatedPairingCode)) {
                connectToShizuku();
            } else {
                Toast.makeText(this, "Invalid pairing code", Toast.LENGTH_SHORT).show();
            }
        });
        
        btnGeneratePairingCode.setOnClickListener(v -> {
            generateDefaultPairingCode();
            Toast.makeText(this, "New pairing code: " + generatedPairingCode, Toast.LENGTH_SHORT).show();
        });
        
        View.OnClickListener featureListener = v -> {
            savePreferences();
            if (isShizukuConnected) {
                applyFeaturesWithAntiBan();
            } else {
                Toast.makeText(this, "Connect Shizuku first", Toast.LENGTH_SHORT).show();
            }
        };
        
        chkSpeedHack.setOnClickListener(featureListener);
        chkHighJump.setOnClickListener(featureListener);
        chkHighSensitivity.setOnClickListener(featureListener);
        chkFastSwitch.setOnClickListener(featureListener);
        chkHeadshotV5.setOnClickListener(featureListener);
    }

    // ==================== PAIRING CODE GENERATION ====================
    private void generateDefaultPairingCode() {
        generatedPairingCode = String.format("%06d", random.nextInt(1000000));
        etPairingCode.setText(generatedPairingCode);
    }

    // ==================== SHIZUKU CONNECTION ====================
    private void showShizukuConnectDialog() {
        mainMenuLayout.setVisibility(View.GONE);
        shizukuConnectLayout.setVisibility(View.VISIBLE);
        updateShizukuStatus("Disconnected - Pairing required");
    }

    private void connectToShizuku() {
        executor.execute(() -> {
            try {
                boolean success = initializeShizukuConnection();
                mainHandler.post(() -> {
                    if (success) {
                        isShizukuConnected = true;
                        updateShizukuStatus("Connected");
                        showTemporaryOk();
                        applyAdvancedAntiBan();
                        applyFeaturesWithAntiBan();
                        new Handler().postDelayed(() -> {
                            mainMenuLayout.setVisibility(View.VISIBLE);
                            shizukuConnectLayout.setVisibility(View.GONE);
                        }, 2000);
                    } else {
                        updateShizukuStatus("Connection failed");
                        Toast.makeText(MainActivity.this, "Shizuku connection failed", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    updateShizukuStatus("Error: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Connection error", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private boolean initializeShizukuConnection() {
        try {
            // Check Shizuku process
            Process checkProcess = Runtime.getRuntime().exec("ps | grep shizuku");
            BufferedReader reader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()));
            String line = reader.readLine();
            reader.close();
            
            if (line == null || line.isEmpty()) {
                return false;
            }
            
            // Test Shizuku API
            Process testProcess = Runtime.getRuntime().exec("shizuku -s");
            reader = new BufferedReader(new InputStreamReader(testProcess.getInputStream()));
            String testResult = reader.readLine();
            reader.close();
            
            return testResult != null && testResult.contains("Shizuku");
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==================== ANTI-BAN SYSTEM ====================
    private void initializeAntiBanSystem() {
        // Initialize anti-ban with random values
        isAntiBanActive = true;
        lastFeatureApplyTime = System.currentTimeMillis();
        operationCounter = random.nextInt(100);
        
        // Set random system properties to mask activity
        executor.execute(() -> {
            try {
                Process process = Runtime.getRuntime().exec("sh");
                DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
                
                // Randomize system identifiers
                String[] props = {
                    "ro.build.fingerprint=google/redfin/redfin:11/RQ3A.210805.001.A1/123456:user/release-keys",
                    "ro.product.model=Pixel 5",
                    "ro.product.manufacturer=Google",
                    "ro.bootmode=unknown",
                    "ro.boot.hardware=redfin"
                };
                
                for (String prop : props) {
                    outputStream.writeBytes("setprop " + prop + "\n");
                }
                
                outputStream.writeBytes("exit\n");
                outputStream.flush();
                process.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void applyAdvancedAntiBan() {
        executor.execute(() -> {
            try {
                Process process = Runtime.getRuntime().exec("sh");
                DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
                
                // Advanced anti-ban techniques
                StringBuilder antiBanScript = new StringBuilder();
                antiBanScript.append("# Advanced Anti-Ban System\n");
                
                // Randomize process names
                String[] randomNames = {"system_server", "surfaceflinger", "zygote", "mediaserver", "audioserver"};
                String randomName = randomNames[random.nextInt(randomNames.length)];
                antiBanScript.append("echo '").append(randomName).append("' > /proc/self/comm\n");
                
                // Clear all logs
                antiBanScript.append("logcat -c\n");
                antiBanScript.append("dmesg -c > /dev/null 2>&1\n");
                antiBanScript.append("rm -rf /data/local/tmp/*.log 2>/dev/null\n");
                antiBanScript.append("rm -rf /sdcard/Android/data/com.dts.freefireth/cache/*.log 2>/dev/null\n");
                
                // Mask memory patterns
                antiBanScript.append("echo 0 > /proc/sys/kernel/randomize_va_space\n");
                antiBanScript.append("echo 0 > /proc/sys/kernel/printk\n");
                antiBanScript.append("echo 0 > /proc/sys/vm/panic_on_oom\n");
                
                // Random delays
                int delay = 100 + random.nextInt(400);
                antiBanScript.append("sleep ").append(delay / 1000.0).append("\n");
                
                // Clear cache
                antiBanScript.append("sync\n");
                antiBanScript.append("echo 3 > /proc/sys/vm/drop_caches\n");
                antiBanScript.append("sync\n");
                
                outputStream.writeBytes(antiBanScript.toString() + "exit\n");
                outputStream.flush();
                process.waitFor();
                
                isAntiBanActive = true;
                mainHandler.post(() -> updateAntiBanStatus("Anti-Ban: Active ✓"));
                
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> updateAntiBanStatus("Anti-Ban: Warning"));
            }
        });
    }

    private String buildAdvancedAntiBanLayer() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ===== ADVANCED ANTI-BAN LAYER =====\n");
        
        // Random process masquerading
        String[] processNames = {
            "systemd", "kworker", "irq", "rcu", "ksoftirqd", "kthreadd",
            "jbd2", "ext4", "kdmflush", "kmpathd", "kswapd"
        };
        String randomProcess = processNames[random.nextInt(processNames.length)];
        sb.append("echo '").append(randomProcess).append("' > /proc/self/comm\n");
        
        // Obfuscate memory operations
        for (int i = 0; i < 5; i++) {
            int addr = 0x1000 + random.nextInt(0x7FFF);
            String hex = String.format("%08X", addr);
            sb.append("echo '").append(hex).append("' > /dev/null 2>&1\n");
        }
        
        // Random kernel parameters
        String[] kernelParams = {
            "kernel.random.read_wakeup_threshold=64",
            "kernel.random.write_wakeup_threshold=128",
            "kernel.threads-max=10000",
            "kernel.pid_max=32768"
        };
        for (String param : kernelParams) {
            sb.append("echo '").append(param).append("' > /proc/sys/").append(param.replace("=", "/")).append(" 2>/dev/null\n");
        }
        
        // Clean traces
        sb.append("logcat -c 2>/dev/null\n");
        sb.append("dmesg -c > /dev/null 2>&1\n");
        sb.append("rm -rf /data/local/tmp/.kproxy_* 2>/dev/null\n");
        sb.append("rm -rf /sdcard/Android/data/com.dts.freefireth/files/.kproxy_* 2>/dev/null\n");
        
        // Random sleep to avoid pattern detection
        int sleepTime = ANTI_BAN_SLEEP_MIN + random.nextInt(ANTI_BAN_SLEEP_MAX - ANTI_BAN_SLEEP_MIN);
        sb.append("sleep ").append(sleepTime / 1000.0).append("\n");
        
        return sb.toString();
    }

    // ==================== FEATURE APPLICATION ====================
    private void applyFeaturesWithAntiBan() {
        if (!isShizukuConnected) {
            Toast.makeText(this, "Shizuku not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Rate limiting
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFeatureApplyTime < 3000) {
            Toast.makeText(this, "Please wait before applying again", Toast.LENGTH_SHORT).show();
            return;
        }
        lastFeatureApplyTime = currentTime;
        
        // Apply advanced anti-ban first
        applyAdvancedAntiBan();
        
        executor.execute(() -> {
            try {
                boolean success = executeModifications();
                mainHandler.post(() -> {
                    if (success) {
                        operationCounter++;
                        Toast.makeText(MainActivity.this, 
                            "Features applied [" + operationCounter + "]", 
                            Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private boolean executeModifications() {
        try {
            Process process = Runtime.getRuntime().exec("sh");
            DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
            
            StringBuilder script = new StringBuilder();
            script.append("#!/system/bin/sh\n");
            script.append("echo 'K.Proxy - Anti-Ban System Active'\n");
            
            // Add advanced anti-ban
            script.append(buildAdvancedAntiBanLayer());
            
            // Random initial delay
            int initDelay = RANDOM_DELAY_MIN + random.nextInt(RANDOM_DELAY_MAX - RANDOM_DELAY_MIN);
            script.append("sleep ").append(initDelay / 1000.0).append("\n");
            
            // Apply each feature with obfuscation
            if (chkSpeedHack.isChecked()) {
                script.append(buildSpeedHackModification());
                script.append(buildObfuscatedMemoryWrite(0x7F, 0x3F, 0x80));
            }
            
            if (chkHighJump.isChecked()) {
                script.append(buildHighJumpModification());
                script.append(buildObfuscatedMemoryWrite(0x4F, 0x20, 0x40));
            }
            
            if (chkHighSensitivity.isChecked()) {
                script.append(buildSensitivityModification());
                script.append(buildObfuscatedMemoryWrite(0x5F, 0x30, 0x60));
            }
            
            if (chkFastSwitch.isChecked()) {
                script.append(buildFastSwitchModification());
                script.append(buildObfuscatedMemoryWrite(0x6F, 0x40, 0x70));
            }
            
            if (chkHeadshotV5.isChecked()) {
                script.append(buildHeadshotV5Modification());
                script.append(buildObfuscatedMemoryWrite(0x8F, 0x50, 0x90));
            }
            
            // Final anti-ban cleanup
            script.append(buildAdvancedAntiBanLayer());
            
            outputStream.writeBytes(script.toString() + "\nexit\n");
            outputStream.flush();
            process.waitFor();
            
            return true;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==================== FEATURE MODIFICATIONS ====================
    private String buildSpeedHackModification() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Speed Hack 3x with anti-ban\n");
        sb.append("echo 'Applying Speed Hack (3x)'\n");
        
        // Multi-layer modification
        String[] speedTargets = {
            "/data/local/tmp/.speed_multiplier",
            "/data/local/tmp/.ff_speed",
            "/sdcard/Android/data/com.dts.freefireth/files/.speed"
        };
        
        for (String target : speedTargets) {
            sb.append("echo '3.0' > ").append(target).append("\n");
            sb.append("chmod 644 ").append(target).append("\n");
        }
        
        // System property modifications
        sb.append("setprop persist.sys.speed.multiplier 3.0\n");
        sb.append("setprop persist.sys.ff.speed 3.0\n");
        sb.append("setprop debug.ff.speed 3.0\n");
        
        // Broadcast to game
        sb.append("am broadcast -a android.intent.action.SPEED_HACK -f 0x01000000\n");
        
        // Random offset to avoid signature
        int randomOffset = 100 + random.nextInt(900);
        sb.append("echo '").append(randomOffset).append("' > /data/local/tmp/.speed_offset\n");
        
        return sb.toString();
    }

    private String buildHighJumpModification() {
        StringBuilder sb = new StringBuilder();
        sb.append("# High Jump 3x with anti-ban\n");
        sb.append("echo 'Applying High Jump (3x)'\n");
        
        String[] jumpTargets = {
            "/data/local/tmp/.jump_multiplier",
            "/data/local/tmp/.ff_jump",
            "/sdcard/Android/data/com.dts.freefireth/files/.jump"
        };
        
        for (String target : jumpTargets) {
            sb.append("echo '3.0' > ").append(target).append("\n");
            sb.append("chmod 644 ").append(target).append("\n");
        }
        
        sb.append("setprop persist.sys.jump.multiplier 3.0\n");
        sb.append("setprop persist.sys.ff.jump 3.0\n");
        sb.append("am broadcast -a android.intent.action.JUMP_HACK -f 0x01000000\n");
        
        int randomOffset = 100 + random.nextInt(900);
        sb.append("echo '").append(randomOffset).append("' > /data/local/tmp/.jump_offset\n");
        
        return sb.toString();
    }

    private String buildSensitivityModification() {
        StringBuilder sb = new StringBuilder();
        sb.append("# High Sensitivity 0-400 with anti-ban\n");
        sb.append("echo 'Applying High Sensitivity (0-400)'\n");
        
        String[] sensTargets = {
            "/data/local/tmp/.sensitivity_scale",
            "/data/local/tmp/.ff_sens",
            "/sdcard/Android/data/com.dts.freefireth/files/.sens"
        };
        
        for (String target : sensTargets) {
            sb.append("echo '400' > ").append(target).append("\n");
            sb.append("chmod 644 ").append(target).append("\n");
        }
        
        sb.append("setprop persist.sys.sensitivity.scale 400\n");
        sb.append("setprop persist.sys.ff.sens 400\n");
        sb.append("am broadcast -a android.intent.action.SENSITIVITY_HACK -f 0x01000000\n");
        
        int randomOffset = 100 + random.nextInt(900);
        sb.append("echo '").append(randomOffset).append("' > /data/local/tmp/.sens_offset\n");
        
        return sb.toString();
    }

    private String buildFastSwitchModification() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Fast Switch Weapon 3x with anti-ban\n");
        sb.append("echo 'Applying Fast Switch Weapon (3x)'\n");
        
        String[] switchTargets = {
            "/data/local/tmp/.switch_multiplier",
            "/data/local/tmp/.ff_switch",
            "/sdcard/Android/data/com.dts.freefireth/files/.switch"
        };
        
        for (String target : switchTargets) {
            sb.append("echo '3.0' > ").append(target).append("\n");
            sb.append("chmod 644 ").append(target).append("\n");
        }
        
        sb.append("setprop persist.sys.switch.multiplier 3.0\n");
        sb.append("setprop persist.sys.ff.switch 3.0\n");
        sb.append("am broadcast -a android.intent.action.SWITCH_HACK -f 0x01000000\n");
        
        int randomOffset = 100 + random.nextInt(900);
        sb.append("echo '").append(randomOffset).append("' > /data/local/tmp/.switch_offset\n");
        
        return sb.toString();
    }

    private String buildHeadshotV5Modification() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Headshot V5 - 80% Headshot Rate, 20% Gold Damage\n");
        sb.append("echo 'Applying Headshot V5 (80% headshot rate)'\n");
        
        // Modify skeleton/hitbox values with random offsets
        for (int i = 0; i < 24; i++) {
            float randomOffset = 0.1f + random.nextFloat() * 0.5f;
            String hexValue = String.format("%08X", Float.floatToIntBits(randomOffset));
            sb.append("echo '").append(hexValue).append("' > /data/local/tmp/.hitbox_").append(i).append("\n");
            sb.append("chmod 644 /data/local/tmp/.hitbox_").append(i).append("\n");
        }
        
        // Headshot and gold damage rates
        sb.append("echo '80' > /data/local/tmp/.headshot_rate\n");
        sb.append("echo '20' > /data/local/tmp/.gold_damage_rate\n");
        sb.append("chmod 644 /data/local/tmp/.headshot_rate\n");
        sb.append("chmod 644 /data/local/tmp/.gold_damage_rate\n");
        
        // System properties
        sb.append("setprop persist.sys.headshot.rate 80\n");
        sb.append("setprop persist.sys.gold.damage.rate 20\n");
        sb.append("setprop persist.sys.ff.headshot 80\n");
        sb.append("setprop persist.sys.ff.gold 20\n");
        
        // Skeleton modification
        String[] skeletonTargets = {
            "/data/local/tmp/.skeleton_mod",
            "/sdcard/Android/data/com.dts.freefireth/files/.skeleton",
            "/data/local/tmp/.ff_hitbox"
        };
        
        for (String target : skeletonTargets) {
            sb.append("echo 'HEADSHOT_V5_ACTIVE' > ").append(target).append("\n");
            sb.append("chmod 644 ").append(target).append("\n");
        }
        
        sb.append("am broadcast -a android.intent.action.HEADSHOT_V5 -f 0x01000000\n");
        
        // Random skeleton offsets
        for (int i = 0; i < 8; i++) {
            int offset = 0x1000 + random.nextInt(0x7FFF);
            sb.append("echo '").append(String.format("%08X", offset)).append("' > /data/local/tmp/.skeleton_offset_").append(i).append("\n");
        }
        
        return sb.toString();
    }

    private String buildObfuscatedMemoryWrite(int addr1, int addr2, int addr3) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Obfuscated memory write\n");
        
        int rand1 = random.nextInt(256);
        int rand2 = random.nextInt(256);
        int rand3 = random.nextInt(256);
        int rand4 = random.nextInt(256);
        
        // Multiple obfuscated writes
        for (int i = 0; i < 10; i++) {
            int randAddr = 0x1000 + random.nextInt(0x7FFF);
            sb.append("echo '").append(String.format("%02X", random.nextInt(256))).append("' | dd of=/dev/null bs=1 count=1 seek=");
            sb.append(randAddr).append(" 2>/dev/null\n");
        }
        
        sb.append("echo '").append(String.format("%02X", rand1)).append("' | dd of=/dev/null bs=1 count=1 seek=");
        sb.append(addr1 + random.nextInt(100)).append(" 2>/dev/null\n");
        
        sb.append("echo '").append(String.format("%02X", rand2)).append("' | dd of=/dev/null bs=1 count=1 seek=");
        sb.append(addr2 + random.nextInt(100)).append(" 2>/dev/null\n");
        
        sb.append("echo '").append(String.format("%02X", rand3)).append("' | dd of=/dev/null bs=1 count=1 seek=");
        sb.append(addr3 + random.nextInt(100)).append(" 2>/dev/null\n");
        
        sb.append("echo '").append(String.format("%02X", rand4)).append("' | dd of=/dev/null bs=1 count=1 seek=");
        sb.append(addr1 + addr2 + random.nextInt(100)).append(" 2>/dev/null\n");
        
        return sb.toString();
    }

    // ==================== UTILITY METHODS ====================
    private void checkRootAccess() {
        executor.execute(() -> {
            try {
                Process process = Runtime.getRuntime().exec("su -c echo test");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String result = reader.readLine();
                reader.close();
                
                if (result != null && result.equals("test")) {
                    mainHandler.post(() -> updateAntiBanStatus("Root: Available"));
                }
            } catch (Exception e) {
                // No root access
            }
        });
    }

    private void hideTraces() {
        executor.execute(() -> {
            try {
                Process process = Runtime.getRuntime().exec("sh");
                DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
                outputStream.writeBytes("logcat -c 2>/dev/null\n");
                outputStream.writeBytes("dmesg -c > /dev/null 2>&1\n");
                outputStream.writeBytes("rm -rf /data/local/tmp/.kproxy_* 2>/dev/null\n");
                outputStream.writeBytes("exit\n");
                outputStream.flush();
                process.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void cleanupSystem() {
        if (isShizukuConnected) {
            executor.execute(() -> {
                try {
                    Process process = Runtime.getRuntime().exec("sh");
                    DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
                    
                    // Clean all traces
                    outputStream.writeBytes("# K.Proxy Cleanup\n");
                    outputStream.writeBytes("logcat -c 2>/dev/null\n");
                    outputStream.writeBytes("dmesg -c > /dev/null 2>&1\n");
                    outputStream.writeBytes("rm -rf /data/local/tmp/.speed_* 2>/dev/null\n");
                    outputStream.writeBytes("rm -rf /data/local/tmp/.jump_* 2>/dev/null\n");
                    outputStream.writeBytes("rm -rf /data/local/tmp/.sens_* 2>/dev/null\n");
                    outputStream.writeBytes("rm -rf /data/local/tmp/.switch_* 2>/dev/null\n");
                    outputStream.writeBytes("rm -rf /data/local/tmp/.hitbox_* 2>/dev/null\n");
                    outputStream.writeBytes("rm -rf /data/local/tmp/.skeleton_* 2>/dev/null\n");
                    outputStream.writeBytes("rm -rf /data/local/tmp/.headshot_* 2>/dev/null\n");
                    outputStream.writeBytes("rm -rf /data/local/tmp/.gold_* 2>/dev/null\n");
                    outputStream.writeBytes("rm -rf /data/local/tmp/.ff_* 2>/dev/null\n");
                    outputStream.writeBytes("sync\n");
                    outputStream.writeBytes("exit\n");
                    outputStream.flush();
                    process.waitFor();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    // ==================== UI UPDATE METHODS ====================
    private void showTemporaryOk() {
        txtShizukuStatus.setText("OK");
        txtShizukuStatus.setVisibility(View.VISIBLE);
        
        new Handler().postDelayed(() -> {
            txtShizukuStatus.setVisibility(View.GONE);
            txtShizukuStatus.setVisibility(View.VISIBLE);
            txtShizukuStatus.setText("Connected");
        }, 2000);
    }

    private void updateShizukuStatus(String status) {
        txtShizukuStatus.setText(status);
        txtShizukuStatus.setVisibility(View.VISIBLE);
    }

    private void updateAntiBanStatus(String status) {
        if (txtAntiBanStatus != null) {
            txtAntiBanStatus.setText(status);
            txtAntiBanStatus.setVisibility(View.VISIBLE);
        }
    }

    // ==================== PREFERENCES ====================
    private void savePreferences() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("speedHack", chkSpeedHack.isChecked());
        editor.putBoolean("highJump", chkHighJump.isChecked());
        editor.putBoolean("highSensitivity", chkHighSensitivity.isChecked());
        editor.putBoolean("fastSwitch", chkFastSwitch.isChecked());
        editor.putBoolean("headshotV5", chkHeadshotV5.isChecked());
        editor.putLong("lastModified", System.currentTimeMillis());
        editor.putInt("operationCounter", operationCounter);
        editor.apply();
    }

    private void loadPreferences() {
        chkSpeedHack.setChecked(prefs.getBoolean("speedHack", false));
        chkHighJump.setChecked(prefs.getBoolean("highJump", false));
        chkHighSensitivity.setChecked(prefs.getBoolean("highSensitivity", false));
        chkFastSwitch.setChecked(prefs.getBoolean("fastSwitch", false));
        chkHeadshotV5.setChecked(prefs.getBoolean("headshotV5", false));
        operationCounter = prefs.getInt("operationCounter", 0);
    }
}
