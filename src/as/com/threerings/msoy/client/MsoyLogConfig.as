package com.threerings.msoy.client {

import com.threerings.util.Log;
import com.threerings.util.StringUtil;

/**
 * Configures narya's logging levels using the msoy configuration.
 * @see com.threerings.util.Log
 * @see DeploymentConfig
 */
public class MsoyLogConfig
{
    /**
     * Initializes the log levels. Top level msoy classes should call this to get log level 
     * configuration goodness.
     */
    public static function init () :void
    {
        var levels :String = DeploymentConfig.logLevelsConfig;
        if (!StringUtil.isBlank(levels)) {
            trace("===== SETTING MSOY LOG LEVELS '" + levels + "'");
            Log.setLevels(levels);
        }
    }
}
}
