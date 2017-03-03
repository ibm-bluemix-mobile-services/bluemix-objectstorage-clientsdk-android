#IBM Bluemix Mobile Services - Object Storage Android SDK
===

[![Build Status](https://travis-ci.org/ibm-bluemix-mobile-services/bluemix-objectstorage-clientsdk-android.svg?branch=master)](https://travis-ci.org/ibm-bluemix-mobile-services/bluemix-objectstorage-clientsdk-android)
[![Build Status](https://travis-ci.org/ibm-bluemix-mobile-services/bluemix-objectstorage-clientsdk-android.svg?branch=development)](https://travis-ci.org/ibm-bluemix-mobile-services/bluemix-objectstorage-clientsdk-android)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.ibm.bluemixmobileservices.clientsdk.android/objectstorage/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.ibm.bluemixmobileservices.clientsdk.android/objectstorage)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/22fcc55378714620bfea8f14ea2a1932)](https://www.codacy.com/app/ibm-bluemix-mobile-services/bluemix-objectstorage-clientsdk-android?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ibm-bluemix-mobile-services/bluemix-objectstorage-clientsdk-android&amp;utm_campaign=Badge_Grade)
[![javadoc.io](https://javadoc-emblem.rhcloud.com/doc/com.ibm.bluemixmobileservices.clientsdk.android/objectstorage/badge.svg)](http://www.javadoc.io/doc/com.ibm.bluemixmobileservices.clientsdk.android/objectstorage)

This is the [IBM® Bluemix® Object Storage](https://new-console.ng.bluemix.net/docs/services/ObjectStorage/index.html) SDK for Android.
Object Storage is a cloud storage solution where each account has containers in it, and each folder holds objects.
Objects are used to store any data. As a simple example to understand the concept, think of containers as folders and objects as files.

###Adding the SDK to an existing Android project
To add the Object Storage SDK to your project, go to your module's `build.gradle` and add this into the `dependencies{}` closure:

```compile 'com.ibm.bluemixmobileservices.clientsdk.android:objectstorage:1.+'```

###Before using the SDK
In order to use this SDK, the `BMSClient` needs to be initialized as follows:

```BMSClient.initialize(getApplicationContext(), BMSClient.REGION_US_SOUTH);```

The first parameter is the application's context, which you can get from any of your activities by calling `activity.getApplicationContext()`.
Change the second parameter to the Bluemix region you are working from.

###Using the SDK
####Initializing the SDK and Authenticating to the Object Storage service
First, start by initializing the Object Storage SDK as follows:

```ObjectStorage.initialize(ObjectStorage.BluemixRegion.DALLAS);```

Where the region is the region where the Object Storage is.

Next, using your Object Storage service instance's service credentials, authenticate to the server:

```
    ObjectStorage.connect(projectID, userID, password, new ObjectStorageResponseListener<String>(){
        @Override
        public void onSuccess(String authToken) {
            //Handle success
        }
        
        @Override
        public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
            //Handle failure
        }
    });
```

After authenticating, you are now free to use the rest of the Object Storage functionality.

####Create, retrieve and delete containers
Create new containers as follows:

```
    ObjectStorage.createContainer(containerName, new ObjectStorageResponseListener<ObjectStorageContainer>(){
        @Override
        public void onSuccess(ObjectStorageContainer container) {
            //Handle success
        }
        
        @Override
        public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
            //Handle failure
        }
    });
```

This will return an `ObjectStorageContainer` in the `onSuccess` callback.

Retrieve existing containers as follows:
```
    ObjectStorage.getContainer(containerName, new ObjectStorageResponseListener<ObjectStorageContainer>(){
        @Override
        public void onSuccess(ObjectStorageContainer container) {
            //Handle success
        }
        
        @Override
        public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
            //Handle failure
        }
    });
```

This will return an `ObjectStorageContainer` in the `onSuccess` callback.

Get a list of all containers in this account:
```
    ObjectStorage.getContainerList(containerName, new ObjectStorageResponseListener<List<ObjectStorageContainer>>(){
        @Override
        public void onSuccess(List<ObjectStorageContainer> containerList) {
            //Handle success
        }
        
        @Override
        public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
            //Handle failure
        }
    });
```

This will return a list of `ObjectStorageContainer` in the `onSuccess` callback.

Delete a container:
```
    ObjectStorage.deleteContainer(containerName, new ObjectStorageResponseListener<Void>(){
        @Override
        public void onSuccess(Void noReturnValue) {
            //Handle success
        }
        
        @Override
        public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
            //Handle failure
        }
    });
```

or you can also call `ObjectStorageContainer.delete()`. This will only call the `onSuccess` callback; no return value is given.

####Create, update, retrieve, load and delete objects
To upload a new object to Object Storage with its associated data:
```
    byte[] objectData = getObjectDataAsBytes();
    
    container.storeObject(containerName, objectData, new ObjectStorageResponseListener<ObjectStorageObject>(){
        @Override
        public void onSuccess(ObjectStorageObject storedObject) {
            //Handle success
        }
        
        @Override
        public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
            //Handle failure
        }
    });
```

This will return an `ObjectStorageObject` in the `onSuccess` callback.

To retrieve an object that was already uploaded to Object Storage:
```
    container.getObject(containerName, new ObjectStorageResponseListener<ObjectStorageObject>(){
        @Override
        public void onSuccess(ObjectStorageObject storedObject) {
            //Handle success
        }
        
        @Override
        public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
            //Handle failure
        }
    });
```

This will return an `ObjectStorageObject` in the `onSuccess` callback.

To get a list of all the objects available in a given container:
```
    container.getObjectList(containerName, new ObjectStorageResponseListener<List<ObjectStorageObject>>(){
        @Override
        public void onSuccess(List<ObjectStorageObject> storedObjectList) {
            //Handle success
        }
        
        @Override
        public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
            //Handle failure
        }
    });
```

This will return a list of `ObjectStorageObject` in the `onSuccess` callback.

To delete an object:
```
    container.deleteObject(containerName, new ObjectStorageResponseListener<Void>(){
        @Override
        public void onSuccess(Void noReturnValue) {
            //Handle success
        }
        
        @Override
        public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
            //Handle failure
        }
    });
```

This will call the `onSuccess` callback with no return value. You can also call `storedObject.delete()` for the same effect.

You can use the `ObjectStorageObject` to load the object's data contents:
```
    storedObject.load(shouldCache, new ObjectStorageResponseListener<byte[]>(){
        @Override
        public void onSuccess(byte[] objectData) {
            //Handle success
        }
        
        @Override
        public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
            //Handle failure
        }
    });
```

This will return the object's data as a `byte[]`. Note that you can choose to cache or not; 
if you choose to do so, the object's data will be kept in memory and can be accessed by calling `storedObject.getCachedData()`.

####Get and update account/container/object metadata
You can add metadata to your object storage account, or to any container or object, which will be kept in Object Storage alongside
everything else. For example, you can use this to indicate a Category for the containers, or an author for the object, and so on.

To add metadata to your Object Storage account:
```
    Map<String,String> metadataUpdates = new HashMap<String,String>();
    
    metadataUpdates.put(ObjectStorage.METADATA_PREFIX + "Author", "Daniel González");
    
    ObjectStorage.updateAccountMetadata(metadataUpdates, new ObjectStorageResponseListener<Void>(){
        @Override
        public void onSuccess(Void noReturnValue) {
            //Handle success
        }
        
        @Override
        public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
            //Handle failure
        }
    });
```

This will call the `onSuccess` callback with no return value.

To get the account's metadata:
```
    ObjectStorage.getAccountMetadata(new ObjectStorageResponseListener<Map<String, List<String>(){
        @Override
        public void onSuccess(Map<String, List<String> metadata) {
            //Handle success
        }
        
        @Override
        public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
            //Handle failure
        }
    });
```

This will return the account metadata on the `onSuccess` callback.

To update a container's metadata:
```
    Map<String,String> metadataUpdates = new HashMap<String,String>();
    
    metadataUpdates.put(ObjectStorageContainer.METADATA_PREFIX + "Category", "Literature");
    
    container.updateMetadata(metadataUpdates, new ObjectStorageResponseListener<Void>(){
        @Override
        public void onSuccess(Void noReturnValue) {
            //Handle success
        }
        
        @Override
        public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
            //Handle failure
        }
    });
```

This will call the `onSuccess` callback with no return value.

To get a container's metadata:
```
    container.getMetadata(new ObjectStorageResponseListener<Map<String, List<String>>(){
        @Override
        public void onSuccess(Map<String, List<String> metadata) {
            //Handle success
        }
        
        @Override
        public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
            //Handle failure
        }
    });
```

This will return the container's metadata on the `onSuccess` callback.

To update an object's metadata:
```
    Map<String,String> metadataUpdates = new HashMap<String,String>();
        
    metadataUpdates.put(ObjectStorageObject.METADATA_PREFIX + "Author", "Daniel González");
    
    storedObject.updateMetadata(metadataUpdates, new ObjectStorageResponseListener<Void>(){
        @Override
        public void onSuccess(Void noReturnValue) {
            //Handle success
        }
        
        @Override
        public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
            //Handle failure
        }
    });
```

This will call the `onSuccess` callback with no return value.

To get an object's metadata:
```
    storedObject.getMetadata(new ObjectStorageResponseListener<Map<String, List<String>>(){
        @Override
        public void onSuccess(Map<String, List<String> metadata) {
            //Handle success
        }
        
        @Override
        public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
            //Handle failure
        }
    });
```

This will return the object's metadata on the `onSuccess` callback.

###Supported Levels
The package is supported on Android API level 17 and up (Android 4.2.x and up).

###Change log

####1.0.1
* Fixed bug where invalid containers/objects would be included when retrieving lists of containers/objects.

####1.0.0
* Initial release.

###License

Copyright 2016 IBM Corp.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
