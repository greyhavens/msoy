//
// $Id$

package com.threerings.msoy.item.data.all {

/**
 * Contains the runtime data for a Prop item. A prop is smart furniture that is associated with an
 * AVRG.
 */
public class Prop extends Item
{
    public function Prop ()
    {
    }

    // from Item
    override public function getType () :int
    {
        return PROP;
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
