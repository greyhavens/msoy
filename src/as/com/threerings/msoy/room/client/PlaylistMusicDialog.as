//
// $Id$

package com.threerings.msoy.room.client {

import flash.geom.Point;

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
public class PlaylistMusicDialog extends MusicDialog
{
    public function PlaylistMusicDialog (
        ctx :WorldContext, near :Point = null, roomObj :RoomObject = null, scene :MsoyScene = null)
    {
        super(ctx, near);
        _roomObj = roomObj;
        _scene = scene;
    }

    override public function close () :void
    {
        super.close();
        _playList.shutdown();
    }

    override protected function didOpen () :void
    {
        super.didOpen();
        if (_roomObj != null) {
            _playList.init(_roomObj, RoomObject.PLAYLIST, RoomObject.PLAY_COUNT);
            callLater(scrollToCurrent);
        }
    }

    protected function scrollToCurrent () :void
    {
        _playList.scrollToKey(new ItemIdent(Item.AUDIO, _roomObj.currentSongId));
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

        var titleBox :HBox = new HBox();
        titleBox.percentWidth = 100;
        titleBox.addChild(FlexUtil.createSpacer(10, 10));
        titleBox.addChild(FlexUtil.createLabel(
            Msgs.WORLD.get("l.playlist_" + _scene.getPlaylistControl()), "playlistTitle"));
        if ((_scene.getPlaylistControl() == MsoySceneModel.ACCESS_EVERYONE) ||
                _scene.canManage(WorldContext(_ctx).getMemberObject())) {
            titleBox.addChild(FlexUtil.createSpacer(20, 10));
            titleBox.addChild(new CommandButton(Msgs.WORLD.get("b.add_music"),
                WorldController.VIEW_STUFF, Item.AUDIO));
        }
        addChild(titleBox);

        var cf :ClassFactory = new ClassFactory(PlaylistRenderer);
        cf.properties = { wctx: _ctx, roomObj: _roomObj };
        _playList = new DSetList(cf, Comparators.createReverse(Comparators.compareComparables));
        _playList.percentWidth = 100;
        _playList.height = 100;
        addChild(_playList);
    }

    protected var _roomObj :RoomObject;
    protected var _scene :MsoyScene;

    protected var _playList :DSetList;
}
}
