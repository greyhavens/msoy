//
// $Id$

package com.threerings.msoy.room.client {

import flash.geom.Point;

import mx.containers.VBox;
import mx.controls.HRule;
import mx.core.UIComponent;

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

        addOpenCallback(function () :void {
            _roomObj.djsEntryAdded.add(invalidate);
            _roomObj.djsEntryRemoved.add(invalidate);
            _roomObj.djsChanged.add(invalidate);
        });
        addCloseCallback(function () :void {
            _roomObj.djsEntryAdded.remove(invalidate);
            _roomObj.djsEntryRemoved.remove(invalidate);
            _roomObj.djsChanged.remove(invalidate);
        });
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
        invalidate();
    }

    protected function invalidate () :void
    {
        var djMode :Boolean = _roomObj.inDjMode();

        if (djMode) {
            if (_playlist != null) {
                _extras.removeChild(_playlist);
                _playlist = null;
            }
            if (_djList == null) {
                _djList = new DjList(WorldContext(_ctx), _roomObj);
                _extras.addChild(_djList);
                _djList.validateNow();
            }

            var amDj :Boolean = _roomObj.djs.containsKey(_ctx.getMyId());
            if (amDj && _tracklist == null) {
                _tracklist = new Tracklist(WorldContext(_ctx), _roomObj);
                _extras.addChild(_tracklist);
                _tracklist.validateNow();

            } else if (!amDj && _tracklist != null) {
                _extras.removeChild(_tracklist);
                _tracklist = null;
            }

        } else {
            if (_djList != null) {
                _extras.removeChild(_djList);
                _djList = null;
            }
            if (_tracklist != null) {
                _extras.removeChild(_tracklist);
                _tracklist = null;
            }
            if (_playlist == null) {
                _playlist = new Playlist(WorldContext(_ctx), _roomObj, _scene);
                _extras.addChild(_playlist);
                _playlist.validateNow();
            }
        }
    }

    protected var _roomObj :RoomObject;
    protected var _scene :MsoyScene;

    protected var _extras :VBox;
    protected var _playlist :UIComponent;
    protected var _djList :UIComponent;
    protected var _tracklist :UIComponent;
}
}
