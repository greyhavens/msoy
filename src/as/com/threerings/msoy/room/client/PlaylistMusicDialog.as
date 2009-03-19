//
// $Id$

package com.threerings.msoy.room.client {

import flash.geom.Point;

import mx.core.ClassFactory;
import mx.core.ScrollPolicy;

import mx.controls.HRule;
import mx.controls.List;

import com.threerings.util.Integer;

import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.item.data.all.Audio;

import com.threerings.msoy.world.client.MusicDialog;
import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.data.RoomObject;

/**
 * Extends the music dialog with playlist information.
 */
public class PlaylistMusicDialog extends MusicDialog
{
    public function PlaylistMusicDialog (ctx :WorldContext, roomObj :RoomObject, near :Point = null)
    {
        super(ctx, near);
        _roomObj = roomObj;

        _listener = new NamedListener(
            [ RoomObject.PLAYLIST, RoomObject.PLAY_COUNT ], updatePlaylist);
    }

    override public function close () :void
    {
        super.close();
        if (_roomObj != null) {
            _roomObj.removeListener(_listener);
        }
    }

    override protected function didOpen () :void
    {
        super.didOpen();
        if (_roomObj != null) {
            _roomObj.addListener(_listener);
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        if (_roomObj == null) {
            return; // nothing else to do: act like parent class
        }

        var rule :HRule = new HRule();
        rule.percentWidth = 100;
        addChild(rule);
        addChild(FlexUtil.createLabel(Msgs.WORLD.get("l.playlist")));

        var cf :ClassFactory = new ClassFactory(PlaylistRenderer);
        cf.properties = { wctx: _ctx, roomObj: _roomObj };

        _playList = new List();
        _playList.percentWidth = 100;
        _playList.height = 100;
        _playList.verticalScrollPolicy = ScrollPolicy.ON;
        _playList.selectable = false;
        _playList.itemRenderer = cf;
        addChild(_playList);
        updatePlaylist();
    }

    protected function updatePlaylist () :void
    {
        // get all the songs
        var songs :Array = _roomObj.playlist.toArray();
        songs.sort(function (song1 :Audio, song2 :Audio) :int {
            // TODO: playlist ordering
            return Integer.compare(song1.itemId, song2.itemId);
        });
        _playList.dataProvider = songs;
    }

    // TODO: listen on RoomObject, re-order, etc.

    protected var _roomObj :RoomObject;

    protected var _playList :List;

    protected var _listener :NamedListener;
}
}

import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.EventListener;
import com.threerings.presents.dobj.NamedEvent;

/**
 * Listens on a set of names for NamedEvents, calls the callback.
 */
class NamedListener
    implements EventListener
{
    public function NamedListener (names :Array, callback :Function)
    {
        _names = names;
        _callback = callback;
    }

    public function eventReceived (event :DEvent) :void
    {
        if ((event is NamedEvent) && (-1 != _names.indexOf(NamedEvent(event).getName()))) {
            _callback();
        }
    }

    protected var _names :Array;

    protected var _callback :Function;
}
