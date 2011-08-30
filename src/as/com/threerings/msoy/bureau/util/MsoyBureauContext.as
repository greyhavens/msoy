//
// $Id$

package com.threerings.msoy.bureau.util {

import com.whirled.bureau.util.WhirledBureauContext;

import com.threerings.msoy.bureau.client.WindowDirector;

/** Context for Msoy bureau client code. */
public interface MsoyBureauContext extends WhirledBureauContext
{
    /** Access the window director for this bureau. */
    function getWindowDirector () :WindowDirector;
}

}

