//
// $Id$

package com.threerings.msoy.world.client {

public interface LoadingWatcher
{
    /**
     * Update whether we're loading or not.
     */
    function setLoading (loading :Boolean, loadingDecor :Boolean) :void;
}
}
