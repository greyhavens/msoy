//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.DisplayObject;
import flash.geom.Rectangle;

import com.threerings.util.CommandEvent;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.room.client.OccupantSprite;
import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.world.client.WorldContext;

/**
 * Displays a sprite for a member in a scene.
 */
public class MemberSprite extends ActorSprite
{
    /**
     * Creates a sprite for the supplied member.
     */
    public function MemberSprite (ctx :WorldContext, occInfo :MemberInfo, extraInfo :Object)
    {
        super(ctx, occInfo, extraInfo);
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
    override public function setOccupantInfo (newInfo :OccupantInfo, extraInfo :Object) :void
    {
        super.setOccupantInfo(newInfo, extraInfo);

        // take care of setting up or changing our TableIcon
        var newSummary :GameSummary = (newInfo as MemberInfo).getGameSummary();
        if (_tableIcon != null && !_tableIcon.getGameSummary().equals(newSummary)) {
            _tableIcon.shutdown();
            _tableIcon = null;
        }
        if (_tableIcon == null && newSummary != null) {
            _tableIcon = new TableIcon(this, newSummary);
        }

        // take care of setting up or changing our PartyIcon
        var newId :int = (newInfo as MemberInfo).getPartyId();
        if (_partyIcon != null && (_partyIcon.id != newId)) {
            _partyIcon.shutdown();
            _partyIcon = null;
        }
        if (_partyIcon == null && newId != 0) {
            _partyIcon = new PartyIcon(this, newId, extraInfo);
        }
    }

    // from ActorSprite
    override public function getDesc () :String
    {
        return "m.avatar";
    }

    // from EntitySprite
    override public function getHoverColor () :uint
    {
        return AVATAR_HOVER;
    }

    // from EntitySprite
    override public function hasAction () :Boolean
    {
        return true;
    }

    override public function getMemberId () :int
    {
        return (_occInfo as MemberInfo).getMemberId();
    }

    // from OccupantSprite
    override protected function isNameChangeRequired (oldInfo :OccupantInfo,
                                                      newInfo :OccupantInfo) :Boolean
    {
        return super.isNameChangeRequired(oldInfo, newInfo) || // is true if oldInfo == null
            (MemberInfo(oldInfo).isSubscriber() != MemberInfo(newInfo).isSubscriber()) ||
            (MemberInfo(oldInfo).isAway() != MemberInfo(newInfo).isAway());
    }

    // from OccupantSprite
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
        if (isVisiblyIdle(newInfo as MemberInfo) == (_idleIcon == null)) {
            if (_idleIcon == null) {
                _idleIcon = (new IDLE_ICON() as DisplayObject);
                addDecoration(_idleIcon, {
                    weight: OccupantSprite.DEC_WEIGHT_IDLE,
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

    // from ActorSprite
    override protected function postClickAction () :void
    {
        CommandEvent.dispatch(_sprite, RoomController.AVATAR_CLICKED, this);
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

    // don't show our idleness if we're AFK
    protected function isVisiblyIdle (info :MemberInfo) :Boolean
    {
        return (info.status == OccupantInfo.IDLE) && !info.isAway();
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

    /** A decoration used when we're in a party. */
    protected var _partyIcon :PartyIcon;
}
}

import flash.geom.Rectangle;
import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import com.threerings.text.TextFieldUtil;

import com.threerings.orth.data.MediaDesc;
import com.threerings.orth.data.MediaDescSize;
import com.threerings.orth.ui.ScalingMediaDescContainer;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.room.client.EntitySprite;
import com.threerings.msoy.room.client.MemberSprite;
import com.threerings.msoy.room.client.OccupantSprite;
import com.threerings.msoy.ui.GlowSprite;
import com.threerings.msoy.world.client.WorldController;

/**
 * A decoration used when this actor is at a table in a lobby.
 */
class TableIcon extends GlowSprite
{
    public function TableIcon (host :MemberSprite, gameSummary :GameSummary)
    {
        _host = host;
        _gameSummary = gameSummary;
        var iconSize :int = gameSummary.avrGame ? MediaDescSize.HALF_THUMBNAIL_SIZE
                                                : MediaDescSize.THUMBNAIL_SIZE;
        _gameThumb = ScalingMediaDescContainer.createView(gameSummary.thumbMedia, iconSize);
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

        var cmd :Object;
        var arg :Object;
        if (gameSummary.avrGame) {
            cmd = WorldController.JOIN_AVR_GAME;
            arg = gameSummary.gameId;

        } else {
            var memberId :int = (host.getActorInfo().username as MemberName).getId();
            cmd = WorldController.JOIN_PLAYER_GAME;
            arg = [ gameSummary.gameId, memberId ];
        }
        init(EntitySprite.GAME_HOVER, cmd, arg);

        // specify our bounds explicitly, as our width is centered at 0.
        _host.addDecoration(this, {
              toolTip: gameSummary.name,
              weight: OccupantSprite.DEC_WEIGHT_GAME,
              bounds: new Rectangle(width/-2, 0, width, height)
         });
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

    protected var _host :MemberSprite;
    protected var _gameSummary :GameSummary;
    protected var _gameThumb :ScalingMediaDescContainer;
}

import flash.geom.Rectangle;

import com.threerings.util.Log;

import com.threerings.orth.data.MediaDescSize;
import com.threerings.orth.ui.ScalingMediaDescContainer;

import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.party.data.PartySummary;
import com.threerings.msoy.room.client.EntitySprite;
import com.threerings.msoy.room.client.MemberSprite;
import com.threerings.msoy.room.client.OccupantSprite;
import com.threerings.msoy.ui.GlowSprite;
import com.threerings.msoy.world.client.WorldController;

class PartyIcon extends GlowSprite
{
    /** The party id. */
    public var id :int;

    public function PartyIcon (host :MemberSprite, partyId :int, extraInfo :Object)
    {
        _host = host;
        id = partyId;

        var summ :PartySummary = extraInfo.parties.get(partyId) as PartySummary;
        if (summ == null) {
            Log.getLog(this).warning("Ohnoez, couldn't set up PartyIcon.");
            return;
        }

        _icon = ScalingMediaDescContainer.createView(
            Group.logo(summ.icon), MediaDescSize.QUARTER_THUMBNAIL_SIZE);
        _icon.x = _icon.maxW / -2; // position with 0 at center
        addChild(_icon);

        init(EntitySprite.OTHER_HOVER, WorldController.GET_PARTY_DETAIL, summ.id);

        var width :int = _icon.maxW;
        var height :int = _icon.maxH
        // specify our bounds explicitly, as our width is centered at 0.
        _host.addDecoration(this, {
              toolTip: summ.name,
              weight: OccupantSprite.DEC_WEIGHT_PARTY,
              bounds: new Rectangle(width/-2, 0, width, height)
        });
    }

    public function shutdown () :void
    {
        _icon.shutdown();
        _host.removeDecoration(this);
    }

    protected var _host :MemberSprite;
    protected var _icon :ScalingMediaDescContainer;
}
