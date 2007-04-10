package com.threerings.msoy.client {

import flash.events.Event;
import flash.events.MouseEvent;

import mx.containers.TitleWindow;

import mx.controls.Button;
import mx.controls.Text;
import mx.controls.TextArea;

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
        window.setStyle("horizontalAlign", "center");
        var instruction :Text = new Text();
        instruction.width = 300;
        instruction.text = Msgs.GENERAL.get("l.embed_instruction");
        instruction.selectable = false;
        window.addChild(instruction);
        var html :TextArea = new TextArea();
        html.minHeight = 100;
        html.width = 300;
        html.editable = false;
        var url :String = _headerBar.root.loaderInfo.loaderURL;
        url = url.replace(/(http:\/\/[^\/]*).*/, "$1/clients/world-client.swf");
        html.text = "<embed width='100%' height='550' flashvars='sceneId=" + 
            _ctx.getMsoyController().getSceneIdString() + "' \n" +
            "src='" + url + "' \n" +
            "wmode='opaque' pluginspace='http://www.macromedia.com/go/getflashplayer' \n" +
            "type='application/x-shockwave-flash' />";
        window.addChild(html);
        var closeButton :Button = new Button();
        closeButton.label = Msgs.GENERAL.get("b.done_embed_link");
        closeButton.addEventListener(MouseEvent.CLICK, function (evt :MouseEvent) :void {
            PopUpManager.removePopUp(window);
        });
        closeButton.buttonMode = true;
        window.addChild(closeButton);
        PopUpManager.addPopUp(window, _ctx.getTopPanel(), true);
        PopUpManager.centerPopUp(window);
    }

    protected function locationChanged (place :PlaceObject) :void
    {
        var scene :Scene = _ctx.getSceneDirector().getScene();
        if (scene != null) {
            _headerBar.setLocationText(scene.getName());
            // we know the MsoyController is initialized at this point, so it is safe to check
            // whether we are embedded or not.  
            _headerBar.setEmbedLinkButtonVisible(!_ctx.getMsoyController().isEmbedded());
        } else {
            // For now we can embed scenes with game lobbies attached, but not game instances - 
            // when we have a unique URL for game instance locations, then we can embed those 
            // locations as well, and have the embedded page bring the game lobby attached to the 
            // default game room.
            _headerBar.setEmbedLinkButtonVisible(false);
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
