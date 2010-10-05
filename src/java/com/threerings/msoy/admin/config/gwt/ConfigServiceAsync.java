//
// $Id: $

package com.threerings.msoy.admin.config.gwt;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.admin.config.gwt.ConfigService.ConfigurationRecord;
import com.threerings.msoy.admin.config.gwt.ConfigService.ConfigurationResult;

/**
 * Provides the asynchronous version of {@link com.threerings.msoy.admin.gwt.AdminService}.
 */
public interface ConfigServiceAsync
{
    /**
     * The async version of {@link com.threerings.msoy.admin.gwt.ConfigService#getConfig}.
     */
    public void getConfiguration (AsyncCallback<ConfigurationResult> callback);

    /**
     * The async version of {@link com.threerings.msoy.admin.gwt.ConfigService#updateConfiguration}.
     */
    public void updateConfiguration (
        String key, ConfigField[] updates, AsyncCallback<ConfigurationRecord> callback);

}
