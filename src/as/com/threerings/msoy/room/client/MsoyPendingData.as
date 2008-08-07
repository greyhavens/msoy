//
// $Id$

package com.threerings.msoy.room.client {

import com.threerings.whirled.client.PendingData;

import com.threerings.msoy.room.data.MsoyLocation;

/**
 * Extends our pending scene data with Whirled specific bits.
 */
public class MsoyPendingData extends PendingData
{
    /** The location in the new scene at which we want to arrive. */
    public var destLoc :MsoyLocation;

    /** The id of the scene from which we came. */
    public var previousSceneId :int;

    /** Message to be displayed after a successful scene traversal. */
    public var message :String;
}
}
