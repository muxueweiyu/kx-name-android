package com.game.shell;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 设置全屏无标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 2. 保持屏幕常亮 (挂机游戏核心：防止手机自动锁屏)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 3. 动态创建 WebView (避免 XML 加载开销)
        webView = new WebView(this);
        setContentView(webView);

        // 4. 配置 WebSettings 优化运行性能与缓存
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true); // 开启 DOM 存储，Cocos 存档核心
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        
        // 5. 允许混合协议 (部分 H5 游戏会从 HTTP 静态资源服务器拉图片)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // 6. 缓存策略优化 (有本地缓存直接读本地，省电省流量)
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // 7. 防止页面跳转时唤醒手机默认浏览器，强制在 App 内完成
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        // 8. 载入游戏网页地址
        webView.loadUrl("http://kx.hdhive.com/");
    }

    // 9. 拦截物理返回键，使其行为为“游戏内后退”而非直接关闭 App
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
