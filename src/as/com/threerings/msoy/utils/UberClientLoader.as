//
// $Id$

package com.threerings.msoy.utils {

import mx.controls.SWFLoader;

import com.threerings.msoy.client.DeploymentConfig;

public class UberClientLoader extends SWFLoader
{
    /** Mode constants */
    public static const CLIENT :int = 0;
    public static const AVATAR_VIEWER :int = 100;
    public static const GENERIC_VIEWER :int = 199;

    /**
     * A SWF loader that is simply configured to view the uberclient in
     * a particular mode.
     *
     * @see UberClient
     */
    public function UberClientLoader (mode :int)
    {
        _mode = mode;
    }

    /**
     * If called with no args, loads the UberClient.
     */
    override public function load (url :Object = null) :void
    {
        if (url == null) {
            url = "/clients/" + DeploymentConfig.version + "/world-client.swf";
        }
        super.load(url);
    }

    /**
     * Get the mode that should be used by the uberclient.
     */
    public function getMode () :int
    {
        return _mode;
    }

    protected var _mode :int;
}
}
