package com.rsassistant.auth;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

import com.rsassistant.util.PreferenceManager;

public class OAuthCallbackActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri uri = getIntent().getData();
        if (uri != null && uri.getScheme().equals("rsassistant") && uri.getHost().equals("callback")) {
            String code = uri.getQueryParameter("code");
            String token = uri.getQueryParameter("access_token");
            String error = uri.getQueryParameter("error");

            PreferenceManager prefs = new PreferenceManager(this);

            if (error != null) {
                prefs.setLoggedIn(false);
            } else if (token != null) {
                prefs.setAccessToken(token);
                prefs.setLoggedIn(true);
            } else if (code != null) {
                // Exchange code for token
                prefs.setAccessToken(code);
                prefs.setLoggedIn(true);
            }
        }

        finish();
    }
}
