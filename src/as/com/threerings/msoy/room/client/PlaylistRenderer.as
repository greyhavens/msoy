//
// $Id$

package com.threerings.msoy.room.client {

import flash.events.MouseEvent;

import mx.containers.HBox;
import mx.controls.Label;
import mx.controls.scrollClasses.ScrollBar;

import com.threerings.util.CommandEvent;
import com.threerings.util.ValueEvent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.ui.MediaControls;
import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Audio;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.room.data.RoomObject;

public class PlaylistRenderer extends HBox
{
    public var wctx :WorldContext;
    public var roomObj :RoomObject;

    public function PlaylistRenderer ()
    {
        Prefs.events.addEventListener(Prefs.BLEEPED_MEDIA, handleBleepChange, false, 0, true);
    }

    override public function set data (value :Object) :void
    {
        super.data = value;
        if (value == null) {
            return;
        }

        var audio :Audio = Audio(value);
        var isPlayingNow :Boolean = (roomObj.currentSongId == audio.itemId);
        var isManager :Boolean = wctx.getMsoyController().canManagePlace();
        var canRemove :Boolean = isManager || (wctx.getMyId() == audio.ownerId);

        FlexUtil.setVisible(_playBtn, isManager);
        _playBtn.enabled = !isPlayingNow;
        _thumbnail.setMediaDesc(audio.getThumbnailMedia());
        updateName();
        if (audio.used.forAnything()) {
            _name.toolTip = Msgs.WORLD.get("i.manager_music");
        } else {
            var info :MemberInfo = roomObj.getMemberInfo(audio.ownerId);
            _name.toolTip = Msgs.WORLD.get("i.visitor_music",
                (info != null) ? info.username : Msgs.WORLD.get("m.none"));
        }
        _name.setStyle("fontWeight", isPlayingNow ? "bold" : "normal");
        FlexUtil.setVisible(_removeBtn, canRemove);
        _removeBtn.enabled = canRemove;
    }

    protected function updateName () :void
    {
        var audio :Audio = Audio(data);
        var isBleeped :Boolean = audio.audioMedia.isBleepable() &&
            (Prefs.isGlobalBleep() || Prefs.isMediaBleeped(audio.audioMedia.getMediaId()));
        if (isBleeped) {
            _name.text = Msgs.GENERAL.get("m.bleeped");
            _name.setStyle("color", "red");
        } else {
            _name.text = audio.name;
            _name.setStyle("color", "white");
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _playBtn = new CommandButton("\u25B6", doPlay);
        addChild(_playBtn);

        _thumbnail = MediaWrapper.createView(null, MediaDesc.QUARTER_THUMBNAIL_SIZE);
        addChild(_thumbnail);

        _name = FlexUtil.createLabel(null);
        // 70 pix for buttons/spacing
        // 30 pix: quarter thumbnail (20 pix) plus 10 pix of spacing.
        _name.width = MediaControls.WIDTH - ScrollBar.THICKNESS - 70 - 30;
        _name.addEventListener(MouseEvent.CLICK, handleInfoClicked);
        addChild(_name);

        _removeBtn = new CommandButton(null, doRemove);
        _removeBtn.styleName = "closeButton";
        addChild(_removeBtn);
    }

    protected function doPlay () :void
    {
        roomObj.roomService.jumpToSong(Audio(data).itemId,
            wctx.confirmListener(null, MsoyCodes.WORLD_MSGS, null, _playBtn));
    }

    protected function doRemove () :void
    {
        roomObj.roomService.modifyPlaylist(Audio(data).itemId, false,
            wctx.confirmListener(null, MsoyCodes.WORLD_MSGS));
        _removeBtn.enabled = false;
    }

    protected function handleBleepChange (event :ValueEvent) :void
    {
        var audio :Audio = Audio(data);
        if (audio != null && audio.audioMedia.isBleepable()) {
            const id :String = String(event.value[0]);
            if (id == Prefs.GLOBAL_BLEEP || id == audio.audioMedia.getMediaId()) {
                updateName();
            }
        }
    }

    protected function handleInfoClicked (event :MouseEvent) :void
    {
        var audio :Audio = Audio(data);
        CommandEvent.dispatch(this, MsoyController.AUDIO_CLICKED,
            [ audio.audioMedia, audio.getIdent() ]);
    }

    protected var _playBtn :CommandButton;
    protected var _thumbnail :MediaWrapper;
    protected var _name :Label;
    protected var _removeBtn :CommandButton;
}
}
