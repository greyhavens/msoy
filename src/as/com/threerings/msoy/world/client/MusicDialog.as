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
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var audioDisp :MsoyAudioDisplay = new MsoyAudioDisplay(
            WorldContext(_ctx).getWorldController().getMusicPlayer(), handleMusicInfo);

        addChild(new FlexWrapper(audioDisp, true));
    }

    protected function handleMusicInfo () :void
    {
        CommandEvent.dispatch(this, WorldController.MUSIC_INFO);
    }
}
}
