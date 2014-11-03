package com.blipper.android;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

import static java.lang.String.format;

public class BlipperMainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private static final int RC_SIGN_IN = 0;
    private GoogleApiClient googleApiClient;
    private boolean intentInProgress, signInClicked;
    private ConnectionResult connectionResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blipper_main);
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(getLocalClassName(), "Connected to Google API Client");
        signInClicked = false;
        String message = format("Hi %s! Welcome to Blipper", Plus.AccountApi.getAccountName(googleApiClient));
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(getLocalClassName(), "Connection suspended to Google API Client");
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(getLocalClassName(), "Connection failed to Google API Client with error code: " + connectionResult.getErrorCode());
        if (!intentInProgress) {
            this.connectionResult = connectionResult;
            if (signInClicked) {
                resolveSignInError();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            if (resultCode != RESULT_OK) {
                signInClicked = false;
            }

            intentInProgress = false;

            if (!googleApiClient.isConnecting()) {
                googleApiClient.connect();
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.sign_in_button) {
            handleSignIn();
        }
        if (view.getId() == R.id.sign_out_button) {
            handleSignOut();
        }
    }

    private void handleSignIn() {
        Log.d(getLocalClassName(), "Sign In button clicked");
        if (!googleApiClient.isConnecting()) {
            signInClicked = true;
            resolveSignInError();
        }
    }

    private void handleSignOut() {
        Log.d(getLocalClassName(), "Sign Out button clicked");
        if (googleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(googleApiClient);
            googleApiClient.disconnect();
            googleApiClient.connect();
            Toast.makeText(this, "Good bye !", Toast.LENGTH_LONG).show();
        }
    }

    private void resolveSignInError() {
        if (connectionResult != null && connectionResult.hasResolution()) {
            try {
                intentInProgress = true;
                startIntentSenderForResult(connectionResult.getResolution().getIntentSender(),
                        RC_SIGN_IN, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                intentInProgress = false;
                googleApiClient.connect();
            }
        }
    }
}
