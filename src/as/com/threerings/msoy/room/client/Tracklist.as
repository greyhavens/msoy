//
// $Id$

package com.threerings.msoy.room.client {

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.ui.MsoyAudioDisplay;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.util.F;
import flash.events.Event;
import mx.containers.HBox;
import mx.core.ClassFactory;
import mx.containers.VBox;
import mx.events.FlexEvent;

import com.threerings.util.Comparators;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.world.client.WorldController;
import com.threerings.flex.CommandButton;
import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.flex.DSetList;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.flex.FlexUtil;
import com.threerings.msoy.room.data.MsoyScene;

public class Tracklist extends VBox
{
    public function Tracklist (ctx :WorldContext, roomObj :RoomObject, scene :MsoyScene)
    {
        _ctx = ctx;
        _roomObj = roomObj;
        _scene = scene;
    }

    override protected function createChildren () :void
    {
        var cf :ClassFactory = new ClassFactory(PlaylistRenderer);
        cf.properties = { wctx: _ctx, roomObj: _roomObj, djMode: true };
        _playlist = new DSetList(cf, Comparators.compareComparables);
        _playlist.percentWidth = 100;
        _playlist.height = 100;
        addChild(_playlist);

        addEventListener(Event.ADDED_TO_STAGE, function (..._) :void {
            _playlist.init(_ctx.getMemberObject(), MemberObject.TRACKS);
        });
        addEventListener(Event.REMOVED_FROM_STAGE, F.adapt(_playlist.shutdown));

        // HACK: Can't get percentWidth working properly, fixed width for now
        this.width = MsoyAudioDisplay.WIDTH;
    }

    protected var _ctx :WorldContext;
    protected var _roomObj :RoomObject;
    protected var _scene :MsoyScene;
    protected var _playlist :DSetList;
}
}
