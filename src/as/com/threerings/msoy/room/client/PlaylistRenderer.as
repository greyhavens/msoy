//
// $Id$

package com.threerings.msoy.room.client {

import flash.events.MouseEvent;

import mx.containers.HBox;
import mx.controls.Label;
import mx.controls.scrollClasses.ScrollBar;

import com.threerings.util.CommandEvent;
import com.threerings.util.NamedValueEvent;

import com.threerings.orth.data.MediaDesc;
import com.threerings.orth.data.MediaDescSize;
import com.threerings.orth.ui.MediaWrapper;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.data.Track;
import com.threerings.msoy.ui.MediaControls;
import com.threerings.msoy.world.client.WorldContext;

public class PlaylistRenderer extends HBox
{
    public var wctx :WorldContext;
    public var roomObj :RoomObject;

    public function get djMode () :Boolean { return _djMode; }
    public function set djMode (djMode :Boolean) :void
    {
        _djMode = djMode;
        _playBtn.label = djMode ? "\u2B06" : "\u25B6";
    }

    public function PlaylistRenderer ()
    {
        Prefs.events.addEventListener(Prefs.BLEEPED_MEDIA, handleBleepChange, false, 0, true);

        _playBtn = new CommandButton(null, doPlay);
        _thumbnail = MediaWrapper.createView(null, MediaDescSize.QUARTER_THUMBNAIL_SIZE);
        _removeBtn = new CommandButton(null, doRemove);
        _removeBtn.styleName = "closeButton";
        _name = FlexUtil.createLabel(null);

        // 70 pix for buttons/spacing
        // 30 pix: quarter thumbnail (20 pix) plus 10 pix of spacing.
        _name.width = MediaControls.WIDTH - ScrollBar.THICKNESS - 70 - 30;
        _name.addEventListener(MouseEvent.CLICK, handleInfoClicked);
    }

    public function showRemoveButton (show :Boolean) :void
    {
        _removeBtn.visible = show;
    }

    override public function set data (value :Object) :void
    {
        super.data = value;
        if (value == null) {
            return;
        }

        var audio :Audio = getAudio();
        var isManager :Boolean = wctx.getMsoyController().canManagePlace();
        var canRemove :Boolean = djMode || isManager || (wctx.getMyId() == audio.ownerId);

        if (djMode) {
            var minOrder :int = int.MAX_VALUE;
            for each (var track :Track in wctx.getMemberObject().tracks.toArray()) {
                if (track.order < minOrder) {
                    minOrder = track.order;
                }
            }
            var topTrack :Boolean = (Track(data).order == minOrder);
            _playBtn.visible = !topTrack;
            _playBtn.toolTip = Msgs.WORLD.get("i.move_to_top");
            showRemoveButton(false);

            // TODO(bruno): Why doesn't this work?
            // setStyle("backgroundColor", topTrack ? 0x54a9da : undefined);

        } else {
            var isPlayingNow :Boolean = (roomObj.currentSongId == audio.itemId);

            FlexUtil.setVisible(_playBtn, isManager);
            _playBtn.enabled = !isPlayingNow;
            if (audio.used.forAnything()) {
                _name.toolTip = Msgs.WORLD.get("i.manager_music");
            }
            _name.setStyle("fontWeight", isPlayingNow ? "bold" : "normal");

            FlexUtil.setVisible(_removeBtn, canRemove);
        }

        _removeBtn.enabled = canRemove;

        updateName();
        _thumbnail.setMediaDesc(audio.getThumbnailMedia());
    }

    protected function getAudio () :Audio
    {
        if (data == null) {
            return null;
        }
        return djMode ? Track(data).audio : Audio(data);
    }

    protected function updateName () :void
    {
        var audio :Audio = getAudio();
        var isBleeped :Boolean = audio.audioMedia.isBleepable() &&
            (Prefs.isGlobalBleep() || Prefs.isMediaBleeped(audio.audioMedia.getMediaId()));
        if (isBleeped) {
            _name.text = Msgs.GENERAL.get("m.bleeped");
            _name.setStyle("color", "red");
        } else {
            _name.text = audio.name;
            _name.setStyle("color", "black");
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        addChild(_playBtn);
        addChild(_thumbnail);
        addChild(_name);

        addChild(_removeBtn);
    }

    protected function doPlay () :void
    {
        var itemId :int = getAudio().itemId;
        if (djMode) {
            // Move this track to the top of the queue
            roomObj.roomService.setTrackIndex(itemId, 0);
        } else {
            roomObj.roomService.jumpToSong(itemId,
                wctx.confirmListener(null, MsoyCodes.WORLD_MSGS, null, _playBtn));
        }
    }

    protected function doRemove () :void
    {
        roomObj.roomService.addOrRemoveSong(getAudio().itemId, false,
            wctx.confirmListener(null, MsoyCodes.WORLD_MSGS));
        _removeBtn.enabled = false;
    }

    protected function handleBleepChange (event :NamedValueEvent) :void
    {
        var audio :Audio = getAudio();
        if (audio != null && audio.audioMedia.isBleepable() &&
                (event.name == Prefs.GLOBAL_BLEEP || event.name == audio.audioMedia.getMediaId())) {
            updateName();
        }
    }

    protected function handleInfoClicked (event :MouseEvent) :void
    {
        var audio :Audio = getAudio();
        CommandEvent.dispatch(this, MsoyController.AUDIO_CLICKED,
            [ audio.audioMedia, audio.getIdent() ]);
    }

    protected var _playBtn :CommandButton;
    protected var _thumbnail :MediaWrapper;
    protected var _name :Label;
    protected var _removeBtn :CommandButton;
    protected var _djMode :Boolean;
}
}
