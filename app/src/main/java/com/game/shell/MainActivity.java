package com.game.shell;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.io.InputStream;

public class MainActivity extends Activity {
    private WebView webView;
    private PowerManager.WakeLock wakeLock;
    
    // 🚀 挂机逻辑核心变量
    private TextView floatButton;
    private boolean isForging = false;
    private final Handler pulseHandler = new Handler();
    
    // 🛡️ 12. 宿主后台主动定时心跳注入，强行驱动 WebKit 引擎 + 动态登录探测
    private final Runnable pulseRunnable = new Runnable() {
        @Override
        public void run() {
            if (webView != null) {
                // 12.1. 后台心跳维持并触发网页 autoPulse 驱动
                webView.evaluateJavascript("if (window.autoPulse) { window.autoPulse(); }", null);
                
                // 12.2. 动态检测玩家是否已登录进入主界面，以此决定是否渐显显示悬浮挂机按钮
                String checkLoginJS = "(function() {\n" +
                        "    try {\n" +
                        "        if (typeof System !== 'undefined') {\n" +
                        "            var m = System.get('chunks:///_virtual/GameServerData.ts');\n" +
                        "            if (m && m.GameServerData && m.GameServerData.getInstance()) {\n" +
                        "                var info = m.GameServerData.getInstance().fullInfo;\n" +
                        "                return info ? true : false;\n" +
                        "            }\n" +
                        "        }\n" +
                        "    } catch(e) {}\n" +
                        "    return false;\n" +
                        "})()";
                
                webView.evaluateJavascript(checkLoginJS, value -> {
                    boolean isLoggedIn = "true".equals(value);
                    updateButtonVisibility(isLoggedIn);
                });
            }
            pulseHandler.postDelayed(this, 1500); // 1.5 秒心跳
        }
    };

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 设置全屏无标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 2. 保持屏幕常亮 (前台运行时防止系统锁屏)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 3. 开启后台 CPU 唤醒锁 (防止切后台/锁屏时系统挂起 CPU)
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GameShell::WakeLockTag");
            wakeLock.acquire();
        }

        // 4. 创建根布局 FrameLayout
        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // 5. 动态创建 WebView 并加入根布局
        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rootLayout.addView(webView);

        // 6. 配置 WebSettings 优化运行性能与缓存
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true); // 开启 DOM 存储，Cocos 存档核心
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        
        // 允许混合协议 (部分 H5 游戏会从 HTTP 静态资源服务器拉图片)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // 缓存策略优化 (有本地缓存直接读本地，省电省流量)
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // 防止页面跳转时唤醒手机默认浏览器，强制在 App 内完成，刷新时清空状态
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 网页刷新重载时重置挂机按钮状态
                isForging = false;
                if (floatButton != null) {
                    floatButton.setText("钓鱼: 关");
                    floatButton.setAlpha(0.5f);
                    updateButtonBackground(false);
                }
            }
        });

        // 7. 动态构建“挂机小胶囊”悬浮按钮 (95dp x 30dp)
        floatButton = new TextView(this);
        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(
                dpToPx(this, 95), dpToPx(this, 30));
        btnParams.gravity = Gravity.TOP | Gravity.END;
        btnParams.topMargin = dpToPx(this, 60); // 避开顶部状态栏
        btnParams.rightMargin = dpToPx(this, 12);
        floatButton.setLayoutParams(btnParams);
        
        floatButton.setText("钓鱼: 关");
        floatButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        floatButton.setTypeface(null, Typeface.BOLD);
        floatButton.setTextColor(Color.WHITE);
        floatButton.setGravity(Gravity.CENTER);
        
        // 初始状态下设置为完全透明和隐藏，等登录成功后再渐显
        floatButton.setAlpha(0.0f);
        floatButton.setVisibility(View.GONE);
        updateButtonBackground(false);

        // 8. 🚀 注册拖拽与点击手势监听器，完美区分拖拽与点击，附带边缘保护防止滑出屏幕
        floatButton.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;
            private float startRawX, startRawY;
            private boolean isDragging = false;
            
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        startRawX = event.getRawX();
                        startRawY = event.getRawY();
                        isDragging = false;
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;
                        
                        float parentWidth = ((View)view.getParent()).getWidth();
                        float parentHeight = ((View)view.getParent()).getHeight();
                        float margin = dpToPx(MainActivity.this, 12);
                        float topBound = dpToPx(MainActivity.this, 60); // 避开顶部状态栏
                        
                        // 边缘判定保护
                        newX = Math.max(margin, Math.min(parentWidth - view.getWidth() - margin, newX));
                        newY = Math.max(topBound, Math.min(parentHeight - view.getHeight() - margin, newY));
                        
                        view.setX(newX);
                        view.setY(newY);
                        
                        if (Math.abs(event.getRawX() - startRawX) > 10 || Math.abs(event.getRawY() - startRawY) > 10) {
                            isDragging = true;
                        }
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                        if (!isDragging) {
                            // 确定不是拖拽后，触发点击开启/关闭挂机
                            toggleForge();
                        }
                        return true;
                }
                return false;
            }
        });

        rootLayout.addView(floatButton);
        setContentView(rootLayout);

        // 9. 载入游戏网页地址
        webView.loadUrl("http://kx.hdhive.com/");

        // 10. 启动脉冲心跳守护
        pulseHandler.postDelayed(pulseRunnable, 1500);
    }

    // 🚀 点击切换挂机状态机 (Lazy Loading / 按需载入)
    private void toggleForge() {
        if (webView == null) return;
        
        webView.evaluateJavascript("typeof window.batchSmartForge !== 'undefined'", value -> {
            boolean isAlreadyInjected = "true".equals(value);
            
            if (isForging) {
                // 状态1：正在挂机中 -> 【停止】
                isForging = false;
                webView.evaluateJavascript("window.stopBatchForge()", null);
                floatButton.setText("钓鱼: 关");
                updateButtonBackground(false);
                floatButton.animate().alpha(0.5f).setDuration(300).start();
            } else {
                // 状态2：未开启挂机 -> 【开启】
                isForging = true;
                
                if (isAlreadyInjected) {
                    executeStartForge();
                } else {
                    injectHackerJS(this::executeStartForge);
                }
            }
        });
    }

    private void executeStartForge() {
        if (webView == null) return;
        webView.evaluateJavascript("window.batchSmartForge(1)", value -> {
            floatButton.setText("钓鱼: 开 🟢");
            updateButtonBackground(true);
            floatButton.animate().alpha(0.8f).setDuration(300).start();
        });
    }

    // 从 assets 中按需提取并注入极简挂机 JS
    private void injectHackerJS(final Runnable onCompleted) {
        try {
            InputStream is = getAssets().open("hacker_init.js");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsContent = new String(buffer, "UTF-8");
            
            webView.evaluateJavascript(jsContent, value -> {
                if (onCompleted != null) {
                    onCompleted.run();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 动态淡入淡出显示/隐藏按钮
    private void updateButtonVisibility(final boolean isLoggedIn) {
        runOnUiThread(() -> {
            if (floatButton == null) return;
            
            if (isLoggedIn) {
                if (floatButton.getVisibility() != View.VISIBLE) {
                    floatButton.setVisibility(View.VISIBLE);
                    floatButton.setAlpha(0.0f);
                    floatButton.animate()
                            .alpha(isForging ? 0.8f : 0.5f)
                            .setDuration(400)
                            .start();
                }
            } else {
                if (floatButton.getVisibility() == View.VISIBLE) {
                    floatButton.animate()
                            .alpha(0.0f)
                            .setDuration(400)
                            .withEndAction(() -> floatButton.setVisibility(View.GONE))
                            .start();
                }
            }
        });
    }

    // 高级毛玻璃微边框样式自适应切换
    private void updateButtonBackground(boolean active) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.argb((int)(255 * 0.25), 0, 0, 0)); // 25% 半透明黑色
        gd.setCornerRadius(dpToPx(this, 15)); // 圆角胶囊
        
        if (active) {
            // 激活状态下：60% 半透明绿色边框
            gd.setStroke(dpToPx(this, 1), Color.argb((int)(255 * 0.6), 0, 255, 0));
        } else {
            // 待机状态下：20% 半透明白色边框
            gd.setStroke(dpToPx(this, 1), Color.argb((int)(255 * 0.2), 255, 255, 255));
        }
        floatButton.setBackground(gd);
    }

    private int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    // 10. 拦截物理返回键，使其行为为“游戏内后退”而非直接关闭 App
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // 11. 释放唤醒锁与清除定时回调，防止内存泄漏与多余的 CPU 占用
    @Override
    protected void onDestroy() {
        pulseHandler.removeCallbacks(pulseRunnable);
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
