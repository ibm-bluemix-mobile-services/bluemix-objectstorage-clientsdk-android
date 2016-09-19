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

import java.io.InputStream;

public class ObjectStorageObject {
    public final String METADATA_PREFIX = "X-Object-Meta-";

    protected String name = null;
    protected String containerName = null;
    protected String url = null;

    public ObjectStorageObject(String name, String containerName){
        this.name = name;
        this.containerName = containerName;
        this.url = ObjectStorage.objectStorageURL + "/" + containerName + "/" + name;
    }

    public InputStream load(boolean shouldCache){
        return null;
    }

    public void delete(){

    }

    public String getName(){
        return name;
    }


}
