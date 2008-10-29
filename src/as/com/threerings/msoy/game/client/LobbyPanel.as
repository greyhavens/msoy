//
// $Id$

package com.threerings.msoy.game.client {

import mx.containers.HBox;
import mx.containers.VBox;
import mx.core.UIComponent;
import mx.controls.Label;
import mx.controls.Spacer;
import mx.controls.Text;
import mx.controls.TextInput;

import com.threerings.util.Log;

import com.threerings.flash.TextFieldUtil;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandLinkButton;
import com.threerings.flex.FlexUtil;

import com.threerings.parlor.data.Table;
import com.threerings.parlor.game.data.GameConfig;

import com.whirled.game.data.GameDefinition;

import com.threerings.msoy.ui.CopyableText;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.data.PlayerObject;

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
        showCloseButton = true;
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
        super.didOpen();
        x = 20; // force layout
        y = 40;
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
