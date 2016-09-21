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

/**
 * IBM® Object Storage is a Bluemix service that can be used to store any data. This class is used to
 * authenticate with the Object Storage service, as well as creating, retrieving and deleting any containers being used.
 */
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

    /**
     * Initialize the Object Storage SDK by specifying which Bluemix region the Object Storage service is in.
     * @param applicationContext the Android application context; can be found from any Activity by calling activity.getApplicationContext()
     * @param bluemixRegion the Bluemix region that the Object Storage service being used is in
     */
    public static void initialize(Context applicationContext, BluemixRegion bluemixRegion){
        context = applicationContext;
        region = bluemixRegion;

        if(region == null){
            region = BluemixRegion.DALLAS; //Set Dallas region by default if none is specified.
        }
    }

    /**
     * Authenticate with the Object Storage service using your project id, user id and password.
     * These service credentials can be found in Bluemix.
     *
     * @param projectIdentifier the project id from the Object Storage service credentials
     * @param userIdentifier the user id from the Object Storage service credentials
     * @param accountPassword the password from the Object Storage service credentials
     * @param userResponseListener an optional response listener with onSuccess and onFailure callbacks. If successful, onSuccess will be called with the authentication token.
     */
    public static void connect(final String projectIdentifier, String userIdentifier, String accountPassword, final ObjectStorageResponseListener<String> userResponseListener){
        if(userIdentifier == null || accountPassword == null){
            logger.debug("Authentication failed because the user ID or password was null.");
            if(userResponseListener != null){
                userResponseListener.onFailure(null, new Throwable("User ID and password cannot be null."), null);
            }
        }

        projectID = projectIdentifier;
        userID = userIdentifier;
        password = accountPassword;

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

    /**
     * Create the container with the given name, and return the created container in the response listener's success callback.
     * If the container already exists, it simply returns that existing container.
     *
     * @param containerName the name of the container to be created
     * @param userResponseListener an optional response listener with onSuccess and onFailure callbacks. If successful, onSuccess will be called with the created container.
     */
    public static void createContainer(final String containerName, final ObjectStorageResponseListener<ObjectStorageContainer> userResponseListener){
        if(objectStorageURL == null){
            logger.error("You have not yet authenticated with Object Storage. Call ObjectStorage.connect() first.");
            return;
        }

        refreshAuthToken(new ObjectStorageResponseListener<String>() {
            @Override
            public void onSuccess(String authToken) {
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

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                logger.error("Could not authenticate with Object Storage. Call ObjectStorage.connect() in order to do so.");
            }
        });
    }

    /**
     * Retrieve the container with the given name, which is returned in the onSuccess callback.
     * If the container does not exist, the onFailure callback will be called, which will have the response with the 404 from ObjectStorage.
     *
     * @param containerName the name of the container to be retrieved
     * @param userResponseListener an optional response listener with onSuccess and onFailure callbacks. If successful, onSuccess will be called with the container.
     */
    public static void getContainer(final String containerName, final ObjectStorageResponseListener<ObjectStorageContainer> userResponseListener){
        if(objectStorageURL == null){
            logger.error("You have not yet authenticated with Object Storage. Call ObjectStorage.connect() first.");
            return;
        }

        refreshAuthToken(new ObjectStorageResponseListener<String>() {
            @Override
            public void onSuccess(String authToken) {
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

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                logger.error("Could not authenticate with Object Storage. Call ObjectStorage.connect() in order to do so.");
            }
        });


    }

    /**
     * Get a list of all the containers in this Object Storage service instance.
     * @param userResponseListener an optional response listener with onSuccess and onFailure callbacks. If successful, onSuccess will be called with the list of containers.
     */
    public static void getContainerList(final ObjectStorageResponseListener<List<ObjectStorageContainer>> userResponseListener){
        if(objectStorageURL == null){
            logger.error("You have not yet authenticated with Object Storage. Call ObjectStorage.connect() first.");
            return;
        }

        refreshAuthToken(new ObjectStorageResponseListener<String>() {
            @Override
            public void onSuccess(String authToken) {
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

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                logger.error("Could not authenticate with Object Storage. Call ObjectStorage.connect() in order to do so.");
            }
        });
    }

    /**
     * Delete a container from this Object Storage account.
     * @param containerName the name of the container to be deleted
     * @param userResponseListener an optional response listener with onSuccess and onFailure callbacks. If successful, onSuccess will be called with null parameters.
     */
    public static void deleteContainer(final String containerName, final ObjectStorageResponseListener<Void> userResponseListener){
        if(objectStorageURL == null){
            logger.error("You have not yet authenticated with Object Storage. Call ObjectStorage.connect() first.");
            return;
        }

        refreshAuthToken(new ObjectStorageResponseListener<String>() {
            @Override
            public void onSuccess(String authToken) {
                Request containerRequest = new Request(objectStorageURL + "/" + containerName, Request.DELETE);

                containerRequest.addHeader(AUTH_HEADER, authToken);

                containerRequest.send(null, new ResponseListener() {
                    @Override
                    public void onSuccess(Response response) {

                        if(userResponseListener != null){
                            logger.debug("Successfully deleted container: " + containerName);
                            userResponseListener.onSuccess(null);
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

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                logger.error("Could not authenticate with Object Storage. Call ObjectStorage.connect() in order to do so.");
            }
        });


    }

    /**
     * Get the account metadata.
     * @param userResponseListener an optional response listener with onSuccess and onFailure callbacks. If successful, onSuccess will be called with a map of the metadata headers.
     */
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

    /**
     * Update the account metadata with the given metadata headers. When doing this, prepend the ObjectStorage.METADATA_PREFIX to the header names in the map.
     * @param metadataUpdates a map of metadata headers to be added to the account metadata
     * @param userResponseListener an optional response listener with onSuccess and onFailure callbacks. If successful, onSuccess will be called with null parameters.
     */
    public static void updateAccountMetadata(final Map<String, String> metadataUpdates, final ObjectStorageResponseListener<Void> userResponseListener){
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
                            userResponseListener.onSuccess(null);
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