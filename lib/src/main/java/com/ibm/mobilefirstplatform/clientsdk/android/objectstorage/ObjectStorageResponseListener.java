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

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;

import org.json.JSONObject;

/**
 * Response listener interface with generic return value so that ideally there is only one type of response listener used
 * for all different methods.
 *
 * @param <T> the return type value
 */
public interface ObjectStorageResponseListener<T> {

    void onSuccess(T returnValue);
    void onFailure(Response response, Throwable t, JSONObject extendedInfo);
}
