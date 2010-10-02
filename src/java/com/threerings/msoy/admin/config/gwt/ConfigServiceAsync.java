//
// $Id: $


package com.threerings.msoy.admin.config.gwt;

import java.util.Collection;

import com.threerings.msoy.admin.config.gwt.ConfigService.ConfigurationResult;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Provides the asynchronous version of {@link com.threerings.msoy.admin.gwt.AdminService}.
 */
public interface ConfigServiceAsync
{
    /**
     * The async version of {@link com.threerings.msoy.admin.gwt.ConfigService#getConfig}.
     */
    public void getConfig (AsyncCallback<ConfigurationResult> callback);

    /**
     * The async version of {@link com.threerings.msoy.admin.gwt.ConfigService#updateConfiguration}.
     */
    public void updateConfiguration (
        Collection<ConfigField> updates, AsyncCallback<ConfigurationResult> callback);

}
