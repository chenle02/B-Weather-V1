package com.chenboda01.bweatherv1;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView webView;

    public class AndroidBridge {
        @JavascriptInterface
        public void openApp(String pkg, String cls, String label) {
            runOnUiThread(() -> {
                try {
                    PackageManager pm = getPackageManager();
                    Intent launch = pm.getLaunchIntentForPackage(pkg);
                    if (launch == null && cls != null && cls.length() > 0) {
                        launch = new Intent(Intent.ACTION_MAIN);
                        launch.addCategory(Intent.CATEGORY_LAUNCHER);
                        launch.setClassName(pkg, cls);
                    }
                    if (launch != null) {
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(launch);
                    } else {
                        Toast.makeText(MainActivity.this, label + " is not installed yet.", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Could not open " + label + ".", Toast.LENGTH_LONG).show();
                }
            });
        }

        @JavascriptInterface
        public void requestLocation() {
            runOnUiThread(() -> {
                try {
                    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 42);
                        return;
                    }
                    LocationManager lm = (LocationManager)getSystemService(LOCATION_SERVICE);
                    Location loc = null;
                    if (lm != null) {
                        loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                    if (loc != null) sendLocation(loc);
                    else if (lm != null) {
                        lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, new LocationListener() {
                            public void onLocationChanged(Location location) { sendLocation(location); }
                            public void onStatusChanged(String p, int s, android.os.Bundle b) {}
                            public void onProviderEnabled(String p) {}
                            public void onProviderDisabled(String p) {}
                        }, null);
                    } else Toast.makeText(MainActivity.this, "Location unavailable.", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Could not get location.", Toast.LENGTH_LONG).show();
                }
            });
        }
        private void sendLocation(Location loc) {
            String js = "window.bweatherUseLocation && window.bweatherUseLocation(" + loc.getLatitude() + "," + loc.getLongitude() + ")";
            webView.evaluateJavascript(js, null);
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
        webView.loadUrl("file:///android_asset/index.html");
    }

    public void onBackPressed() {
        webView.evaluateJavascript("window.bweatherBack && window.bweatherBack()", null);
    }
}
