package com.threerings.msoy.world.client {

import flash.events.MouseEvent;

import flash.geom.Matrix;
import flash.geom.Point;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import mx.events.EffectEvent;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.chat.data.ChatMessage;

import com.threerings.msoy.data.MediaData;
import com.threerings.msoy.data.MsoyOccupantInfo;
import com.threerings.msoy.world.data.MsoyLocation;

public class AvatarSprite extends MsoySprite
{
    public function AvatarSprite (occInfo :MsoyOccupantInfo, loc :MsoyLocation)
    {
        super(occInfo.media);

        var txt :TextField = new TextField();
        _label = txt;

        // set up our occupant info
        setOccupantInfo(occInfo);

        sendMessage("setAction", "standing");
        setOrientation(loc.orient);

//        var matrix :Matrix = this.transform.matrix; // create a copy
//        matrix.b = .5;
//        //matrix.c = .5;
//        this.transform.matrix = matrix;

        txt.wordWrap = false;
        txt.multiline = false;
        txt.restrict = "";
        txt.autoSize = TextFieldAutoSize.CENTER;
        txt.text = occInfo.username.toString();
        txt.x = 0;
        txt.y = 0;
        rawChildren.addChild(txt);
    }

    /**
     * Update the occupant info.
     */
    public function setOccupantInfo (occInfo :MsoyOccupantInfo) :void
    {
        _occInfo = occInfo;

        _label.textColor = getStatusColor(_occInfo.status);
    }

    /**
     * Get the oid of the body that this represents.
     */
    public function getOid () :int
    {
        return _occInfo.bodyOid;
    }

    protected function getStatusColor (status :int) :uint
    {
        switch (status) {
        case OccupantInfo.IDLE:
            return 0xFFFF00;

        case OccupantInfo.DISCONNECTED:
            return 0xFF0000;

        default:
            return 0x00FF00;
        }
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
     * Get the style of chat bubble to use for this occupant.
     */
    public function getBubbleStyle () :int
    {
        return _occInfo.bubbleStyle;
    }

    public function getBubblePopStyle () :int
    {
        return _occInfo.bubblePopStyle;
    }

    public function moveTo (destLoc :MsoyLocation, sceneWidth :int) :void
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
        _move.duration = destLoc.distance(this.loc) * sceneWidth * 6; // TODO
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
