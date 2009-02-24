//
// $Id$

package com.threerings.msoy.game.client {

import mx.containers.HBox;
import mx.core.UIComponent;

import com.threerings.flex.FlexUtil;



import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.Msgs;


import com.threerings.msoy.game.data.LobbyObject;

/**
 * A panel that displays pending table games.
 */
public class LobbyPanel extends FloatingPanel
{
    /** The maximum width of the lobby panel. */
    public static const WIDTH :int = 325; // in px

    /** The maximum height of the lobby panel. */
    public static const HEIGHT :int = 450; // in px

    /**
     * Create a new LobbyPanel.
     */
    public function LobbyPanel (gctx :GameContext, ctrl :LobbyController)
    {
        super(gctx.getMsoyContext());
        _gctx = gctx;
        _ctrl = ctrl;

        setStyle("horizontalAlign", "left"); // override the sexy default
        width = WIDTH;
        height = HEIGHT;
        showCloseButton = !gctx.getMsoyContext().getMsoyClient().isEmbedded() ||
            (gctx.getLocationDirector().getPlaceObject() != null) ||
            (gctx.getMsoyContext().getLocationDirector().getPlaceObject() != null);
    }

    public function init (lobbyObj :LobbyObject) :void
    {
        _lobbyObj = lobbyObj;
    }

    /**
     * Switches to the specified interface mode.
     */
    public function setMode (mode :int) :void
    {
        var padding :int = 10;
        switch (mode) {
        case LobbyController.MODE_SPLASH:
            this.title = _lobbyObj.game.name;
            setContents(new LobbySplashPanel(_gctx, _ctrl, _lobbyObj));
            break;
        case LobbyController.MODE_MATCH:
            this.title = _lobbyObj.game.name;
            setContents(new LobbyMatchPanel(_gctx, _ctrl, _lobbyObj));
            padding = 0; // yay!
            break;
        case LobbyController.MODE_CREATE:
            this.title = Msgs.GAME.get("t.create_game", _lobbyObj.game.name);
            setContents(new TableCreationPanel(_gctx, _ctrl, _lobbyObj));
            break;
        case LobbyController.MODE_SEATED:
            this.title = Msgs.GAME.get("t.mini_table", _lobbyObj.game.name);
            setContents(new LobbyTablePanel(_gctx, _ctrl, _lobbyObj));
            break;
        }

        // one of our panels requires custom padding, thanks Bill!
        setStyle("paddingLeft", padding);
        setStyle("paddingRight", padding);
        setStyle("paddingBottom", padding);
    }

    override protected function didOpen () :void
    {
        x = 20; // force layout
        y = 40;
        super.didOpen();
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        // display feedback indicating that we're locating their game; once init is called, we'll
        // display the real UI
        var loading :HBox = new HBox();
        loading.styleName = "lobbyLoadingBox";
        loading.addChild(FlexUtil.createLabel(Msgs.GAME.get("m.locating_game")));
        this.title = Msgs.GAME.get("t.locating_game");
        addChild(loading);
    }

    protected function setContents (contents :UIComponent) :void
    {
        if (numChildren > 0) {
            removeChildAt(0);
        }
        addChild(contents);
    }

    /** Buy one get one free. */
    protected var _gctx :GameContext;

    /** Our controller. */
    protected var _ctrl :LobbyController;

    /** Our lobby object. */
    protected var _lobbyObj :LobbyObject;
}
}
