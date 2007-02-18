//
// $Id$

package com.threerings.msoy.client {

import flash.display.Stage;

/**
 * Manages the simple in-header client.
 */
public class HeaderClient extends BaseClient
{
    public static const log :Log = Log.getLog(HeaderClient);

    public function HeaderClient (stage :Stage)
    {
        super(stage);
        logon();
    }
}
}
