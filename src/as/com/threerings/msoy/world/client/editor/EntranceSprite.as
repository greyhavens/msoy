//
// $Id$

package com.threerings.msoy.world.client.editor {

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.data.MsoyLocation;

public class EntranceSprite extends FurniSprite
{
    /** The sprite image used for positioning the entrance location. */
    [Embed(source="../../../../../../../../rsrc/media/entrance.png")]
    public static const MEDIA_CLASS :Class;

    public function EntranceSprite (location :MsoyLocation)
    {
        // fake furni data for the fake sprite
        var furniData :EntranceFurniData = new EntranceFurniData();
        furniData.media = new MediaDesc();
        furniData.loc = location;
        super(furniData);

        setMediaClass(MEDIA_CLASS);
        setLocation(location);
    }
}
}
