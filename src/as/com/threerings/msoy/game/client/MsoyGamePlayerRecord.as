//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.crowd.data.OccupantInfo;

import com.whirled.game.client.GamePlayerRecord;

import com.threerings.msoy.data.MsoyUserOccupantInfo;

public class MsoyGamePlayerRecord extends GamePlayerRecord
{
    override public function setup (occInfo :OccupantInfo) :void
    {
        super.setup(occInfo);

        _subscriber = (occInfo is MsoyUserOccupantInfo) &&
            MsoyUserOccupantInfo(occInfo).isSubscriber();
    }

    override public function getExtraInfo () :Object
    {
        // for now, we just return _subscriber, but we could wrap this up in an object with
        // other attrs
        return _subscriber;
    }

    /** Is this player a subscriber? */
    protected var _subscriber :Boolean;
}
}
