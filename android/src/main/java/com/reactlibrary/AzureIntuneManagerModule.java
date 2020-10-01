package com.reactlibrary;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableArray;


import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.IClaimable;


import android.app.Activity;
import android.util.Log;
import android.R;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;



public class AzureIntuneManagerModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private static IMultipleAccountPublicClientApplication msalClient = null;
    private IAccount msalAccount = null;

    public AzureIntuneManagerModule(@Nonnull ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "AzureIntuneManager";
    }

    @ReactMethod
    public void init(String clientID, Promise promise) {
        try {
            getMSALApplication(clientID);
            promise.resolve("init werkt");
        } catch(Exception e) {
            promise.reject("init werkt niet", e);
        }
    }

    @ReactMethod
    public void acquireTokenAsync(String clientID, ReadableArray scopes, final Promise promise) {
        final Activity activity = getCurrentActivity();
        try {
            String scope = scopes.getString(0);
            String[] arr = {scope};
            if(msalClient != null) {
                msalClient.acquireToken(activity, arr, getAuthenticationCallback(promise));
            }
        } catch(Exception e) {
            promise.reject(e);
        }
    }
    
    @ReactMethod
    public void acquireTokenAsyncSilent(String userId, ReadableArray scopes, final Promise promise) {
        try {
            String scope = scopes.getString(0);
            String[] arr = {scope};
            msalAccount = msalClient.getAccount(userId);
            if(msalAccount != null && msalClient != null) {
                String authority = msalClient.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
                Log.w("MSAL token", "Silent executed");
                msalClient.acquireTokenSilentAsync(arr, msalAccount, authority, handleSilentResult(promise));
            }
        } catch(Exception e) {
            Log.w("MSAL token", "Silent executed");
            promise.reject(e);
        }
    }

    private AuthenticationCallback getAuthenticationCallback(final Promise promise) {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                promise.resolve(resultToDict(authenticationResult));
            }
            @Override
            public void onError(MsalException exception) {
                if (exception instanceof MsalClientException) {
                    promise.reject(exception);
                    //And exception from the client (MSAL)
                } else if (exception instanceof MsalServiceException) {
                    //An exception from the server
                      promise.reject(exception);
                }
            }
            @Override
            public void onCancel() {
                /* User canceled the authentication */
            }
        };
    }

    private SilentAuthenticationCallback handleSilentResult(final Promise promise) {
        return new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                promise.resolve(resultToDict(authenticationResult));
            }
            @Override
            public void onError(MsalException exception) {
                Log.w("MSAL silent error", "Error");
                if (exception instanceof MsalClientException) {
                    promise.reject(exception);
                } else if (exception instanceof MsalServiceException) {
                    promise.reject(exception);
                }
                Log.w("MSAL acquireTokenAsync", exception);
            }
        };
    }

    private void getMSALApplication(String clientID) throws IOException {
        try {
            InputStream stream = reactContext.getAssets().open("msal_config.json");
            File CONFIG = parseJSONIntoFile(stream);
            PublicClientApplication.createMultipleAccountPublicClientApplication(getReactApplicationContext(),
            CONFIG,
            new IPublicClientApplication.IMultipleAccountApplicationCreatedListener() {
                @Override
                public void onCreated(IMultipleAccountPublicClientApplication msalClientResponse) {
                    msalClient = msalClientResponse;
                    return;
                }
                @Override
                public void onError(MsalException exception) {
                    Log.w("MSAL exception onError", exception);
                }
            });
        } catch(Exception e) {
            Log.w("getMSALApplication catch", e);
        }
    }

    private WritableMap resultToDict(IAuthenticationResult authResult) {
        WritableMap map = new WritableNativeMap();
        map.putString("accessToken", authResult.getAccessToken());
        map.putString("idToken", authResult.getAccount().getIdToken());
        map.putString("userId", authResult.getAccount().getId());
        map.putString("expiresOn", String.format("%s", authResult.getExpiresOn().getTime()));
        map.putMap("userInfo", userToDict(authResult.getAccount(), authResult.getTenantId()));
        return map;
    }

    private WritableMap userToDict(IAccount account, String tenantId) {
        WritableMap map = new WritableNativeMap();
        map.putString("userIdentifier", account.getId());
        map.putString("tenant", tenantId);
        map.putString("userName", account.getUsername());
        return map;
    }


      private File parseJSONIntoFile (InputStream in) throws IOException {
        String PREFIX = "prefix";
        String SUFFIX = ".tmp";
        final File tempFile = File.createTempFile(PREFIX, SUFFIX);
        tempFile.deleteOnExit();
        FileOutputStream out = new FileOutputStream(tempFile);
        try {
            IOUtils.copy(in, out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tempFile;
    }
}

