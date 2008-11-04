//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.DisplayObject;
import flash.geom.Rectangle;

import com.threerings.util.CommandEvent;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.game.data.GameSummary;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.data.MemberInfo;

/**
 * Displays a sprite for a member in a scene.
 */
public class MemberSprite extends ActorSprite
{
    /**
     * Creates a sprite for the supplied member.
     */
    public function MemberSprite (ctx :WorldContext, occInfo :MemberInfo)
    {
        super(ctx, occInfo);
    }

    /**
     * Get a list of the names of special actions that this avatar supports.
     */
    public function getAvatarActions () :Array
    {
        return validateActionsOrStates(callUserCode("getActions_v1") as Array);
    }

    /**
     * Get a list of the names of the states that this avatar may be in.
     */
    public function getAvatarStates () :Array
    {
        return validateActionsOrStates(callUserCode("getStates_v1") as Array);
    }

    /**
     * Get our preferred y value for positioning.
     */
    public function getPreferredY () :int
    {
        return int(Math.round(_scale * _preferredY));
    }

    /**
     * Informs the avatar that the player it represents just spoke.
     */
    public function performAvatarSpoke () :void
    {
        callUserCode("avatarSpoke_v1");
    }

    // from RoomElement
    override public function isImportant () :Boolean
    {
        // our own sprite is more important than the others
        return _ctx.getMyName().equals(_occInfo.username);
    }

    // from OccupantSprite
    override public function setOccupantInfo (newInfo :OccupantInfo) :void
    {
        super.setOccupantInfo(newInfo);

        // take care of setting up or changing our TableIcon
        var newSummary :GameSummary = (newInfo as MemberInfo).getGameSummary();
        if (_tableIcon != null && !_tableIcon.getGameSummary().equals(newSummary)) {
            _tableIcon.shutdown();
            _tableIcon = null;
        }
        if (_tableIcon == null && newSummary != null) {
            _tableIcon = new TableIcon(this, newSummary);
        }
    }

    // from ActorSprite
    override public function getDesc () :String
    {
        return "m.avatar";
    }

    // from MsoySprite
    override public function getHoverColor () :uint
    {
        return AVATAR_HOVER;
    }

    // from MsoySprite
    override public function hasAction () :Boolean
    {
        return true;
    }

    override protected function getSpecialProperty (name :String) :Object
    {
        switch (name) {
        case "member_id":
            return (_occInfo as MemberInfo).getMemberId();

        default:
            return super.getSpecialProperty(name);
        }
    }

    // from ActorSprite
    override public function toString () :String
    {
        return "MemberSprite[" + _occInfo.username + " (oid=" + _occInfo.bodyOid + ")]";
    }

    // from OccupantSprite
    override protected function configureDisplay (
        oldInfo :OccupantInfo, newInfo :OccupantInfo) :Boolean
    {
        // update our scale
        var oldScale :Number = _scale;
        _scale = (newInfo as MemberInfo).getScale();

        // see if our media has been updated
        var changed :Boolean = super.configureDisplay(oldInfo, newInfo);

        // if scale is the only thing that changed, make sure we report changedness
        if (!changed && oldScale != _scale) {
            scaleUpdated();
        }

        return changed || (oldScale != _scale);
    }

    // from OccupantSprite
    override protected function configureDecorations (
        oldInfo :OccupantInfo, newInfo :OccupantInfo) :Boolean
    {
        var reconfig :Boolean = super.configureDecorations(oldInfo, newInfo);

        // check whether our idle status has changed
        if ((newInfo.status == OccupantInfo.IDLE) == (_idleIcon == null)) {
            if (_idleIcon == null) {
                _idleIcon = (new IDLE_ICON() as DisplayObject);
                addDecoration(_idleIcon, {
                    weight: Number.MAX_VALUE / 2,
                    bounds: new Rectangle(0, 0, 50, 45)
                });
            } else {
                removeDecoration(_idleIcon);
                _idleIcon = null;
            }
            appearanceChanged();
            reconfig = false; // we took care of rearranging our decorations
        }

        return reconfig;
    }

    // from OccupantSprite
    override protected function isNameChangeRequired (
        oldInfo :OccupantInfo, newInfo :OccupantInfo) :Boolean
    {
        if (super.isNameChangeRequired(oldInfo, newInfo)) {
            return true;
        }
        if (oldInfo != null && MemberInfo(oldInfo).isGreeter() != MemberInfo(newInfo).isGreeter()) {
            return true;
        }
        return false;
    }

    // from OccupantSprite
    override protected function setNameStatus (occInfo :OccupantInfo) :void
    {
        _label.setStatus(occInfo.status, MemberInfo(occInfo).isGreeter());
    }

