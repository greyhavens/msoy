//
// $Id$

package com.threerings.msoy.world.client.editor {

import com.threerings.msoy.world.client.MsoySprite;

public class EntranceSprite extends MsoySprite
{
    /** The sprite image used for positioning the entrance location. */
    [Embed(source="../../../../../../../../rsrc/media/entrance.png")]
    public static const MEDIA_CLASS :Class;

    public function EntranceSprite ()
    {
        super(null, null);
        setMediaClass(MEDIA_CLASS);
    }
}
}
