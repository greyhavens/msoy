//
// $Id$

package com.threerings.msoy.world.client {

import flash.geom.Point;

import com.threerings.util.CommandEvent;

import com.threerings.flex.FlexWrapper;

import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MsoyAudioDisplay;

public class MusicDialog extends FloatingPanel
{
    public function MusicDialog (ctx :WorldContext, near :Point = null)
    {
        super(ctx, Msgs.GENERAL.get("t.music"));
        _near = near;

        showCloseButton = true;
        setStyle("paddingTop", 0);
        setStyle("paddingRight", 0);
        setStyle("paddingBottom", 0);
        setStyle("paddingLeft", 0);

        _display = new MsoyAudioDisplay(ctx.getWorldController().getMusicPlayer(), handleMusicInfo);
    }

    override public function close () :void
    {
        _display.unhook();
        super.close();
    }

    override protected function didOpen () :void
    {
        if (_near != null) {
            x = _near.x;
            y = _near.y;
        }

        super.didOpen();
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(new FlexWrapper(_display, true));
    }

    protected function handleMusicInfo () :void
    {
        CommandEvent.dispatch(this, WorldController.MUSIC_INFO);
    }

    /** A point we should attempt to pop near. */
    protected var _near :Point;

    protected var _display :MsoyAudioDisplay;
}
}
