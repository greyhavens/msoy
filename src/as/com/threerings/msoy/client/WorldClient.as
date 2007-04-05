//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.Stage;
import flash.display.StageQuality;
import flash.events.ContextMenuEvent;
import flash.external.ExternalInterface;
import flash.geom.Point;
import flash.system.Capabilities;
import flash.ui.ContextMenu;

import mx.core.Application;
import mx.resources.ResourceBundle;

import com.threerings.util.Name;
import com.threerings.util.ResultAdapter;
import com.threerings.util.StringUtil;

import com.threerings.flash.MenuUtil;

import com.threerings.whirled.data.SceneMarshaller;
import com.threerings.whirled.spot.data.SpotMarshaller;
import com.threerings.whirled.spot.data.SpotSceneObject;

import com.threerings.parlor.data.ParlorMarshaller;
import com.threerings.toybox.data.ToyBoxMarshaller;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.item.data.ItemMarshaller;
import com.threerings.msoy.item.web.Document;
import com.threerings.msoy.item.web.Furniture;
import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.Photo;

import com.threerings.msoy.world.data.PetMarshaller;
import com.threerings.msoy.world.data.RoomConfig;

import com.threerings.msoy.game.client.LobbyController;
import com.threerings.msoy.game.data.LobbyMarshaller;
import com.threerings.msoy.game.data.WorldGameConfig;
import com.threerings.msoy.game.data.WorldGameMarshaller;

import com.threerings.msoy.game.chiyogami.client.ChiyogamiController;

/**
 * Handles the main services for the world and game clients.
 */
public class WorldClient extends BaseClient
{
    public function WorldClient (stage :Stage)
    {
        super(stage);

        // TODO: allow users to choose? I think it's a decision that we should make for them.
        // Jon speculates that maybe we can monitor the frame rate and automatically shift it,
        // but noticable jiggles occur when it's switched and I wouldn't want the entire
        // world to jiggle when someone starts walking, then jiggle again when they stop.
        // So: for now we just peg it to MEDIUM.
        stage.quality = StageQuality.MEDIUM;

        // set up a context menu that blocks funnybiz on the stage
        var menu :ContextMenu = new ContextMenu();
        menu.hideBuiltInItems();
        Application.application.contextMenu = menu;
        menu.addEventListener(ContextMenuEvent.MENU_SELECT, contextMenuWillPopUp);

        // now logon
        logon();
    }

    // from BaseClient
    override public function fuckingCompiler () :void
    {
        super.fuckingCompiler();
        var c :Class;
        c = RoomConfig;
        c = SceneMarshaller;
        c = SpotMarshaller;
        c = SpotSceneObject;
        c = ParlorMarshaller;
        c = Document;
        c = Photo;
        c = Furniture;
        c = Game;
        c = ItemMarshaller;
        c = LobbyMarshaller;
        c = WorldGameMarshaller;
        c = LobbyController;
        c = ToyBoxMarshaller;
        c = PetMarshaller;
        c = WorldGameConfig;
        c = ChiyogamiController;

        // these cause bundles to be compiled in.
        [ResourceBundle("general")]
        [ResourceBundle("game")]
        [ResourceBundle("editing")]
        [ResourceBundle("item")]
        [ResourceBundle("prefs")]
        var rb :ResourceBundle;
    }

    /**
     * Notifies our JavaScript shell that the flash client should be cleared out.
     */
    public function closeClient () :void
    {
        try {
            if (ExternalInterface.available) {
                ExternalInterface.call("clearClient");
            }
        } catch (err :Error) {
            log.warning("ExternalInterface.call('clearClient') failed: " + err);
        }
    }

    // from BaseClient
    override protected function createContext () :BaseContext
    {
        return (_wctx = new WorldContext(this));
    }

    // from BaseClient
    override protected function configureExternalFunctions () :void
    {
        super.configureExternalFunctions();
        ExternalInterface.addCallback("clientLogon", externalClientLogon);
        ExternalInterface.addCallback("clientGo", externalClientGo);
        ExternalInterface.addCallback("clientLogoff", externalClientLogoff);
        ExternalInterface.addCallback("setMinimized", externalSetMinimized);

        _wctx.getMsoyController().setEmbedded(
            !Boolean(ExternalInterface.call("helloWhirled")));
    }

    /**
     * Called to process ContextMenuEvent.MENU_SELECT.
     */
    protected function contextMenuWillPopUp (event :ContextMenuEvent) :void
    {
        var menu :ContextMenu = (event.target as ContextMenu);
        var custom :Array = menu.customItems;
        custom.length = 0;

//        custom.push(MenuUtil.createControllerMenuItem(
//                        Msgs.GENERAL.get("b.toggle_fullscreen"),
//                        MsoyController.TOGGLE_FULLSCREEN, null, false,
//                        _wctx.getMsoyController().supportsFullScreen()));

        try {
            var allObjects :Array =
                _stage.getObjectsUnderPoint(new Point(_stage.mouseX, _stage.mouseY));
            var seenObjects :Array = [];
            for each (var disp :DisplayObject in allObjects) {
                do {
                    seenObjects.push(disp);
                    if (disp is ContextMenuProvider) {
                        (disp as ContextMenuProvider).populateContextMenu(custom);
                    }
                    disp = disp.parent;

                } while (disp != null && (seenObjects.indexOf(disp) == -1));
            }
        } catch (e :Error) {
            Log.getLog(this).logStackTrace(e);
        }

        // HACK: putting the separator in the menu causes the item to not
        // work in linux, so we don't do it in linux.
        var useSep :Boolean = (-1 == Capabilities.os.search("Linux"));

        // add the About menu item
        custom.push(MenuUtil.createControllerMenuItem(
                        Msgs.GENERAL.get("b.about"),
                        MsoyController.ABOUT, null, useSep));

        // then, the menu will pop up
    }

    /**
     * Exposed to javascript so that it may notify us to logon.
     */
    protected function externalClientLogon (memberId :int, token :String) :void
    {
        if (token == null) {
            return;
        }

        log.info("Logging on via external request [id=" + memberId + ", token=" + token + "].");
        var co :MemberObject = _wctx.getMemberObject();
        if (co == null || co.getMemberId() != memberId) {
            _wctx.getMsoyController().handleLogon(createStartupCreds(_wctx.getStage(), token));
        }
    }

    /**
     * Exposed to javascript so that it may notify us to move to a new location.
     */
    protected function externalClientGo (where :String) :void
    {
        var eidx :int = where.indexOf("=");
        if (eidx == -1) {
            log.warning("Dropping malformed go request [where=" + where + "].");
        } else {
            log.info("Changing scenes per external request [where=" + where + "].");
            var params :Object = new Object();
            params[where.substring(0, eidx)] = where.substring(eidx+1);
            _wctx.getMsoyController().goToPlace(params);
        }
    }

    /**
     * Exposed to javascript so that it may notify us to logoff.
     */
    protected function externalClientLogoff (backAsGuest :Boolean = true) :void
    {
        log.info("Logging off via external request [backAsGuest=" + backAsGuest + "].");

        if (backAsGuest) {
            // have the controller handle it it will logoff, then back as a guest
            _wctx.getMsoyController().handleLogon(null);
        } else {
            logoff(false);
        }
    }

    /**
     * Exposed to javascript so that it may let us know when we've been pushed out of the way.
     */
    protected function externalSetMinimized (mini :Boolean) :void   
    {
        log.info("Client was notified that its minimized status has changed: " + mini);
        _wctx.getMsoyController().setMinimized(mini);
    }

    protected var _wctx :WorldContext;
}
}
