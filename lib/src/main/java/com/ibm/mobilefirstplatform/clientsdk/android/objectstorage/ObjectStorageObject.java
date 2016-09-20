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

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Request;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class ObjectStorageObject {
    public static final String METADATA_PREFIX = "X-Object-Meta-";

    public static Logger logger = Logger.getLogger(Logger.INTERNAL_PREFIX + ObjectStorageObject.class.getName());

    protected String name = null;
    protected ObjectStorageContainer container = null;
    protected String url = null;

    protected byte[] bytes;

    public ObjectStorageObject(String name, ObjectStorageContainer container, byte[] objectBytes){
        this.name = name;
        this.container = container;
        this.url = ObjectStorage.objectStorageURL + "/" + container.getName() + "/" + name;
        this.bytes = objectBytes;
    }

    public String getName(){
        return name;
    }

    public void load(final boolean shouldCache, final ObjectStorageResponseListener<byte[]> userResponseListener){
        logger.debug("Loading object: " + name);
        ObjectStorage.refreshAuthToken(new ObjectStorageResponseListener<String>() {
            @Override
            public void onSuccess(String authToken) {
                Request loadRequest = new Request(url, Request.GET);

                loadRequest.addHeader(ObjectStorage.AUTH_HEADER, authToken);

                loadRequest.send(null, new ResponseListener() {
                    @Override
                    public void onSuccess(Response response) {
                        logger.debug("Successfully loaded object: " + name);

                        byte[] data = response.getResponseBytes();

                        if(shouldCache){
                            bytes = data;
                        }

                        if(userResponseListener != null){
                            userResponseListener.onSuccess(data);
                        }
                    }

                    @Override
                    public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                        logger.error("Failed to load object: " + name);
                        if(userResponseListener != null){
                            userResponseListener.onFailure(response, t, extendedInfo);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                logger.error("Could not authenticate to Object Storage. Call ObjectStorage.connect() in order to do so.");
                if(userResponseListener != null){
                    userResponseListener.onFailure(response, t, extendedInfo);
                }
            }
        });
    }

    public void delete(final ObjectStorageResponseListener<Boolean> userResponseListener){ //TODO: Should use Boolean?
        container.deleteObject(name, userResponseListener);
    }

    public void getMetadata(final ObjectStorageResponseListener<Map<String, List<String>>> userResponseListener){
        if(url == null){
            logger.error("You have not yet authenticated to Object Storage. Call ObjectStorage.connect() first.");
            return;
        }
        ObjectStorage.refreshAuthToken(new ObjectStorageResponseListener<String>() {
            @Override
            public void onSuccess(String authToken) {
                Request containerRequest = new Request(url, Request.HEAD);

                containerRequest.addHeader(ObjectStorage.AUTH_HEADER, authToken);

                containerRequest.send(null, new ResponseListener() {
                    @Override
                    public void onSuccess(Response response) {
                        Map<String, List<String>> metadataMap = response.getHeaders();

                        logger.debug("Successfully retrieved object metadata.");

                        if(userResponseListener != null){
                            userResponseListener.onSuccess(metadataMap);
                        }
                    }

                    @Override
                    public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                        logger.error("Failed to retrieve object metadata.");
                        if(userResponseListener != null){
                            userResponseListener.onFailure(response, t, extendedInfo);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                logger.error("Could not authenticate to Object Storage. Call ObjectStorage.connect() in order to do so.");
                if(userResponseListener != null){
                    userResponseListener.onFailure(response, t, extendedInfo);
                }
            }
        });

    }

    public void updateMetadata(final Map<String, String> metadataUpdates, final ObjectStorageResponseListener<Boolean> userResponseListener) { //TODO: Use something other than boolean?
        if(url == null){
            logger.error("You have not yet authenticated to Object Storage. Call ObjectStorage.connect() first.");
            return;
        }

        ObjectStorage.refreshAuthToken(new ObjectStorageResponseListener<String>() {
            @Override
            public void onSuccess(String authToken) {
                Request containerRequest = new Request(url, Request.POST);

                containerRequest.addHeader(ObjectStorage.AUTH_HEADER, authToken);

                for(Map.Entry<String, String> metadata : metadataUpdates.entrySet()){
                    containerRequest.addHeader(metadata.getKey(), metadata.getValue());
                }

                String body = "";

                containerRequest.send(null, body, new ResponseListener() {
                    @Override
                    public void onSuccess(Response response) {
                        logger.debug("Object metadata successfully updated.");
                        if(userResponseListener != null){
                            userResponseListener.onSuccess(true); //TODO: pass null?
                        }
                    }

                    @Override
                    public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                        logger.error("Failed to update object metadata.");
                        if(userResponseListener != null){
                            userResponseListener.onFailure(response, t, extendedInfo);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                logger.error("Could not authenticate to Object Storage. Call ObjectStorage.connect() in order to do so.");
                if(userResponseListener != null){
                    userResponseListener.onFailure(response, t, extendedInfo);
                }
            }
        });
    }

    public byte[] getCachedData(){
        return bytes;
    }

    @Override
    public String toString(){
        return name;
    }

}