    // from ActorSprite
    override protected function postClickAction () :void
    {
        CommandEvent.dispatch(this, RoomController.AVATAR_CLICKED, this);
    }

    // from ActorSprite
    override protected function createBackend () :EntityBackend
    {
        _preferredY = 0;
        return new AvatarBackend();
    }

    /**
     * Verify that the actions or states received from usercode are not wacky.
     *
     * @return the cleaned Array, which may be empty things didn't check out.
     */
    protected function validateActionsOrStates (vals :Array) :Array
    {
        if (vals == null) {
            return [];
        }
        // If there are duplicates, non-strings, or strings.length > 64, then the
        // user has bypassed the checks in their Control and we just discard everything.
        for (var ii :int = 0; ii < vals.length; ii++) {
            if (!validateUserData(vals[ii], null)) {
                return [];
            }
            // reject duplicates
            for (var jj :int = 0; jj < ii; jj++) {
                if (vals[jj] === vals[ii]) {
                    return [];
                }
            }
        }
        // everything checks out...
        return vals;
    }

    /**
     * Routed from usercode by our backend.
     */
    internal function setPreferredYFromUser (prefY :int) :void
    {
        _preferredY = prefY;
    }

    /** The preferred y value, in pixels, when a user selects a location. */
    protected var _preferredY :int

    /** A decoration used when we're in a table in a lobby. */
    protected var _tableIcon :TableIcon;

    /** A decoration added when we've idled out. */
    protected var _idleIcon :DisplayObject;
}
}

import flash.display.Sprite;
import flash.events.MouseEvent;
import flash.filters.GlowFilter;
import flash.geom.Rectangle;
import flash.text.TextFieldAutoSize;
import flash.text.TextField;

import com.threerings.util.CommandEvent;

import com.threerings.flash.TextFieldUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.ui.ScalingMediaContainer;

import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.world.client.WorldController;

import com.threerings.msoy.room.client.MemberSprite;
import com.threerings.msoy.room.client.MsoySprite;

/**
 * A decoration used when this actor is at a table in a lobby.
 */
class TableIcon extends Sprite
{
    public function TableIcon (host :MemberSprite, gameSummary :GameSummary)
    {
        _host = host;
        _gameSummary = gameSummary;
        _gameThumb = ScalingMediaContainer.createView(gameSummary.getThumbMedia());
        _gameThumb.x = _gameThumb.maxW / -2; // position with 0 at center
        addChild(_gameThumb);

        var width :int = _gameThumb.maxW;
        var height :int = _gameThumb.maxH

        if (!gameSummary.avrGame) {
            var label :TextField = TextFieldUtil.createField(
                Msgs.GAME.get("m.join_game", gameSummary.name),
                {
                    autoSize: TextFieldAutoSize.CENTER,
                    textColor: 0xFFFFFF,
                    outlineColor: 0x000000
                },
                { size: 10, bold: true });
            label.x = label.width / -2; // position with 0 at center
            label.y = height; // no gap between icon!
            addChild(label);

            width = Math.max(width, label.width);
            height += label.height;
        }

        addEventListener(MouseEvent.MOUSE_OVER, handleMouseIn);
        addEventListener(MouseEvent.MOUSE_OUT, handleMouseOut);
        addEventListener(MouseEvent.CLICK, handleMouseClick);

        _host.addDecoration(this,
            // specify our bounds explicitly, as our width is centered at 0.
            { toolTip: gameSummary.name, bounds: new Rectangle(width/-2, 0, width, height) });
    }

    public function getGameSummary () :GameSummary
    {
        return _gameSummary;
    }

    public function shutdown () :void
    {
        _gameThumb.shutdown();
        _host.removeDecoration(this);
    }

    protected function handleMouseIn (... ignored) :void
    {
        this.filters = [ new GlowFilter(MsoySprite.GAME_HOVER, 1, 32, 32, 2) ];
    }

    protected function handleMouseOut (... ignored) :void
    {
        this.filters = null;
    }

    protected function handleMouseClick (... ignored) :void
    {
        if (_gameSummary.avrGame) {
            CommandEvent.dispatch(this, WorldController.JOIN_AVR_GAME, _gameSummary.gameId);
        } else {
            var memberId :int = (_host.getActorInfo().username as MemberName).getMemberId();
            CommandEvent.dispatch(
                this, WorldController.JOIN_PLAYER_GAME, [ _gameSummary.gameId, memberId ] );
        }
    }

    protected var _host :MemberSprite;
    protected var _gameSummary :GameSummary;
    protected var _gameThumb :ScalingMediaContainer;
}
