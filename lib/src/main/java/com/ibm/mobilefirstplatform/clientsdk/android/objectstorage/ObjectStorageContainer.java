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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ObjectStorageContainer {
    public static final String METADATA_PREFIX = "X-Container-Meta-";

    public static Logger logger = Logger.getLogger(Logger.INTERNAL_PREFIX + ObjectStorageObject.class.getName());

    protected String name;
    protected String url;

    public ObjectStorageContainer(String name){
        this.name = name;

        url = ObjectStorage.objectStorageURL + "/" + name;
    }

    public String getName(){
        return name;
    }

    public void storeObject(final String objectName, final byte[] objectData, final ObjectStorageResponseListener<ObjectStorageObject> userResponseListener){ //TODO: Return something other than boolean?
        //This container is used to create the object to be returned.
        final ObjectStorageContainer container = this;

        ObjectStorage.refreshAuthToken(new ObjectStorageResponseListener<String>() {
            @Override
            public void onSuccess(String authToken) {
                Request storeRequest = new Request(url + "/" + objectName, Request.PUT);

                storeRequest.addHeader(ObjectStorage.AUTH_HEADER, authToken);

                storeRequest.addHeader(Request.CONTENT_TYPE, "application/octet-stream");
                storeRequest.addHeader("Content-Length", "" + objectData.length);

                storeRequest.send(null, objectData, new ResponseListener() {
                    @Override
                    public void onSuccess(Response response) {
                        logger.debug("Successfully stored object: " + objectName);

                        ObjectStorageObject object = new ObjectStorageObject(objectName, container, objectData);

                        userResponseListener.onSuccess(object);
                    }

                    @Override
                    public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                        logger.error("Failed to store object: " + objectName);
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

    public void getObject(final String objectName, final ObjectStorageResponseListener<ObjectStorageObject> userResponseListener){
        if(objectName == null){
            logger.error("Object name cannot be null.");

            if(userResponseListener != null){
                userResponseListener.onFailure(null, new Throwable("Failed to get object. Object name cannot be null."), null);
            }
        }

        //Used to pass container reference to created object.
        final ObjectStorageContainer container = this;

        ObjectStorage.refreshAuthToken(new ObjectStorageResponseListener<String>() {
            @Override
            public void onSuccess(String authToken) {
                Request containerRequest = new Request(url + "/" + objectName, Request.GET);

                containerRequest.addHeader(ObjectStorage.AUTH_HEADER, authToken);

                containerRequest.send(null, new ResponseListener() {
                    @Override
                    public void onSuccess(final Response response) {
                        logger.debug("Successfully retrieved object.");

                        byte[] objectBytes = response.getResponseBytes();

                        ObjectStorageObject object = new ObjectStorageObject(objectName, container, objectBytes);

                        if(userResponseListener != null){
                            userResponseListener.onSuccess(object);
                        }
                    }

                    @Override
                    public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                        logger.error("Failed to retrieve object: " + objectName);

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

    public void getObjectList(final ObjectStorageResponseListener<List<ObjectStorageObject>> userResponseListener){
        //Used to pass container reference to created objects.
        final ObjectStorageContainer container = this;

        ObjectStorage.refreshAuthToken(new ObjectStorageResponseListener<String>() {
            @Override
            public void onSuccess(String authToken) {
                Request objectListRequest = new Request(url, Request.GET);

                objectListRequest.addHeader(ObjectStorage.AUTH_HEADER, authToken);

                objectListRequest.send(null, new ResponseListener() {
                    @Override
                    public void onSuccess(Response response) {
                        logger.debug("Successfully retrieved object list for " + name + ".");
                        List<ObjectStorageObject> objectList = new ArrayList<>();

                        String responseBody = response.getResponseText();
                        String[] objectNameList = responseBody.split("\n");

                        for(String objectName : objectNameList){
                            objectList.add(new ObjectStorageObject(objectName, container, null));
                        }

                        if(userResponseListener != null){
                            userResponseListener.onSuccess(objectList);
                        }
                    }

                    @Override
                    public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                        logger.error("Failed to retrieve object list for container: " + name);
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

    public void deleteObject(final String objectName, final ObjectStorageResponseListener<Boolean> userResponseListener){ //TODO: Return something other than boolean?
        ObjectStorage.refreshAuthToken(new ObjectStorageResponseListener<String>() {
            @Override
            public void onSuccess(String authToken) {
                Request objectRequest = new Request(url + "/" + objectName, Request.DELETE);

                objectRequest.addHeader(ObjectStorage.AUTH_HEADER, authToken);

                objectRequest.send(null, new ResponseListener() {
                    @Override
                    public void onSuccess(Response response) {
                        logger.debug("Successfully deleted object: " + objectName);

                        if(userResponseListener != null){
                            userResponseListener.onSuccess(true); //TODO: Use something other than boolean?
                        }
                    }

                    @Override
                    public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                        logger.error("Failed to delete object: " + objectName);
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

    public void delete(final ObjectStorageResponseListener<Boolean> userResponseListener){ //TODO: Use something other than boolean?
        ObjectStorage.deleteContainer(name, userResponseListener);
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

                        logger.debug("Successfully retrieved container metadata.");

                        if(userResponseListener != null){
                            userResponseListener.onSuccess(metadataMap);
                        }
                    }

                    @Override
                    public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                        logger.error("Failed to retrieve container metadata.");
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

    @Override
    public String toString(){
        return name;
    }
}
