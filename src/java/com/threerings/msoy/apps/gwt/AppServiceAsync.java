//
// $Id$

package com.threerings.msoy.apps.gwt;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.facebook.gwt.FacebookInfo;
import com.threerings.msoy.facebook.gwt.FacebookTemplate;
import com.threerings.msoy.facebook.gwt.FeedThumbnail;
import com.threerings.msoy.facebook.gwt.KontagentInfo;

/**
 * Provides the asynchronous version of {@link AppService}.
 */
public interface AppServiceAsync
{
    /**
     * The async version of {@link AppService#getApps}.
     */
    void getApps (AsyncCallback<List<AppInfo>> callback);

    /**
     * The async version of {@link AppService#createApp}.
     */
    void createApp (String name, AsyncCallback<Integer> callback);

    /**
     * The async version of {@link AppService#getAppData}.
     */
    void getAppData (int appId, AsyncCallback<AppService.AppData> callback);

    /**
     * The async version of {@link AppService#deleteApp}.
     */
    void deleteApp (int appId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AppService#updateAppInfo}.
     */
    void updateAppInfo (AppInfo appInfo, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AppService#updateFacebookInfo}.
     */
    void updateFacebookInfo (FacebookInfo info, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AppService#loadTemplates}.
     */
    void loadTemplates (int appId, AsyncCallback<List<FacebookTemplate>> callback);

    /**
     * The async version of {@link AppService#updateTemplates}.
     */
    void updateTemplates (int appId, Set<FacebookTemplate> changed, Set<FacebookTemplate.Key> removed, Map<FacebookTemplate.Key, Boolean> abled, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AppService#loadThumbnails}.
     */
    void loadThumbnails (int appId, AsyncCallback<List<FeedThumbnail>> callback);

    /**
     * The async version of {@link AppService#updateThumbnails}.
     */
    void updateThumbnails (int appId, List<FeedThumbnail> thumbnails, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AppService#updateKontagentInfo}.
     */
    void updateKontagentInfo (int appId, KontagentInfo kinfo, AsyncCallback<Void> callback);
}
