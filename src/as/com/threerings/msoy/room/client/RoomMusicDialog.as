//
// $Id$

package com.threerings.msoy.room.client {

import flash.geom.Point;

import mx.containers.VBox;
import mx.controls.HRule;

import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.world.client.MusicDialog;
import com.threerings.msoy.world.client.WorldContext;

/**
 * Extends the music dialog with playlist information.
 */
public class RoomMusicDialog extends MusicDialog
{
    public function RoomMusicDialog (
        ctx :WorldContext, near :Point = null, roomObj :RoomObject = null, scene :MsoyScene = null)
    {
        super(ctx, near);
        _roomObj = roomObj;
        _scene = scene;
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        if (_roomObj == null) {
            return; // nothing else to do: act like parent class
        }

        // TODO: for now we disable seeking to avoid fucking around.
        _display.getControls().setSeekAllowed(false);

        var rule :HRule = new HRule();
        rule.percentWidth = 100;
        addChild(rule);

        addChild(_extras = new VBox());

        var wctx :WorldContext = WorldContext(_ctx);
        _extras.addChild(new Playlist(wctx, _roomObj, _scene));
        _extras.addChild(new DjList(wctx, _roomObj));
        _extras.addChild(new Tracklist(wctx, _roomObj, _scene));
    }

    protected var _roomObj :RoomObject;
    protected var _scene :MsoyScene;

    protected var _extras :VBox;
}
}
