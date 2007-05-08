package com.threerings.msoy.world.client {

import com.threerings.msoy.world.data.DecorData;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.RoomCodes;

public class DecorSprite extends FurniSprite
{
    /**
     * Construct a new DecorSprite.
     */
    public function DecorSprite (decor :DecorData)
    {
        super(decor);
    }
    
    override public function getRoomLayer () :int
    {
        return RoomCodes.DECOR_LAYER;
    }

    public function getDecorData () :DecorData
    {
        return _furni as DecorData;
    }

    override public function update (furni :FurniData) :void
    {
        super.update(furni);
        checkAlpha();
    }

    override public function getDesc () :String
    {
        return "m.decor";
    }

    override public function getToolTipText () :String
    {
        // no tooltip
        return null;
    }

    override public function setEditing (editing :Boolean) :void
    {
        super.setEditing(editing);
        checkAlpha();
    }

    // documentation inherited
    override public function hasAction () :Boolean
    {
        return false; // decor has no action
    }

    // documentation inherited
    override public function capturesMouse () :Boolean
    {
        return false; // decor does not capture mouse actions
    }

    // documentation inherited
    override public function hitTestPoint (
        x :Number, y :Number, shapeFlag :Boolean = false) :Boolean
    {
        return false; // decor never captures mouse clicks.
    }

    override public function toString () :String
    {
        var decor :DecorData = getDecorData();
        if (decor != null) {
            return "DecorSprite[" + decor.itemType + ":" + decor.itemId + ":"
                + decor.width + "x" + decor.depth + "/" + decor.media.getMediaPath() + "]";
        } else {
            return "DecorSprite[null]";
        }
    }

    /**
     * Configure our alpha differently depending on whether or not we're editing.
     */
    protected function checkAlpha () :void
    {
        alpha = _editing ? .4 : 1;
    }
}
}
