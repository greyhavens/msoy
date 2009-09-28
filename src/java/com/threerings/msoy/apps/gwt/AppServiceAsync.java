//
// $Id$

package com.threerings.msoy.apps.gwt;

import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.apps.gwt.AppService.AppData;
import com.threerings.msoy.facebook.gwt.FacebookInfo;
import com.threerings.msoy.facebook.gwt.FacebookTemplate;
import com.threerings.msoy.facebook.gwt.FeedThumbnail;
import com.threerings.msoy.facebook.gwt.KontagentInfo;

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

    /** The asynchronous version of {@link AppService#updateFacebookInfo}. */
    void updateFacebookInfo (FacebookInfo info, AsyncCallback<Void> callback);

    /** The asynchronous version of {@link AppService#loadNotifications}. */
    void loadNotifications (int appId, AsyncCallback<List<FacebookNotification>> callback);

    /** The asynchronous version of {@link AppService#loadNotifications}. */
    void saveNotification (int appId, FacebookNotification notif, AsyncCallback<Void> callback);

    /** The asynchronous version of {@link AppService#deleteNotification}. */
    void deleteNotification (int appId, String id, AsyncCallback<Void> callback);

    /** The asynchronous version of {@link AppService#scheduleNotification}. */
    void scheduleNotification (int appId, String id, int delay, AsyncCallback<Void> callback);

    /** The asynchronous version of {@link AppService#loadNotificationsStatus}. */
    void loadNotificationsStatus (
        int appId, AsyncCallback<List<FacebookNotificationStatus>> callback);

    /** The asynchronous version of {@link AppService#loadTemplates}. */
    void loadTemplates (int appId, AsyncCallback<List<FacebookTemplate>> callback);

    /** The asynchronous version of {@link AppService#updateTemplates}. */
    void updateTemplates (
        int appId, Set<FacebookTemplate> changed, Set<FacebookTemplate> removed,
        AsyncCallback<Void> callback);

    /** The asynchronous version of {@link AppService#loadThumbnails}. */
    void loadThumbnails(int appId, AsyncCallback<List<FeedThumbnail>> callback);

    /** The asynchronous version of {@link AppService#updateThumbnails}. */
    void updateThumbnails(int appId, List<FeedThumbnail> thumbnails, AsyncCallback<Void> callback);

    /** The asynchronous version of {@link AppService#updateKontagentInfo}. */
    void updateKontagentInfo (int appId, KontagentInfo kinfo, AsyncCallback<Void> callback);
}
