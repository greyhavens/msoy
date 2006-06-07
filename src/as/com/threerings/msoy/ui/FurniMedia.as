package com.threerings.msoy.ui {

import com.threerings.msoy.world.data.FurniData;

public class FurniMedia extends ScreenMedia
{
    public function FurniMedia (furni :FurniData)
    {
        super(furni.media);
    }

    // documentation inherited
    override protected function getHoverColor () :uint
    {
        return 0xe0e040; // yellow
    }
}
}
