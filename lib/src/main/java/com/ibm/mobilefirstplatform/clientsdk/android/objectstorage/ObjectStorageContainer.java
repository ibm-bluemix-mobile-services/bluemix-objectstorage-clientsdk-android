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

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;

import java.io.InputStream;
import java.util.List;

public class ObjectStorageContainer {
    public static final String METADATA_PREFIX = "X-Container-Meta-";

    protected String name;
    protected String url;

    public ObjectStorageContainer(String name){
        this.name = name;

        url = ObjectStorage.objectStorageURL + "/" + name;
    }

    public String getName(){
        return name;
    }

    public void storeObject(String objectName, InputStream objectData){

    }

    public ObjectStorageObject getObject(String objectName){
        //TODO:
        return null;
    }

    public List<ObjectStorageObject> getObjectList(){
        //TODO:
        return null;
    }

    public void deleteObject(String objectName){

    }

    public void delete(ObjectStorageResponseListener<Boolean> userResponseListener){
        ObjectStorage.deleteContainer(name, userResponseListener);
    }
}
