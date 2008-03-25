//
// $Id$

package com.threerings.msoy.client {

import flash.display.LoaderInfo;

public interface LoadingWatcher
{
    /**
     * Called to hand us a tasty loaderinfo in which to sink our tendrils.
     */
    function watchLoader (info :LoaderInfo, isPrimaryForPlace :Boolean = false) :void;
}
}
