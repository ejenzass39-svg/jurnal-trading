package com.jurnal.trading;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> chooser;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setTextZoom(100);
        s.setUseWideViewPort(false);
        web.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("http".equals(u.getScheme()) || "https".equals(u.getScheme())) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) { toast("Tidak bisa membuka link"); }
                    return true;
                }
                return false;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView v, String url, String msg, final JsResult r) {
                new AlertDialog.Builder(MainActivity.this).setMessage(msg)
                        .setPositiveButton(android.R.string.ok, (d, w) -> r.confirm())
                        .setOnCancelListener(d -> r.cancel()).show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView v, String url, String msg, final JsResult r) {
                new AlertDialog.Builder(MainActivity.this).setMessage(msg)
                        .setPositiveButton(android.R.string.ok, (d, w) -> r.confirm())
                        .setNegativeButton(android.R.string.cancel, (d, w) -> r.cancel())
                        .setOnCancelListener(d -> r.cancel()).show();
                return true;
            }

            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (chooser != null) chooser.onReceiveValue(null);
                chooser = cb;
                try {
                    startActivityForResult(p.createIntent(), 1);
                } catch (Exception e) {
                    chooser = null;
                    return false;
                }
                return true;
            }
        });
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == 1 && chooser != null) {
            chooser.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            chooser = null;
        } else {
            super.onActivityResult(req, res, data);
        }
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("(function(){var d=document.getElementById('dlg');if(d&&d.open){d.close();return 1}if(V!='dash'){V='dash';render();return 1}return 0})()",
                val -> { if (!"1".equals(val)) finish(); });
    }

    private void toast(final String m) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, m, Toast.LENGTH_LONG).show());
    }

    /** Dipanggil dari JavaScript: ekspor JSON/CSV ke folder Download. */
    private class Bridge {
        @JavascriptInterface
        public void fetchNews() {
            new Thread(() -> {
                String err = "gagal";
                String[] urls = {"https://nfs.faireconomy.media/ff_calendar_thisweek.json", "https://cdn-nfs.faireconomy.media/ff_calendar_thisweek.json"};
                for (String u : urls) {
                    try {
                        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
                        c.setConnectTimeout(10000);
                        c.setReadTimeout(15000);
                        c.setRequestProperty("User-Agent", "Mozilla/5.0");
                        int code = c.getResponseCode();
                        if (code != 200) { err = "HTTP " + code; continue; }
                        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String l;
                        while ((l = br.readLine()) != null) sb.append(l);
                        br.close();
                        final String ok = "newsDone(" + JSONObject.quote(sb.toString()) + ")";
                        runOnUiThread(() -> web.evaluateJavascript(ok, null));
                        return;
                    } catch (Exception e) { err = String.valueOf(e.getMessage()); }
                }
                final String bad = "newsFail(" + JSONObject.quote(err) + ")";
                runOnUiThread(() -> web.evaluateJavascript(bad, null));
            }).start();
        }

        @JavascriptInterface
        public String loadData() {
            return getSharedPreferences("jurnal", MODE_PRIVATE).getString("data", "");
        }

        @JavascriptInterface
        public void saveData(String json) {
            getSharedPreferences("jurnal", MODE_PRIVATE).edit().putString("data", json).apply();
        }

        @JavascriptInterface
        public void saveFile(String name, String content) {
            try {
                String safe = name.replaceAll("[^A-Za-z0-9._-]", "_");
                String mime = safe.endsWith(".csv") ? "text/csv" : "application/json";
                byte[] data = content.getBytes(StandardCharsets.UTF_8);
                OutputStream os;
                String where;
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.Downloads.DISPLAY_NAME, safe);
                    v.put(MediaStore.Downloads.MIME_TYPE, mime);
                    v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    os = getContentResolver().openOutputStream(u);
                    where = "folder Download";
                } else {
                    File f = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), safe);
                    os = new FileOutputStream(f);
                    where = f.getParent();
                }
                os.write(data);
                os.close();
                toast("Tersimpan di " + where + ": " + safe);
            } catch (Exception e) {
                toast("Gagal menyimpan: " + e.getMessage());
            }
        }
    }
}
