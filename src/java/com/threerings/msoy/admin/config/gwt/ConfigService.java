//
// $Id: $

package com.threerings.msoy.admin.config.gwt;

import java.util.Map;

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
        public Map<String, ConfigurationRecord> records;
    }

    public static class ConfigurationRecord
        implements IsSerializable
    {
        public ConfigField[] fields;
        public int updates;
    }

    public ConfigurationResult getConfiguration () throws ServiceException;

    public ConfigurationRecord updateConfiguration (String key, ConfigField[] updates)
        throws ServiceException;
}
