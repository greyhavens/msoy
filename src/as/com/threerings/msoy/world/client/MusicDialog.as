//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.util.CommandEvent;

import com.threerings.flex.FlexWrapper;

import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MsoyAudioDisplay;

public class MusicDialog extends FloatingPanel
{
    public function MusicDialog (ctx :WorldContext)
    {
        super(ctx, Msgs.GENERAL.get("t.music"));
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

    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(new FlexWrapper(_display, true));
    }

    protected function handleMusicInfo () :void
    {
        CommandEvent.dispatch(this, WorldController.MUSIC_INFO);
    }

    protected var _display :MsoyAudioDisplay;
}
}
