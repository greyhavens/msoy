package com.threerings.msoy.client {

import flash.events.Event;
import flash.events.MouseEvent;

import mx.containers.TitleWindow;

import mx.controls.Button;

import mx.core.Application;

import mx.managers.PopUpManager;

import com.threerings.util.Controller;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.data.Scene;

import com.threerings.msoy.game.data.MsoyGameConfig;

public class HeaderBarController extends Controller
{
    /** Command to close out the minimized flash client */
    public static const CLOSE_CLIENT :String = "CloseClient";

    /** Commant to show a popup box with HTML to copy to your blog to embed the current scene. */
    public static const SHOW_EMBED_HTML :String = "ShowEmbedHtml";

    public function HeaderBarController (ctx :WorldContext, headerBar :HeaderBar)
    {
        _ctx = ctx;
        _headerBar = headerBar;

        setControlledPanel(headerBar);
        
        _ctx.getLocationDirector().addLocationObserver(new LocationAdapter(null, 
            this.locationChanged, null));

        Application.application.stage.addEventListener(Event.RESIZE, function (evt :Event) :void {
            _headerBar.setCloseButtonVisible(_ctx.getMsoyController().getMinimized());
        });
    }

    public function handleCloseClient () :void
    {
        (_ctx.getClient() as WorldClient).closeClient();
    }

    public function handleShowEmbedHtml () :void
    {
        var window :TitleWindow = new TitleWindow();
        window.title = Msgs.GENERAL.get("t.embed_link_window");
        PopUpManager.addPopUp(window, _ctx.getTopPanel(), true);
        PopUpManager.centerPopUp(window);
        var closeButton :Button = new Button();
        closeButton.label = Msgs.GENERAL.get("b.done_embed_link");
        closeButton.addEventListener(MouseEvent.CLICK, function (evt :MouseEvent) :void {
            PopUpManager.removePopUp(window);
        });
        closeButton.buttonMode = true;
        window.addChild(closeButton);
    }

    protected function locationChanged (place :PlaceObject) :void
    {
        var scene :Scene = _ctx.getSceneDirector().getScene();
        if (scene != null) {
            _headerBar.setLocationText(scene.getName());
        } else {
            var ctrl :PlaceController = _ctx.getLocationDirector().getPlaceController();
            if (ctrl != null && ctrl.getPlaceConfig() is MsoyGameConfig) {
                _headerBar.setLocationText((ctrl.getPlaceConfig() as MsoyGameConfig).name);
            }
        }
        // we know the MsoyController is initialized at this point, so it is safe to check
        // whether we are embedded or not.
        _headerBar.setEmbedLinkButtonVisible(!_ctx.getMsoyController().isEmbedded());
    }

    protected var _ctx :WorldContext;
    protected var _headerBar :HeaderBar;
}
}
