package com.threerings.msoy.client {

import flash.events.Event;

import mx.core.Application;

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
    }

    protected var _ctx :WorldContext;
    protected var _headerBar :HeaderBar;
}
}
