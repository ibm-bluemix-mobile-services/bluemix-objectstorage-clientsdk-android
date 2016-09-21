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

/**
 * This class represents an object that has been stored in Object Storage. This kind of object has
 * associated data stored with it. The data is given as a byte array.
 */
public class ObjectStorageObject {
    public static final String METADATA_PREFIX = "X-Object-Meta-";

    public static Logger logger = Logger.getLogger(Logger.INTERNAL_PREFIX + ObjectStorageObject.class.getName());

    protected String name = null;
    protected ObjectStorageContainer container = null;
    protected String url = null;

    protected byte[] bytes;

    /**
     * Create a new Object Storage object with the given name and data, inside the given container.
     * If the object has not been stored previously in Object Storage, call
     * {@link ObjectStorageContainer#storeObject(String, byte[], ObjectStorageResponseListener)} first.
     * @param name the name of the object
     * @param container the container this object is stored in
     * @param objectBytes the object's data, given as a byte array
     */
    public ObjectStorageObject(String name, ObjectStorageContainer container, byte[] objectBytes){
        this.name = name;
        this.container = container;
        this.url = ObjectStorage.objectStorageURL + "/" + container.getName() + "/" + name;
        this.bytes = objectBytes;
    }

    /**
     * Get the name of this object.
     * @return the name of this object
     */
    public String getName(){
        return name;
    }

    /**
     * Load the given object's data from Object Storage as a byte array.
     * @param shouldCache specify whether this object's data should be cached in memory, which can be accessed with {@link #getCachedData()}
     * @param userResponseListener an optional response listener with onSuccess and onFailure callbacks. If successful, onSuccess will be called with the object's data as a byte array.
     */
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

    /**
     * Delete this object from Object Storage. It only deletes the object from the container from which it was retrieved.
     * @param userResponseListener an optional response listener with onSuccess and onFailure callbacks. If successful, onSuccess will be called with null parameters.
     */
    public void delete(final ObjectStorageResponseListener<Void> userResponseListener){
        container.deleteObject(name, userResponseListener);
    }

    /**
     * Get a map of the metadata associated with this object.
     * @param userResponseListener an optional response listener with onSuccess and onFailure callbacks. If successful, onSuccess will be called with a map of the object's metadata.
     */
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

    /**
     * Update this object's metadata with the given map of metadata values. In order to do so, prefix all metadata names with {@link #METADATA_PREFIX};
     * @param metadataUpdates a map of the metadata to be added to this object
     * @param userResponseListener an optional response listener with onSuccess and onFailure callbacks. If successful, onSuccess will be called with null parameters.
     */
    public void updateMetadata(final Map<String, String> metadataUpdates, final ObjectStorageResponseListener<Void> userResponseListener) {
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
                            userResponseListener.onSuccess(null);
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

    /**
     * Get this object's data as a byte array that was cached after calling {@link #load(boolean, ObjectStorageResponseListener)}.
     * @return this object's data, as a byte array. This will be null if the object has not been cached while loading it.
     */
    public byte[] getCachedData(){
        return bytes;
    }

    @Override
    public String toString(){
        return name;
    }

}
