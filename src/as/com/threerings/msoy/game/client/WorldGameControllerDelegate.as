//
// $Id$

package com.threerings.msoy.game.client {

import mx.core.UIComponent;

import mx.containers.HBox;

import com.threerings.flex.CommandButton;

import com.threerings.crowd.client.PlaceView;

import com.threerings.parlor.game.client.GameController;
import com.threerings.parlor.game.client.GameControllerDelegate;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.WorldContext;

public class WorldGameControllerDelegate extends GameControllerDelegate
{
    public function WorldGameControllerDelegate (ctrl :GameController)
    {
        super(ctrl);
    }

    /**
     *  A special helper method for setting the place view.
     */
    public function setPlaceView (ctx :WorldContext, view :PlaceView) :void
    {
        var comp :UIComponent = (view as UIComponent);

        comp.percentWidth = 100;

        _panel = new HBox();
        _panel.addChild(comp);

        // TODO: A nice wee X
        var quit :CommandButton = new CommandButton(MsoyController.LEAVE_WORLD_GAME);
        quit.label = Msgs.GAME.get("b.leave_world_game");
        _panel.addChild(quit);

        ctx.getTopPanel().setBottomPanel(_panel);
    }

    /**
     *  A special helper method for clearing the place view.
     */
    public function clearPlaceView (ctx :WorldContext) :void
    {
        ctx.getTopPanel().clearBottomPanel(_panel);
        _panel = null;
    }

    protected var _panel :HBox;
}
}
