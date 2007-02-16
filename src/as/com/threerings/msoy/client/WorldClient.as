package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.Stage;

import flash.external.ExternalInterface;

import flash.events.ContextMenuEvent;

import flash.geom.Point;

import flash.system.Security;

import flash.ui.ContextMenu;

import mx.core.Application;

import mx.resources.ResourceBundle;

import com.threerings.util.MenuUtil;
import com.threerings.util.Name;
import com.threerings.util.ResultAdapter;
import com.threerings.util.StringUtil;

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.data.ClientObject;

import com.threerings.presents.dobj.DObjectManager;

import com.threerings.presents.net.BootstrapData;

// imported so that they'll be compiled into the .swf
import com.threerings.presents.data.TimeBaseMarshaller;
import com.threerings.crowd.data.BodyMarshaller;
import com.threerings.crowd.data.LocationMarshaller;
import com.threerings.crowd.chat.data.ChatMarshaller;

import com.threerings.whirled.data.SceneMarshaller;
import com.threerings.whirled.spot.data.SpotMarshaller;
import com.threerings.whirled.spot.data.SpotSceneObject;

import com.threerings.parlor.data.ParlorMarshaller;
import com.threerings.toybox.data.ToyBoxMarshaller;

import com.threerings.msoy.data.MemberInfo;
import com.threerings.msoy.data.MemberMarshaller;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyAuthResponseData;
import com.threerings.msoy.data.MsoyBootstrapData;
import com.threerings.msoy.data.MsoyCredentials;

import com.threerings.msoy.item.data.ItemMarshaller;
import com.threerings.msoy.item.web.Document;
import com.threerings.msoy.item.web.Furniture;
import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.Photo;

import com.threerings.msoy.world.data.PetMarshaller;
import com.threerings.msoy.world.data.RoomConfig;

import com.threerings.msoy.game.data.LobbyMarshaller;
import com.threerings.msoy.game.data.WorldGameMarshaller;
import com.threerings.msoy.game.client.LobbyController;

import com.threerings.msoy.swiftly.data.SwiftlyMarshaller;

/**
 * Handles the main services for the world and game clients.
 */
public class WorldClient extends BaseClient
{
    public function WorldClient (stage :Stage)
    {
        super(stage);

        // set up a context menu that blocks funnybiz on the stage
        var menu :ContextMenu = new ContextMenu();
        menu.hideBuiltInItems();
        Application.application.contextMenu = menu;
        menu.addEventListener(ContextMenuEvent.MENU_SELECT, contextMenuWillPopUp);
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
        c = SwiftlyMarshaller;
        c = PetMarshaller;

        // these cause bundles to be compiled in.
        [ResourceBundle("general")]
        [ResourceBundle("game")]
        [ResourceBundle("editing")]
        [ResourceBundle("item")]
        [ResourceBundle("prefs")]
        var rb :ResourceBundle; // this needs to be here for the above lines
    }

    // from BaseClient
    override protected function createContext () :BaseContext
    {
        return new WorldContext(this);
    }

    /**
     * Called to process ContextMenuEvent.MENU_SELECT.
     */
    protected function contextMenuWillPopUp (event :ContextMenuEvent) :void
    {
        var menu :ContextMenu = (event.target as ContextMenu);
        var custom :Array = menu.customItems;
        custom.length = 0;

        custom.push(MenuUtil.createControllerMenuItem(
            Msgs.GENERAL.get("b.toggle_fullscreen"), MsoyController.TOGGLE_FULLSCREEN, null, false,
            (_ctx as WorldContext).getMsoyController().supportsFullScreen()));

        try {
        // TODO: this doesn't seem to find or get triggered by
        // perspectivized furniture. It should.
//        trace("Inacc: " + _stage.areInaccessibleObjectsUnderPoint(
//            new Point(_stage.mouseX, _stage.mouseY)));
        var allObjects :Array = _stage.getObjectsUnderPoint(
            new Point(_stage.mouseX, _stage.mouseY));

        var seenObjects :Array = [];
        for each (var disp :DisplayObject in allObjects) {
            do {
//                trace("Checking " + disp);
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

        // then, the menu will pop up
    }
}
}
