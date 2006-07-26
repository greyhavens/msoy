package com.threerings.msoy.world.client {

import flash.events.MouseEvent;

import flash.geom.Matrix;
import flash.geom.Point;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import mx.events.EffectEvent;

import mx.effects.EffectInstance;

import com.threerings.mx.events.CommandEvent;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.chat.data.ChatMessage;

import com.threerings.msoy.data.MediaData;
import com.threerings.msoy.data.MemberInfo;
import com.threerings.msoy.world.data.MsoyLocation;

public class AvatarSprite extends MsoySprite
{
    public function AvatarSprite (occInfo :MemberInfo)
    {
        super(occInfo.media);

        var txt :TextField = new TextField();
        txt.wordWrap = false;
        txt.multiline = false;
        txt.selectable = false;
        txt.restrict = "";
        txt.autoSize = TextFieldAutoSize.CENTER;
        txt.x = 0;
        txt.y = 0;
        _label = txt;
        rawChildren.addChild(txt);

        // set up our occupant info
        setOccupantInfo(occInfo);

        sendMessage("setAction", (_move == null) ? "standing" : "walking");
    }

    override protected function setup (desc :MediaData) :void
    {
        super.setup(desc);

        if (_label != null) {
            // ensure the name label always stays on top
            rawChildren.setChildIndex(_label, rawChildren.numChildren - 1);
        }
    }

    /**
     * Update the occupant info.
     */
    public function setOccupantInfo (occInfo :MemberInfo) :void
    {
        _occInfo = occInfo;

        if (!_occInfo.media.equals(_desc)) {
            setup(_occInfo.media);
        }

        _label.textColor = getStatusColor(_occInfo.status);
        _label.text = occInfo.username.toString();
    }

    /**
     * Get the occupant info for this avatar.
     */
    public function getOccupantInfo () :MemberInfo
    {
        return _occInfo;
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
        return 300;
    }

    override public function get maxContentHeight () :int
    {
        return 400;
    }

    /**
     * Get the style of chat bubble to use for this occupant.
     */
    public function getChatStyle () :int
    {
        return _occInfo.chatStyle;
    }

    public function getChatPopStyle () :int
    {
        return _occInfo.chatPopStyle;
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

        var dx :Number = destLoc.x - loc.x;
        var dy :Number = destLoc.y - loc.y;
        var dz :Number = destLoc.z - loc.z;
        _move.duration = Math.sqrt(36 * dx * dx * sceneWidth * sceneWidth +
            dy * dy * 4 + dz * dz * 9);

        _move.play();
    }

    /**
     * @return true if we're moving.
     */
    public function isMoving () :Boolean
    {
        return (_move != null);
    }

    public function stopMove () :void
    {
        if (_move != null) {
            _move.cancel();
            _move = null;
        }
    }

    override public function shutdown (completely :Boolean = true) :void
    {
        if (completely) {
            stopMove();
        }

        super.shutdown(completely);
    }

    public function setOrientation (orient :int) :void
    {
        var left :Boolean = (orient >= 90 && orient < 270)
        sendMessage("setFacing", left ? "left" : "right");
    }

    override protected function updateContentDimensions (ww :int, hh :int) :void
    {
        super.updateContentDimensions(ww, hh);
        _label.width = ww;
    }

    override public function moveCompleted (orient :Number) :void
    {
        super.moveCompleted(orient);
        setOrientation(int(orient));

        _move = null;
        sendMessage("setAction", "standing");

        if (parent is RoomView) {
            (parent as RoomView).moveFinished(this);
        }
    }

    override protected function mouseClick (event :MouseEvent) :void
    {
        // let's just post a command to our controller
        dispatchEvent(new CommandEvent(RoomController.AVATAR_CLICKED, this));
    }

    override public function isInteractive () :Boolean
    {
        return true;
    }

    override public function hasAction () :Boolean
    {
        return true;
    }

    override protected function contentDimensionsUpdated () :void
    {
        super.contentDimensionsUpdated();

        // ensure the name label is centered and reasonably located
        _label.width = _w;
        _label.x = 0;
    }

    protected var _occInfo :MemberInfo;

    protected var _move :SceneMove;

    protected var _label :TextField;
}
}
