package com.threerings.msoy.world.client {

import flash.events.MouseEvent;

import flash.geom.Point;

import mx.controls.Label;

import mx.events.EffectEvent;

import mx.effects.EffectInstance;

import com.threerings.mx.events.CommandEvent;

import com.threerings.util.MenuUtil;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.chat.data.ChatMessage;

import com.threerings.msoy.client.ContextMenuProvider;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.data.MemberInfo;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;

public class AvatarSprite extends MsoySprite
    implements ContextMenuProvider
{
    public function AvatarSprite (ctx :MsoyContext, occInfo :MemberInfo)
    {
        super(occInfo.media);

        _label = new Label();
        _label.setStyle("textAlign", "center");
        addChild(_label);

        // set up our occupant info
        setOccupantInfo(ctx, occInfo);

        sendMessage("setAction", (_move == null) ? "standing" : "walking");
    }

    override protected function setup (desc :MediaDesc) :void
    {
        super.setup(desc);

/*
        if (_label != null) {
            // ensure the name label always stays on top
            rawChildren.setChildIndex(_label, rawChildren.numChildren - 1);
        }
*/
    }

    /**
     * Update the occupant info.
     */
    public function setOccupantInfo (ctx :MsoyContext, occInfo :MemberInfo) :void
    {
        _occInfo = occInfo;

        if (!_occInfo.media.equals(_desc)) {
            setup(_occInfo.media);
        }

        _label.setStyle("color", getStatusColor(_occInfo.status));
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

    // from ContextMenuProvider
    public function populateContextMenu (menuItems :Array) :void
    {
        if (_occInfo.avatarId != 0) {
            menuItems.unshift(MenuUtil.createControllerMenuItem(
                Msgs.GENERAL.get("b.view_item"), MsoyController.VIEW_ITEM,
                [ Item.AVATAR, _occInfo.avatarId ]));
        }
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

    public function moveTo (destLoc :MsoyLocation, scene :MsoyScene) :void
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

        _move = new SceneMove(this, scene, this.loc, destLoc);
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
        CommandEvent.dispatch(this, RoomController.AVATAR_CLICKED, this);
    }

    override public function isInteractive () :Boolean
    {
        return true;
    }

    override public function hasAction () :Boolean
    {
        return true;
    }

    protected var _occInfo :MemberInfo;

    protected var _move :SceneMove;

    protected var _label :Label;
}
}
