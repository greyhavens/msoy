//
// $Id: $

package com.threerings.msoy.admin.config.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.admin.config.gwt.ConfigField;
import com.threerings.msoy.admin.config.gwt.ConfigService.ConfigurationResult;

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
        List<ConfigField> updates, AsyncCallback<ConfigurationResult> callback);

}
