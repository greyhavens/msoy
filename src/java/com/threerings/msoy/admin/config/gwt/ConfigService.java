//
// $Id: $

package com.threerings.msoy.admin.config.gwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.threerings.msoy.web.gwt.ServiceException;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * Defines remote services available to admins.
 */
@RemoteServiceRelativePath(value=ConfigService.REL_PATH)
public interface ConfigService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/configsvc";

    /** The relative path for this service. */
    public static final String REL_PATH = "../../../.." + ConfigService.ENTRY_POINT;

    public static class ConfigurationResult
        implements IsSerializable
    {
        public ConfigField dummy;
        public Map<String, List<ConfigField>> records;
    }

    public ConfigurationResult getConfig () throws ServiceException;

    public ConfigurationResult updateConfiguration (List<ConfigField> updates);
}
