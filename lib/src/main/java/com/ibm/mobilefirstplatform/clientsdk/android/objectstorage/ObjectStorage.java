/*
 * IBM Confidential OCO Source Materials
 *
 * 5725-I43 Copyright IBM Corp. 2006, 2016
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *
 */
package com.ibm.mobilefirstplatform.clientsdk.android.objectstorage;

import android.content.Context;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Request;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ObjectStorage {
    public static final String METADATA_PREFIX = "X-Account-Meta-";
    public static final String AUTH_HEADER = "X-Auth-Token";

    public enum BluemixRegion {DALLAS, LONDON}

    protected static final String AUTH_URL = "https://identity.open.softlayer.com/v3/auth/tokens";

    protected static final String DALLAS_API_URL = "https://dal.objectstorage.open.softlayer.com/v1/AUTH_";
    protected static final String LONDON_API_URL = "https://lon.objectstorage.open.softlayer.com/v1/AUTH_";

    private static String authToken = null;
    private static String expiresAt = null;
    private static String projectID = null;
    private static String userID = null;
    private static String password = null;

    protected static String objectStorageURL = null;

    private static BluemixRegion region = null;
    private static Context context = null;

    protected static final Logger logger = Logger.getLogger(Logger.INTERNAL_PREFIX + ObjectStorage.class.getName());

    public static void initialize(Context applicationContext, BluemixRegion bluemixRegion){
        region = bluemixRegion;
        context = applicationContext;

        String regionString = null;

        switch(region){
            case DALLAS:
                regionString = BMSClient.REGION_US_SOUTH;
                break;
            case LONDON:
                regionString = BMSClient.REGION_UK;
                break;
        }

        BMSClient.getInstance().initialize(context, regionString);
    }

    public static void connect(final String pID, String uID, String pw, final ObjectStorageResponseListener<String> userResponseListener){
        if(uID == null || pw == null){
            logger.debug("Authentication failed because the user ID or password was null.");
            if(userResponseListener != null){
                userResponseListener.onFailure(null, new Throwable("User ID and password cannot be null."), null);
            }
        }

        projectID = pID;
        userID = uID;
        password = pw;

        if(authToken != null && objectStorageURL != null && !hasToReauthenticate()){
            logger.debug("Authentication session still valid. No authentication request occurred.");
            if(userResponseListener != null){
                userResponseListener.onSuccess(authToken);
            }
        }

        Request request = new Request(AUTH_URL, Request.POST);

        request.addHeader("Content-Type", "application/json");

        JSONObject bodyJSON = getAuthenticationRequestBody(projectID, userID, password);

        request.send(context, bodyJSON.toString(), new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                Map<String, List<String>> headers = response.getHeaders();

                if(headers != null && headers.get("X-Subject-Token") != null) {
                    authToken = headers.get("X-Subject-Token").get(0);
                }
                else{
                    logger.error("Failed to authenticate with Object Storage.");
                    if(userResponseListener != null){
                        userResponseListener.onFailure(response, new Throwable("Failed to authenticate with Object Storage."), null);
                    }
                    return;
                }

                try {
                    JSONObject responseJSON = new JSONObject(response.getResponseText());

                    JSONObject tokenJSON = responseJSON.optJSONObject("token");

                    String utcDate = null;

                    if(tokenJSON != null){
                        utcDate = tokenJSON.optString("expires_at", null);
                    }

                    if(utcDate != null){
                        expiresAt = utcDate;
                    }
                    else{
                        logger.error("Failed to authenticate with Object Storage.");
                        if(userResponseListener != null){
                            userResponseListener.onFailure(response, new Throwable("Failed to authenticate with Object Storage."), null);
                        }
                        return;
                    }
                } catch (JSONException e) {
                    logger.error("Failed to authenticate with Object Storage.");
                    if(userResponseListener != null){
                        userResponseListener.onFailure(response, e, null);
                    }
                }

                switch(region){
                    case DALLAS:
                        objectStorageURL = DALLAS_API_URL + projectID;
                        break;
                    case LONDON:
                        objectStorageURL = LONDON_API_URL + projectID;
                        break;
                }

                if(userResponseListener != null){
                    userResponseListener.onSuccess(authToken);
                }
            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                if(userResponseListener != null){
                    userResponseListener.onFailure(response, t, extendedInfo);
                }
            }
        });
    }

    protected static JSONObject getAuthenticationRequestBody(String projectID, String userID, String password) {
        JSONObject bodyJSON = new JSONObject();

        try {
            JSONObject userJSON = new JSONObject();
            userJSON.put("id", userID);
            userJSON.put("password", password);

            JSONObject passwordJSON = new JSONObject();
            passwordJSON.put("user", userJSON);

            JSONObject identityJSON = new JSONObject();
            identityJSON.put("methods", new JSONArray("[ \"password\" ]"));
            identityJSON.put("password", passwordJSON);

            JSONObject projectJSON = new JSONObject();
            projectJSON.put("id", projectID);

            JSONObject scopeJSON = new JSONObject();
            scopeJSON.put("project", projectJSON);

            JSONObject authJSON = new JSONObject();
            authJSON.put("identity", identityJSON);
            authJSON.put("scope", scopeJSON);

            bodyJSON.put("auth", authJSON);
        } catch (JSONException e) {
            //Just creating JSONObject; no exceptions will occur.
        }
        return bodyJSON;
    }

    public static void createContainer(final String containerName, final ObjectStorageResponseListener<ObjectStorageContainer> userResponseListener){
        if(objectStorageURL == null){
            logger.error("You have not yet authenticated with Object Storage. Call ObjectStorage.connect() first.");
            return;
        }

        refreshAuthToken(new ObjectStorageResponseListener<String>() {
            @Override
            public void onSuccess(String authToken) {

            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                logger.error("Could not authenticate with Object Storage. Call ObjectStorage.connect() in order to do so.");
            }
        });

        Request containerRequest = new Request(objectStorageURL + "/" + containerName, Request.PUT);

        containerRequest.addHeader(AUTH_HEADER, authToken);

        String body = "";

        containerRequest.send(null, body, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                ObjectStorageContainer container = new ObjectStorageContainer(containerName);

                if(userResponseListener != null){
                    userResponseListener.onSuccess(container);
                }
            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                if(userResponseListener != null){
                    userResponseListener.onFailure(response, t, extendedInfo);
                }
            }
        });
    }

    public static void getContainer(final String containerName, final ObjectStorageResponseListener<ObjectStorageContainer> userResponseListener){
        if(objectStorageURL == null){
            logger.error("You have not yet authenticated with Object Storage. Call ObjectStorage.connect() first.");
            return;
        }

        refreshAuthToken(new ObjectStorageResponseListener<String>() {
            @Override
            public void onSuccess(String authToken) {

            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                logger.error("Could not authenticate with Object Storage. Call ObjectStorage.connect() in order to do so.");
            }
        });

        Request containerRequest = new Request(objectStorageURL + "/" + containerName, Request.GET);

        containerRequest.addHeader(AUTH_HEADER, authToken);

        containerRequest.send(null, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                ObjectStorageContainer container = new ObjectStorageContainer(containerName);

                if(userResponseListener != null){
                    userResponseListener.onSuccess(container);
                }
            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                if(userResponseListener != null){
                    userResponseListener.onFailure(response, t, extendedInfo);
                }
            }
        });
    }

    public static void getContainerList(final ObjectStorageResponseListener<List<ObjectStorageContainer>> userResponseListener){
        if(objectStorageURL == null){
            logger.error("You have not yet authenticated with Object Storage. Call ObjectStorage.connect() first.");
            return;
        }

        refreshAuthToken(new ObjectStorageResponseListener<String>() {
            @Override
            public void onSuccess(String authToken) {

            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                logger.error("Could not authenticate with Object Storage. Call ObjectStorage.connect() in order to do so.");
            }
        });

        Request containerRequest = new Request(objectStorageURL, Request.GET);

        containerRequest.addHeader(AUTH_HEADER, authToken);

        containerRequest.send(null, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                List<ObjectStorageContainer> containerList = new ArrayList<>();

                String responseBody = response.getResponseText();
                String[] containerNameList = responseBody.split("\n");

                for(String name : containerNameList){
                    containerList.add(new ObjectStorageContainer(name));
                }

                if(userResponseListener != null){
                    userResponseListener.onSuccess(containerList);
                }
            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                if(userResponseListener != null){
                    userResponseListener.onFailure(response, t, extendedInfo);
                }
            }
        });
    }

    public static void deleteContainer(final String containerName, final ObjectStorageResponseListener<Boolean> userResponseListener){
        if(objectStorageURL == null){
            logger.error("You have not yet authenticated with Object Storage. Call ObjectStorage.connect() first.");
            return;
        }

        refreshAuthToken(new ObjectStorageResponseListener<String>() {
            @Override
            public void onSuccess(String authToken) {

            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                logger.error("Could not authenticate with Object Storage. Call ObjectStorage.connect() in order to do so.");
            }
        });

        Request containerRequest = new Request(objectStorageURL + "/" + containerName, Request.DELETE);

        containerRequest.addHeader(AUTH_HEADER, authToken);

        containerRequest.send(null, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {

                if(userResponseListener != null){
                    logger.debug("Successfully deleted container: " + containerName);
                    userResponseListener.onSuccess(null); //TODO: send something other than null?
                }
            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                logger.error("Failed to delete container: " + containerName);
                if(userResponseListener != null){
                    userResponseListener.onFailure(response, t, extendedInfo);
                }
            }
        });
    }

    public static void getAccountMetadata(final ObjectStorageResponseListener<Map<String, List<String>>> userResponseListener){
        if(objectStorageURL == null){
            logger.error("You have not yet authenticated with Object Storage. Call ObjectStorage.connect() first.");
            return;
        }
        refreshAuthToken(new ObjectStorageResponseListener<String>() {
            @Override
            public void onSuccess(String authToken) {
                Request containerRequest = new Request(objectStorageURL, Request.HEAD);

                containerRequest.addHeader(AUTH_HEADER, authToken);

                containerRequest.send(null, new ResponseListener() {
                    @Override
                    public void onSuccess(Response response) {
                        Map<String, List<String>> metadataMap = response.getHeaders();

                        logger.debug("Successfully retrieved account metadata.");

                        if(userResponseListener != null){
                            userResponseListener.onSuccess(metadataMap);
                        }
                    }

                    @Override
                    public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                        logger.error("Failed to retrieve account metadata.");
                        if(userResponseListener != null){
                            userResponseListener.onFailure(response, t, extendedInfo);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                logger.error("Could not authenticate with Object Storage. Call ObjectStorage.connect() in order to do so.");
            }
        });
    }

    public static void updateAccountMetadata(final Map<String, String> metadataUpdates, final ObjectStorageResponseListener<Boolean> userResponseListener){ //TODO: use something other than boolean?
        if(objectStorageURL == null){
            logger.error("You have not yet authenticated with Object Storage. Call ObjectStorage.connect() first.");
            return;
        }

        refreshAuthToken(new ObjectStorageResponseListener<String>() {
            @Override
            public void onSuccess(String authToken) {
                Request containerRequest = new Request(objectStorageURL, Request.POST);

                containerRequest.addHeader(AUTH_HEADER, authToken);

                for(Map.Entry<String, String> metadata : metadataUpdates.entrySet()){
                    containerRequest.addHeader(metadata.getKey(), metadata.getValue());
                }

                String body = "";

                containerRequest.send(null, body, new ResponseListener() {
                    @Override
                    public void onSuccess(Response response) {
                        logger.debug("Account metadata successfully updated.");
                        if(userResponseListener != null){
                            userResponseListener.onSuccess(true); //TODO: pass null?
                        }
                    }

                    @Override
                    public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                        logger.error("Failed to update account metadata.");
                        if(userResponseListener != null){
                            userResponseListener.onFailure(response, t, extendedInfo);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                logger.error("Could not authenticate with Object Storage. Call ObjectStorage.connect() in order to do so.");
            }
        });
    }

    protected static boolean hasToReauthenticate(){
        if(expiresAt == null || authToken == null){
            return true;
        }

        try {
            Date expirationDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(expiresAt);

            return expirationDate.before(new Date());
        } catch (ParseException e) {
            logger.error("Failed to parse expiration date, will have to reauthenticate.", e);
            return true;
        }
    }

    protected static void refreshAuthToken(ObjectStorageResponseListener<String> userResponseListener){
        connect(projectID, userID, password, userResponseListener);
    }
}