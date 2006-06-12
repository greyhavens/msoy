package com.threerings.msoy.ui {

import flash.events.MouseEvent;

import flash.geom.Matrix;
import flash.geom.Point;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import mx.events.EffectEvent;

import com.threerings.crowd.chat.data.ChatMessage;

import com.threerings.msoy.data.MediaData;
import com.threerings.msoy.data.MsoyOccupantInfo;
import com.threerings.msoy.world.data.MsoyLocation;

public class Avatar extends ScreenMedia
{
    public function Avatar (occInfo :MsoyOccupantInfo, loc :MsoyLocation)
    {
        super(occInfo.media);
        _occInfo = occInfo;

        sendMessage("setAction", "standing");
        setOrientation(loc.orient);

//        var matrix :Matrix = this.transform.matrix; // create a copy
//        matrix.b = .5;
//        //matrix.c = .5;
//        this.transform.matrix = matrix;

        var txt :TextField = new TextField();
        _label = txt;
        txt.textColor = 0xFFFF00;
        txt.wordWrap = false;
        txt.multiline = false;
        txt.restrict = "";
        txt.autoSize = TextFieldAutoSize.CENTER;
        txt.text = occInfo.username.toString();
        txt.x = 0;
        txt.y = 0;
        rawChildren.addChild(txt);
    }

    override public function get maxContentWidth () :int
    {
        return 250;
    }

    override public function get maxContentHeight () :int
    {
        return 400;
    }

    /**
     * Get a configured ChatBubble instance to use for rendering chat
     * from this avatar. The bubble will initially be empty.
     */
    public function createChatBubble () :ChatBubble
    {
        return ChatBubble.createInstance(_occInfo.bubbleType);
    }

    public function getBubblePopStyle () :int
    {
        return _occInfo.bubblePopStyle;
    }

    public function moveTo (destLoc :MsoyLocation) :void
    {
        // if there's already a move, kill it
        if (_move != null) {
            _move.cancel();
        }

        // set walking, and maybe change facing direction
        sendMessage("setAction", "walking");
        if (destLoc.x > loc.x) {
            setOrientation(0);
        } else if (destLoc.x < loc.x)  {
            setOrientation(180);
        }

        _move = new SceneMove(this);
        _move.src = this.loc;
        _move.dest = destLoc;
        _move.addEventListener(EffectEvent.EFFECT_END, moveStopped);

        _move.play();
    }

    protected function setOrientation (orient :int) :void
    {
        var left :Boolean = (orient >= 90 && orient < 270)
        sendMessage("setFacing", left ? "left" : "right");
    }

    override protected function updateContentDimensions (ww :int, hh :int) :void
    {
        super.updateContentDimensions(ww, hh);
        _label.width = ww;
    }

    protected function moveStopped (event :EffectEvent) :void
    {
        _move = null;
        sendMessage("setAction", "standing");
    }

    override public function moveCompleted (orient :Number) :void
    {
        super.moveCompleted(orient);
        setOrientation(int(orient));
    }

    override protected function mouseClick (event :MouseEvent) :void
    {
        //setFacing(!_left);
    }

    override public function isInteractive () :Boolean
    {
        return true;
    }

    protected var _occInfo :MsoyOccupantInfo;

    protected var _move :SceneMove;

    protected var _label :TextField;
}
}
