//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.msoy.world.data.MobInfo;

import flash.display.DisplayObject;

/**
 * Displays a MOB in the world.
 */
public class MobSprite extends OccupantSprite
{
    public function MobSprite (occInfo :MobInfo)
    {
        super(occInfo);
    }

    // from OccupantSprite
    override protected function configureDisplay (
        oldInfo :OccupantInfo, newInfo :OccupantInfo) :Boolean
    {
        var ominfo :MobInfo = (oldInfo as MobInfo), nminfo :MobInfo = (newInfo as MobInfo);
        if (ominfo == null || ominfo.getIdent() != nminfo.getIdent()) {
            var sprite :DisplayObject = null; // TODO: get from AVRG
            setMediaObject(sprite);
            return true;
        }
        return false;
    }
}
}
