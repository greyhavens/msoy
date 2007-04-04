package com.threerings.msoy.world.client {

import com.threerings.msoy.world.data.DecorData;
import com.threerings.msoy.world.data.FurniData;

public class DecorSprite extends FurniSprite
{
    /**
     * Construct a new DecorSprite.
     */
    public function DecorSprite (decor :DecorData)
    {
        super(decor);
    }
    
    override public function isIncludedInLayout () :Boolean
    {
        return false; // because it's a background
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
        alpha = _editing ? .65 : 1;
    }
}
}
