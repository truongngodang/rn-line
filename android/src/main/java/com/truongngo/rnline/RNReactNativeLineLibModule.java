
package com.truongngo.rnline;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableMap;
import com.linecorp.linesdk.LineAccessToken;
import com.linecorp.linesdk.LineApiResponse;
import com.linecorp.linesdk.LineProfile;
import com.linecorp.linesdk.Scope;
import com.linecorp.linesdk.api.LineApiClient;
import com.linecorp.linesdk.api.LineApiClientBuilder;
import com.linecorp.linesdk.auth.LineAuthenticationParams;
import com.linecorp.linesdk.auth.LineLoginApi;
import com.linecorp.linesdk.auth.LineLoginResult;

import java.util.Arrays;
import java.util.Collections;

public class RNReactNativeLineLibModule extends ReactContextBaseJavaModule {

  private static final int REQUEST_CODE = 1;
  private LineApiClient lineApiClient;
  private Promise currentPromise;
  private static final String ERROR = "ERROR";

  private ReactApplicationContext reactContext;

  public RNReactNativeLineLibModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    this.reactContext.addActivityEventListener(mActivityEventListener);
  }

  @Override
  public String getName() {
    return "RNReactNativeLineLib";
  }

  private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
      super.onActivityResult(activity, requestCode, resultCode, data);
      if (currentPromise != null) {
        final Promise promise = currentPromise;
        currentPromise = null;
        if (requestCode != REQUEST_CODE) {
          promise.reject(ERROR, "Unsupported request");
          return;
        }
        final LineLoginResult loginResult = LineLoginApi.getLoginResultFromIntent(data);
        switch (loginResult.getResponseCode()) {
          case SUCCESS:
            promise.resolve(parseLoginResult(loginResult));
            break;
          case CANCEL:
            promise.reject(ERROR, "Line login canceled by user");
            break;
          case AUTHENTICATION_AGENT_ERROR:
            promise.reject(ERROR, "The user has denied the approval");
            break;
          default:
            promise.reject(ERROR, loginResult.getErrorData().toString());
            break;
        }
      }
    }
  };

  @ReactMethod
  public void currentAccessToken(final Promise promise) {
    currentPromise = promise;
    new GetAccessTokenTask().execute();
  }

  @ReactMethod
  public void getUserProfile(final Promise promise) {
    currentPromise = promise;
    new GetProfileTask().execute();
  }

  @SuppressLint("StaticFieldLeak")
  private class GetAccessTokenTask extends AsyncTask<Void, Void, LineApiResponse<LineAccessToken>> {
    protected LineApiResponse<LineAccessToken> doInBackground(Void... params) {
      return getLineApiClient().getCurrentAccessToken();
    }

    @Override
    protected void onPostExecute(LineApiResponse<LineAccessToken> lineApiResponse) {
      if (currentPromise != null) {
        final Promise promise = currentPromise;
        currentPromise = null;
        if (lineApiResponse.isSuccess()) {
          promise.resolve(parseAccessToken(lineApiResponse.getResponseData()));
        } else {
          promise.reject(ERROR, lineApiResponse.getErrorData().toString());
        }
      }
    }
  }

  @SuppressLint("StaticFieldLeak")
  public class GetProfileTask extends AsyncTask<Void, Void, LineApiResponse<LineProfile>> {
    protected LineApiResponse<LineProfile> doInBackground(Void... params) {
      return getLineApiClient().getProfile();
    }

    protected void onPostExecute(LineApiResponse<LineProfile> lineApiResponse) {
      if (currentPromise != null) {
        final Promise promise = currentPromise;
        currentPromise = null;
        if (lineApiResponse.isSuccess()) {
          promise.resolve(parseProfile(lineApiResponse.getResponseData()));
        } else {
          promise.reject(ERROR, lineApiResponse.getErrorData().toString());
        }
      }
    }
  }

 @ReactMethod
  public void login(final Promise promise) {
    try {
      currentPromise = promise;
      LineAuthenticationParams params = new LineAuthenticationParams.Builder()
              .scopes(Collections.singletonList(Scope.PROFILE))
              .botPrompt(LineAuthenticationParams.BotPrompt.aggressive)
              .build();
      Intent loginIntent = LineLoginApi.getLoginIntent(
              this.reactContext,
              this.reactContext.getString(R.string.line_channel_id),
              params);
      getCurrentActivity().startActivityForResult(loginIntent, REQUEST_CODE);
    } catch (Exception e) {
      promise.reject("ERROR", e.toString());
    }
  }

  @ReactMethod
  public void logout(final Promise promise) {
    currentPromise = promise;
    new LogoutTask().execute();
  }

  public class LogoutTask extends AsyncTask<Void, Void, LineApiResponse> {
    @Override
    protected LineApiResponse doInBackground(Void... voids) {
      return getLineApiClient().logout();
    }

    @Override
    protected void onPostExecute(LineApiResponse lineApiResponse) {
      if (currentPromise != null) {
        final Promise promise = currentPromise;
        currentPromise = null;
        if (lineApiResponse.isSuccess()) {
          promise.resolve("SUCCESS");
        } else {
          promise.reject(ERROR, lineApiResponse.getErrorData().toString());
        }
      }
    }
  }

  private LineApiClient getLineApiClient() {
    if (lineApiClient == null) {
      String channelId = this.reactContext.getString(R.string.line_channel_id);
      lineApiClient = new LineApiClientBuilder(this.reactContext, channelId).build();
    }
    return lineApiClient;
  }

  private WritableMap parseLoginResult(LineLoginResult loginResult) {
    WritableMap result = Arguments.createMap();
    result.putMap("profile", parseProfile(loginResult.getLineProfile()));
    result.putMap("accessToken", parseAccessToken(loginResult.getLineCredential().getAccessToken()));
    return result;
  }

  private WritableMap parseProfile(LineProfile profile) {
    WritableMap result = Arguments.createMap();
    result.putString("displayName", profile.getDisplayName());
    result.putString("userID", profile.getUserId());
    result.putString("statusMessage", profile.getStatusMessage());
    if (profile.getPictureUrl() != null) {
      result.putString("pictureURL", profile.getPictureUrl().toString());
    }
    return result;
  }

  private WritableMap parseAccessToken(LineAccessToken accessToken) {
    WritableMap result = Arguments.createMap();
    result.putString("accessToken", accessToken.getTokenString());
    result.putString("expirationDate", Long.toString(accessToken.getExpiresInMillis()));
    return result;
  }

}