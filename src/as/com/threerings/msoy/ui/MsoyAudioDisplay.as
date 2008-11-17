//
// $Id#

package com.threerings.msoy.ui {

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.MouseEvent;

import flash.media.ID3Info;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import com.threerings.util.Log;
import com.threerings.util.ValueEvent;

import com.threerings.flash.TextFieldUtil;
import com.threerings.flash.media.AudioPlayer;
import com.threerings.flash.media.MediaPlayerCodes;

/**
 * The msoy-skinned audio display.
 */
public class MsoyAudioDisplay extends Sprite
{
    public static const WIDTH :int = MediaControls.WIDTH;

    public static const HEIGHT :int = MediaControls.HEIGHT + ((FIELD_HEIGHT + PAD) * 2);

    /**
     * Create.
     */
    public function MsoyAudioDisplay (player :AudioPlayer, commentCallback :Function = null)
    {
        _player = player;
        _player.addEventListener(MediaPlayerCodes.METADATA, handleMetadata);

        _controls = new MediaControls(player, commentCallback);

        configureUI();
    }

    override public function get width () :Number
    {
        return WIDTH;
    }

    override public function get height () :Number
    {
        return HEIGHT;
    }

    /**
     * Stop playing our video.
     */
    public function unload () :void
    {
        _controls.unload();
    }

    protected function configureUI () :void
    {
        var masker :Shape = new Shape();
        this.mask = masker;
        addChild(masker);
        var g :Graphics = masker.graphics;
        g.beginFill(0xFFFFFF);
        g.drawRect(0, 0, WIDTH, HEIGHT);
        g.endFill();

//        const text :String = "N/A"; // i18n is not happening here, this is non-flex.
        const textProps :Object = { selectable: false, x: PAD, width: WIDTH - (PAD * 2) };
        const formatProps :Object = { color: 0xFFFFFF, size: 12, bold: true, italic: true };

        _artist = TextFieldUtil.createField("unknown artist", textProps, formatProps);
        addChild(_artist);

        _song = TextFieldUtil.createField("unknown song", textProps, formatProps);
        _song.y += FIELD_HEIGHT + PAD;
        addChild(_song);

        _controls.y = (FIELD_HEIGHT + PAD) * 2; //HEIGHT - MediaControls.HEIGHT;
        addChild(_controls);
    }

    protected function handleMetadata (event :ValueEvent) :void
    {
        const id3 :Object = event.value;
        updateField(_artist, id3.artist as String);
        updateField(_song, id3.songName as String);
    }

    protected function updateField (field :TextField, info :String) :void
    {
        if (info != null) {
            TextFieldUtil.updateText(field, info);
            TextFieldUtil.updateFormat(field, { italic: false });
        }
    }

    protected static const FIELD_HEIGHT :int = 15;

    protected static const PAD :int = 5;

    protected const log :Log = Log.getLog(this);

    protected var _player :AudioPlayer;

    protected var _controls :MediaControls;

    protected var _artist :TextField;

    protected var _song :TextField;
}
}
