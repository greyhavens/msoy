//
// $Id$

package com.threerings.msoy.applets {

import mx.core.Application;

import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

public class AppletContext
{
    public function AppletContext (app :Application)
    {
        _app = app;
        _msgMgr = new MessageManager();

        _appletBundle = _msgMgr.getBundle("applet");
    }

    /**
     * Get the application containing this applet.
     */
    public function getApplication () :Application
    {
        return _app;
    }

    /**
     * Access the applet message bundle.
     */
    public function get APPLET () :MessageBundle
    {
        return _appletBundle;
    }

    /** The application containing this applet. If we're loaded into another
     * application, this will point to our lower parent, not the top-level app. */
    protected var _app :Application;

    /** The message manager. */
    protected var _msgMgr :MessageManager;

    /** The applet message bundle. */
    protected var _appletBundle :MessageBundle;
}
}
