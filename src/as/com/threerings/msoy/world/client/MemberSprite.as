//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.geom.Rectangle;

import com.threerings.util.CommandEvent;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.world.data.MemberInfo;

/**
 * Displays a sprite for a member in a scene.
 */
public class MemberSprite extends ActorSprite
{
    /**
     * Creates a sprite for the supplied member.
     */
    public function MemberSprite (occInfo :MemberInfo)
    {
        super(occInfo);
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

    // from OccupantSprite
    override public function setOccupantInfo (newInfo :OccupantInfo) :void
    {
        super.setOccupantInfo(newInfo);

        // take care of setting up or changing our TableIcon
        var minfo :MemberInfo = (newInfo as MemberInfo);
        if (_tableIcon != null && !_tableIcon.getGameSummary().equals(minfo.getGameSummary())) {
            _tableIcon.shutdown();
            _tableIcon = null;
        }
        if (_tableIcon == null && minfo.getGameSummary() != null &&
            minfo.getGameSummary().gameId != Game.TUTORIAL_GAME_ID) {
            _tableIcon = new TableIcon(this, minfo.getGameSummary());
        }
    }

    // from ActorSprite
    override public function messageReceived (name :String, arg :Object, isAction :Boolean) :void
    {
        super.messageReceived(name, arg, isAction);

        // TODO: remove someday
        // TEMP: dispatch an old-style avatar action notification
        // Deprecated 2007-03-13
        // Commented out 2008-02-11. We should be good.
        //if (isAction) {
        //    callUserCode("action_v1", name); // no arg
        //}
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
                    bounds: new Rectangle(0, 0, 50, 80)
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

import flash.events.MouseEvent;
import flash.filters.GlowFilter;

import com.threerings.util.CommandEvent;

import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.ui.ScalingMediaContainer;

import com.threerings.msoy.world.client.MemberSprite;
import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.world.client.WorldController;

/**
 * A decoration used when this actor is at a table in a lobby.
 */
class TableIcon extends ScalingMediaContainer
{
    public function TableIcon (host :MemberSprite, gameSummary :GameSummary)
    {
        super(30, 30);
        _host = host;
        _gameSummary = gameSummary;
        setMediaDesc(gameSummary.getThumbMedia());

        addEventListener(MouseEvent.MOUSE_OVER, handleMouseIn);
        addEventListener(MouseEvent.MOUSE_OUT, handleMouseOut);
        addEventListener(MouseEvent.CLICK, handleMouseClick);
    }

    public function getGameSummary () :GameSummary
    {
        return _gameSummary;
    }

    override public function shutdown (completely :Boolean = true) :void
    {
        if (parent != null) {
            _host.removeDecoration(this);
        }
        super.shutdown(completely);
    }

    override protected function contentDimensionsUpdated () :void
    {
        super.contentDimensionsUpdated();

        // we wait until our size is known prior to adding ourselves
        if (parent == null) {
            _host.addDecoration(this, { toolTip: _gameSummary.name });
        }
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
        CommandEvent.dispatch(this, WorldController.JOIN_GAME_LOBBY, _gameSummary.gameId);
    }

    protected var _gameSummary :GameSummary;
    protected var _host :MemberSprite;
}
