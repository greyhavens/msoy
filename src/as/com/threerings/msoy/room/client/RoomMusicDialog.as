//
// $Id$

package com.threerings.msoy.room.client {

import flash.geom.Point;

import mx.containers.VBox;
import mx.containers.HBox;
import mx.controls.HRule;
import mx.core.ClassFactory;

import com.threerings.util.Comparators;

import com.threerings.flex.CommandButton;
import com.threerings.flex.DSetList;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.world.client.MusicDialog;
import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.client.WorldController;

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
        _extras.addChild(new Playlist(WorldContext(_ctx), _roomObj, _scene));

        _extras.addChild(new Tracklist(WorldContext(_ctx), _roomObj, _scene));
    }

    protected var _roomObj :RoomObject;
    protected var _scene :MsoyScene;

    protected var _extras :VBox;
}
}
