//
// $Id$

package com.threerings.msoy.data;

import com.threerings.crowd.data.PlaceConfig;

/**
 * Does something extraordinary.
 */
public class SimpleChatConfig extends PlaceConfig
{
    public Class getControllerClass ()
    {
        return null; // we have no equivalent in the java code
    }

    public String getManagerClassName ()
    {
        return "com.threerings.msoy.server.SimpleChatManager";
    }
}
