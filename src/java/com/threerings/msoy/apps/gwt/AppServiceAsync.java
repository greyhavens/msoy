//
// $Id$

package com.threerings.msoy.apps.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.apps.gwt.AppService.AppData;

/**
 * Methods for the web client to access and update applications.
 */
public interface AppServiceAsync
{
    /** The asynchronous version of {@link AppService#getApps}. */
    void getApps (AsyncCallback<List<AppInfo>> callback);

    /** The asynchronous version of {@link AppService#createApp}. */
    void createApp (String name, AsyncCallback<Integer> callback);

    /** The asynchronous version of {@link AppService#getAppData}. */
    void getAppData (int appId, AsyncCallback<AppData> callback);

    /** The asynchronous version of {@link AppService#deleteApp}. */
    void deleteApp (int appId, AsyncCallback<Void> callback);

    /** The asynchronous version of {@link AppService#updateAppInfo}. */
    void updateAppInfo (AppInfo appInfo, AsyncCallback<Void> callback);
}
