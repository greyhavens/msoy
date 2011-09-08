//
// $Id$

package com.threerings.msoy.room.client {

import flash.events.Event;

import mx.containers.VBox;
import mx.core.ClassFactory;
import mx.events.DragEvent;
import mx.events.ListEvent;

import com.threerings.util.Comparators;
import com.threerings.util.F;

import com.threerings.flex.DSetList;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.data.Track;
import com.threerings.msoy.ui.MsoyAudioDisplay;
import com.threerings.msoy.world.client.WorldContext;

public class Tracklist extends VBox
{
    public function Tracklist (ctx :WorldContext, roomObj :RoomObject)
    {
        _ctx = ctx;
        _roomObj = roomObj;

        var cf :ClassFactory = new ClassFactory(PlaylistRenderer);
        cf.properties = { wctx: _ctx, roomObj: _roomObj, djMode: true };
        _playlist = new DSetList(cf, Comparators.compareComparables);
        _playlist.percentWidth = 100;
        _playlist.height = 200;

        // Drag and drop to reorder songs
        var dragIndex :int = -1;
        _playlist.selectable = true;
        _playlist.dragEnabled = true;
        _playlist.dragMoveEnabled = true;
        _playlist.dropEnabled = true;
        _playlist.addEventListener(DragEvent.DRAG_DROP, function (event :DragEvent) :void {
            var dropIndex :int = _playlist.calculateDropIndex();
            if (dragIndex != -1 && dragIndex != dropIndex) {
                var audioId :int = Track(_playlist.dataProvider.getItemAt(dragIndex)).audio.itemId;
                _roomObj.roomService.setTrackIndex(audioId, dropIndex);
            }
        });
        _playlist.addEventListener(ListEvent.CHANGE, function (..._) :void {
            // Flex dragging requires selectable=true, but selectable songs don't make sense. So,
            // clear the list selection, but keep a copy of the index so we know where the drag
            // started from.
            dragIndex = _playlist.selectedIndex;
            _playlist.selectedIndex = -1;
        });

        // Show the remove button only on mouse over
        _playlist.addEventListener(ListEvent.ITEM_ROLL_OVER, function (event :ListEvent) :void {
            PlaylistRenderer(event.itemRenderer).showRemoveButton(true);
        });
        _playlist.addEventListener(ListEvent.ITEM_ROLL_OUT, function (event :ListEvent) :void {
            PlaylistRenderer(event.itemRenderer).showRemoveButton(false);
        });

        addEventListener(Event.ADDED_TO_STAGE, function (..._) :void {
            _playlist.init(_ctx.getMemberObject(), MemberObject.TRACKS);
        });
        addEventListener(Event.REMOVED_FROM_STAGE, F.adapt(_playlist.shutdown));
    }

    override protected function createChildren () :void
    {
        addChild(_playlist);

        // HACK: Can't get percentWidth working properly, fixed width for now
        this.width = MsoyAudioDisplay.WIDTH;
    }

    protected var _ctx :WorldContext;
    protected var _roomObj :RoomObject;
    protected var _playlist :DSetList;
}
}
