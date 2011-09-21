//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.io.TypedArray;

import com.threerings.presents.client.InvocationService_InvocationListener;

/**
 * A functional adapter for the WorldService_HomeResultListener interface.
 */
public class WorldService_HomeResultListenerAdapter
    implements WorldService_HomeResultListener
{
    /**
     * Creates a new World service HomeResult listener that will delegate to the
     * given function(s). Any Function that is null will simply not be called.
     */
    public function WorldService_HomeResultListenerAdapter (
        readyToEnter :Function, selectGift :Function, failed :Function)
    {
        _readyToEnter = readyToEnter;
        _selectGift = selectGift;
        _failed = failed;
    }

    // from Java WorldService_HomeResultListener
    public function readyToEnter (arg1 :int) :void
    {
        if (_readyToEnter != null) {
            _readyToEnter(arg1);
        }
    }

    // from Java WorldService_HomeResultListener
    public function selectGift (arg1 :TypedArray /* of class com.threerings.msoy.item.data.all.Avatar */, arg2 :int) :void
    {
        if (_selectGift != null) {
            _selectGift(arg1, arg2);
        }
    }

    // from InvocationService_InvocationListener
    public function requestFailed (cause :String) :void
    {
        if (_failed != null) {
            _failed(cause);
        }
    }

    protected var _readyToEnter :Function;
    protected var _selectGift :Function;
    protected var _failed :Function;
}
}
