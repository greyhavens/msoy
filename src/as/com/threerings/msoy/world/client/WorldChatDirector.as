//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.util.MessageBundle;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.data.Scene;

import com.threerings.msoy.chat.client.MsoyChatDirector;
import com.threerings.msoy.data.all.RoomName;

/**
 * Extends our standard chat director with custom world bits.
 */
public class WorldChatDirector extends MsoyChatDirector
{
    public function WorldChatDirector (ctx :WorldContext)
    {
        super(ctx);

        var msg :MessageBundle = _msgmgr.getBundle(_bundle);
        registerCommandHandler(msg, "action", new AvatarActionHandler(ctx, false));
        registerCommandHandler(msg, "state", new AvatarActionHandler(ctx, true));
    }

    // from MsoyChatDirector
    override public function locationDidChange (place :PlaceObject) :void
    {
        super.locationDidChange(place);

        // subscribe to the new scene's channel, if we haven't already
        var scene :Scene = (_mctx as WorldContext).getSceneDirector().getScene();
        if (scene != null) {
            _chatTabs.locationDidChange(new RoomName(scene.getName(), scene.getId()));
        } else {
            _chatTabs.locationDidChange(null);
        }
    }
}
}
