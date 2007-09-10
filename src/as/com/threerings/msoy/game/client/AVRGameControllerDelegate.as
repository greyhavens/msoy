//
// $Id$

package com.threerings.msoy.game.client {

import mx.core.UIComponent;

import mx.containers.HBox;

import com.threerings.flex.CommandButton;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.parlor.game.client.GameController;
import com.threerings.parlor.game.client.GameControllerDelegate;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.world.client.RoomView;

/**
 * A delegate controller encapsulating all client-side behavior available
 * to "World" games, whether AVRG or our own.
 */
public class AVRGameControllerDelegate extends GameControllerDelegate
{
    public function AVRGameControllerDelegate (ctrl :GameController)
    {
        super(ctrl);
    }

    override public function init (ctx :CrowdContext, config :PlaceConfig) :void
    {
        super.init(ctx, config);
        _ctx = (ctx as WorldContext);
    }

    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        super.didLeavePlace(plobj);

        // ensure the user regains control of their own avatar
        _ctx.worldProps.userControlsAvatar = true;
    }

    /**
     * Sets whether the user is allowed to control their own avatar.
     */
    public function setAvatarControl (enabled :Boolean) :void
    {
        _ctx.worldProps.userControlsAvatar = enabled;
    }

    /**
     * Get a list of our own avatar actions.
     */
    public function getMyActions () :Array
    {
        var room :RoomView = (_ctx.getTopPanel().getPlaceView() as RoomView);
        return (room != null) ? room.getMyActions() : [];
    }

    /**
     * Get a list of our own avatar states.
     */
    public function getMyStates () :Array
    {
        var room :RoomView = (_ctx.getTopPanel().getPlaceView() as RoomView);
        return (room != null) ? room.getMyStates() : [];
    }

    /**
     *  A special helper method for setting the place view.
     */
    public function setPlaceView (view :PlaceView) :void
    {
        var comp :UIComponent = (view as UIComponent);

        comp.percentWidth = 100;

        _panel = new HBox();
        _panel.addChild(comp);

        // TODO: A nice wee X
        var quit :CommandButton = new CommandButton(MsoyController.LEAVE_AVR_GAME);
        quit.label = Msgs.GAME.get("b.leave_world_game");
        _panel.addChild(quit);
        _panel.height = 100;

        _ctx.getTopPanel().setBottomPanel(_panel);
    }

    /**
     *  A special helper method for clearing the place view.
     */
    public function clearPlaceView () :void
    {
        _ctx.getTopPanel().clearBottomPanel(_panel);
        _panel = null;
    }

    /** Gription on the action. */
    protected var _ctx :WorldContext;

    protected var _panel :HBox;
}
}
