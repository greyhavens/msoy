//
// $Id$

package com.threerings.msoy.room.client {

import flash.events.Event;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.core.ClassFactory;

import com.threerings.util.Comparators;
import com.threerings.util.F;

import com.threerings.flex.CommandButton;
import com.threerings.flex.DSetList;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.ui.MsoyAudioDisplay;
import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.client.WorldController;

public class Playlist extends VBox
{
    public function Playlist (ctx :WorldContext, roomObj :RoomObject, scene :MsoyScene)
    {
        _ctx = ctx;
        _roomObj = roomObj;
        _scene = scene;

        var cf :ClassFactory = new ClassFactory(PlaylistRenderer);
        cf.properties = { wctx: _ctx, roomObj: _roomObj, djMode: false };
        _playlist = new DSetList(cf, Comparators.createReverse(Comparators.compareComparables));
        _playlist.percentWidth = 100;
        _playlist.height = 100;

        var rightBox :HBox = new HBox();
        rightBox.percentWidth = 100;
        rightBox.setStyle("horizontalAlign", "right");
        if ((_scene.getPlaylistControl() == MsoySceneModel.ACCESS_EVERYONE) ||
                _scene.canManage(_ctx.getMemberObject())) {
            rightBox.addChild(new CommandButton(Msgs.WORLD.get("b.add_music"),
                WorldController.VIEW_STUFF, Item.AUDIO));
            rightBox.addChild(FlexUtil.createSpacer());
        }

        _titleBox = new HBox();
        _titleBox.percentWidth = 100;
        _titleBox.addChild(FlexUtil.createLabel(
            Msgs.WORLD.get("l.playlist_" + _scene.getPlaylistControl()), "playlistTitle"));
        _titleBox.addChild(rightBox);

        addEventListener(Event.ADDED_TO_STAGE, function (..._) :void {
            _playlist.init(_roomObj, RoomObject.PLAYLIST, RoomObject.PLAY_COUNT);
            callLater(scrollToCurrent);
        });
        addEventListener(Event.REMOVED_FROM_STAGE, F.adapt(_playlist.shutdown));
    }

    override protected function createChildren () :void
    {
        addChild(_titleBox);
        addChild(_playlist);

        // HACK: Can't get percentWidth working properly, fixed width for now
        this.width = MsoyAudioDisplay.WIDTH;
    }

    protected function scrollToCurrent () :void
    {
        _playlist.scrollToKey(new ItemIdent(Item.AUDIO, _roomObj.currentSongId));
    }

    protected var _ctx :WorldContext;
    protected var _roomObj :RoomObject;
    protected var _scene :MsoyScene;
    protected var _playlist :DSetList;
    protected var _titleBox :HBox;
}
}
