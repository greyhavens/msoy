package com.threerings.msoy.ui {

import com.threerings.msoy.world.data.FurniData;

public class FurniMedia extends ScreenMedia
{
    public function FurniMedia (furni :FurniData)
    {
        super(furni.media);
    }

    override public function get maxContentWidth () :int
    {
        return 2000;
    }

    override public function get maxContentHeight () :int
    {
        return 1000;
    }

    // documentation inherited
    override protected function isInteractive () :Boolean
    {
        return _desc.isInteractive();
    }

    // documentation inherited
    override protected function getHoverColor () :uint
    {
        return 0xe0e040; // yellow
    }
}
}
