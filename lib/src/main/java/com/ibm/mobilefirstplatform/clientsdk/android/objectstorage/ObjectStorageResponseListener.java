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

    /**
     * This is success callback, which will be called with the relevant return value.
     * This will vary based on what method is being used.
     * @param returnValue
     */
    void onSuccess(T returnValue);

    /**
     * This is the failure callback. Any of the three parameters may be null, depending on what error occured.
     *
     * @param response the {@link com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response} object that represents the response from the server. Can get status code, headers and body, if available.
     * @param t The exception/throwable that caused the failure, if applicable.
     * @param extendedInfo Any additional information regarding the failure. It is null in most cases.
     */
    void onFailure(Response response, Throwable t, JSONObject extendedInfo);
}
