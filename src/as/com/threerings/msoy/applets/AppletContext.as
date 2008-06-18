//
// $Id$

package com.threerings.msoy.applets {

import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

public class AppletContext
{
    public function AppletContext ()
    {
        _msgMgr = new MessageManager();

        _appletBundle = _msgMgr.getBundle("applet");
    }

    /**
     * Access the applet message bundle.
     */
    public function get APPLET () :MessageBundle
    {
        return _appletBundle;
    }

    /** The message manager. */
    protected var _msgMgr :MessageManager;

    /** The applet message bundle. */
    protected var _appletBundle :MessageBundle;
}
}
