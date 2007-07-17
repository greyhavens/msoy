//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.whirled.client.PendingData;

import com.threerings.msoy.world.data.MsoyLocation;

/**
 * Extends our pending scene data with Whirled specific bits.
 */
public class MsoyPendingData extends PendingData
{
    /** The location in the new scene at which we want to arrive. */
    public var destLoc :MsoyLocation;
}
}
