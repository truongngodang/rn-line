
package com.grapesgreenslab.rnline;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.linecorp.linesdk.LineAccessToken;
import com.linecorp.linesdk.LineApiResponse;
import com.linecorp.linesdk.LineFriendshipStatus;
import com.linecorp.linesdk.LineIdToken;
import com.linecorp.linesdk.LineProfile;
import com.linecorp.linesdk.Scope;
import com.linecorp.linesdk.api.LineApiClient;
import com.linecorp.linesdk.api.LineApiClientBuilder;
import com.linecorp.linesdk.auth.LineAuthenticationParams;
import com.linecorp.linesdk.auth.LineLoginApi;
import com.linecorp.linesdk.auth.LineLoginResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

public class RNLine extends ReactContextBaseJavaModule {

  private static final int REQUEST_CODE = 1;
  private Promise currentPromise;
  private static final String ERROR = "ERROR";

  private ReactApplicationContext reactContext;

  private String lineChannelID;

  private LineApiClient lineApiClient;

  RNLine(ReactApplicationContext reactContext) {
    super(reactContext);
    this.lineChannelID = reactContext.getString(R.string.line_channel_id);
    this.reactContext = reactContext;
    this.reactContext.addActivityEventListener(mActivityEventListener);
  }

  @Nonnull
  @Override
  public String getName() {
    return "RNLine";
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
            WritableMap response = Arguments.createMap();
            response.putString("action", "LOGIN");
            response.putString("code", loginResult.getResponseCode().name());
            response.putMap("data", parseLoginResult(loginResult));
            promise.resolve(response);
            break;
          case CANCEL:
            promise.reject(ERROR, "The request was canceled.");
            break;
          case AUTHENTICATION_AGENT_ERROR:
            promise.reject(ERROR, "An authentication agent error occurred.");
            break;
          case SERVER_ERROR:
            promise.reject(ERROR, "A server error occurred.");
          case INTERNAL_ERROR:
            promise.reject(ERROR, "An internal error occurred.");
          case NETWORK_ERROR:
            promise.reject(ERROR, "A network error occurred.");
          default:
            promise.reject(ERROR, loginResult.getErrorData().getMessage());
            break;
        }
      }
    }
  };

  @ReactMethod
  public void login(ReadableArray scopes, String botPrompt, final Promise promise) {
    Log.d("scopes", scopes.toString());
    ArrayList<Object> mScopes = scopes.toArrayList();
    List<Scope> scopeList = new ArrayList<Scope>();
    for (Object scope:mScopes) {
      if (scope.toString().equals("PROFILE")) {
        scopeList.add(Scope.PROFILE);
      }
      if (scope.toString().equals("OPENID")) {
        scopeList.add(Scope.OPENID_CONNECT);
      }
      if (scope.toString().equals("EMAIL")) {
        scopeList.add(Scope.OC_EMAIL);
      }
      if (scope.toString().equals("MESSAGE")) {
        scopeList.add(Scope.MESSAGE);
      }
      if (scope.toString().equals("BIRTHDATE")) {
        scopeList.add(Scope.OC_BIRTHDATE);
      }
      if (scope.toString().equals("REAL_NAME")) {
        scopeList.add(Scope.OC_REAL_NAME);
      }
      if (scope.toString().equals("FRIEND")) {
        scopeList.add(Scope.FRIEND);
      }
      if (scope.toString().equals("GROUP")) {
        scopeList.add(Scope.GROUP);
      }
      if (scope.toString().equals("ADDRESS")) {
        scopeList.add(Scope.OC_ADDRESS);
      }
      if (scope.toString().equals("GENDER")) {
        scopeList.add(Scope.OC_GENDER);
      }
      if (scope.toString().equals("PHONE_NUMBER")) {
        scopeList.add(Scope.OC_PHONE_NUMBER);
      }
    }
    Log.d("scopes2", scopeList.toString());
    try {
      currentPromise = promise;
      LineAuthenticationParams.BotPrompt mPrompt = null;
      if (botPrompt.equals("normal") || botPrompt.equals("aggressive")) {
        mPrompt = LineAuthenticationParams.BotPrompt.valueOf(botPrompt);
      }
      LineAuthenticationParams params = new LineAuthenticationParams.Builder().scopes(scopeList).botPrompt(mPrompt).build();
      Intent loginIntent = LineLoginApi.getLoginIntent(
              this.reactContext,
              this.lineChannelID,
              params);
      Objects.requireNonNull(getCurrentActivity()).startActivityForResult(loginIntent, REQUEST_CODE);
    } catch (Exception e) {
      promise.reject("ERROR", e.toString());
    }
  }

  @ReactMethod
  public void getAccessToken(Promise promise) {
    LineApiResponse<LineAccessToken> lineApiResponse = getLineApiClient().getCurrentAccessToken();
    if (lineApiResponse.isSuccess()) {
      WritableMap response = Arguments.createMap();
      response.putString("action", "GET_ACCESS_TOKEN");
      response.putString("code", lineApiResponse.getResponseCode().name());
      response.putMap("data", parseAccessToken(lineApiResponse.getResponseData()));
      promise.resolve(response);
    } else {
      promise.reject(ERROR, lineApiResponse.getErrorData().getMessage());
    }
  }

  @ReactMethod
  public void getUserProfile(Promise promise) {
    LineApiResponse<LineProfile> lineApiResponse = getLineApiClient().getProfile();
    if (lineApiResponse.isSuccess()) {
      WritableMap response = Arguments.createMap();
      response.putString("action", "GET_USER_PROFILE");
      response.putString("code", lineApiResponse.getResponseCode().name());
      response.putMap("data", parseProfile(lineApiResponse.getResponseData()));
      promise.resolve(response);
    } else {
      promise.reject(ERROR, lineApiResponse.getErrorData().getMessage());
    }
  }

  @ReactMethod
  public void getFriendshipStatus(Promise promise) {
    LineApiResponse<LineFriendshipStatus> lineApiResponse = getLineApiClient().getFriendshipStatus();
    if (lineApiResponse.isSuccess()) {
      WritableMap response = Arguments.createMap();
      response.putString("action", "GET_FRIENDSHIP_STATUS");
      response.putString("code", lineApiResponse.getResponseCode().name());
      response.putBoolean("isFriend", lineApiResponse.getResponseData().isFriend());
      promise.resolve(response);
    } else {
      promise.reject(ERROR, lineApiResponse.getErrorData().getMessage());
    }
  }

  // Logout
  @ReactMethod
  public void logout(Promise promise) {
    LineApiResponse lineApiResponse = getLineApiClient().logout();
    if (lineApiResponse.isSuccess()) {
      WritableMap response = Arguments.createMap();
      response.putString("action", "LOGOUT");
      response.putString("code", lineApiResponse.getResponseCode().name());
      response.putNull("data");
      promise.resolve(response);
    } else {
      promise.reject(ERROR, lineApiResponse.getErrorData().getMessage());
    }
  }

  private LineApiClient getLineApiClient() {
    if (lineApiClient == null) {
      lineApiClient = new LineApiClientBuilder(this.reactContext, this.lineChannelID).build();
    }
    return lineApiClient;
  }

  private WritableMap parseLoginResult(LineLoginResult loginResult) {
    LineIdToken lineIdToken = loginResult.getLineIdToken();
    WritableMap result = Arguments.createMap();
    result.putMap("profile", parseProfile(Objects.requireNonNull(loginResult.getLineProfile())));
    result.putBoolean("friendshipStatusChanged", loginResult.getFriendshipStatusChanged());
    result.putMap("accessToken", parseAccessToken(Objects.requireNonNull(loginResult.getLineCredential()).getAccessToken()));
    if (lineIdToken != null) {
      result.putMap("extraInfo", parseExtraInfo(lineIdToken));
    } else {
      result.putNull("extraInfo");
    }
    return result;
  }

  private WritableMap parseExtraInfo(LineIdToken lineIdToken) {
    WritableMap result = Arguments.createMap();
    result.putString("email", lineIdToken.getEmail());
    result.putString("audience", lineIdToken.getAudience());
    result.putString("birthDate", lineIdToken.getBirthdate());
    result.putString("familyName", lineIdToken.getFamilyName());
    result.putString("familyNamePronunciation", lineIdToken.getFamilyNamePronunciation());
    result.putString("gender", lineIdToken.getGender());
    result.putString("givenName", lineIdToken.getGivenName());
    result.putString("givenNamePronunciation", lineIdToken.getGivenNamePronunciation());
    result.putString("issuer", lineIdToken.getIssuer());
    result.putString("middleName", lineIdToken.getMiddleName());
    result.putString("name", lineIdToken.getName());
    result.putString("nonce", lineIdToken.getNonce());
    result.putString("phoneNumber", lineIdToken.getPhoneNumber());
    result.putString("picture", lineIdToken.getPicture());
    result.putString("subject", lineIdToken.getSubject());
    result.putString("address", lineIdToken.getAddress() == null ? null : lineIdToken.getAddress().toString());
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