//
// $Id#

package com.threerings.msoy.ui {

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.MouseEvent;

import com.threerings.util.Log;
import com.threerings.util.ValueEvent;

import com.threerings.flash.media.AudioPlayer;
import com.threerings.flash.media.MediaPlayerCodes;

/**
 * The msoy-skinned audio display.
 */
public class MsoyAudioDisplay extends Sprite
{
    public static const WIDTH :int = 320;

    public static const HEIGHT :int = MediaControls.HEIGHT;

    /**
     * Create.
     */
    public function MsoyAudioDisplay (player :AudioPlayer, commentCallback :Function = null)
    {
        _player = player;
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
        addChild(_controls);

        // TODO!
    }

    protected const log :Log = Log.getLog(this);

    protected var _player :AudioPlayer;

    protected var _controls :MediaControls;
}
}
