package com.threerings.msoy.bureau.util {

import com.threerings.msoy.bureau.client.WindowDirector;
import com.whirled.bureau.util.WhirledBureauContext;

/** Context for Msoy bureau client code. */
public interface MsoyBureauContext extends WhirledBureauContext
{
    /** Access the window director for this bureau. */
    function getWindowDirector () :WindowDirector;
}

}

