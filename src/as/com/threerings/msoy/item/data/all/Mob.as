//
// $Id$

package com.threerings.msoy.item.data.all {

/**
 * Contains the runtime data for a Mob (mobile-object, monster) item.
 */
public class Mob extends SubItem
{
    public function Mob ()
    {
    }

    // from Item
    override public function getType () :int
    {
        return MOB;
    }

    // from Item
    override public function getPreviewMedia () :MediaDesc
    {
        return getFurniMedia();
    }

    // from Item
    override protected function getDefaultFurniMedia () :MediaDesc
    {
        return null; // there is no default
    }
}
}
