//
// $Id$

package com.threerings.msoy.map.client {

import com.threerings.display.DisplayUtil;
import com.threerings.util.NetUtil;
import com.threerings.flex.FlexUtil;
import com.threerings.media.ScalingMediaContainer;
import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.data.all.MediaDescSize;
import com.threerings.msoy.map.data.Whirled;
import com.threerings.msoy.map.data.WhirledMap;

import com.adobe.serialization.json.JSONDecoder;

import mx.containers.Box;
import mx.containers.Canvas;
import mx.containers.HBox;
import mx.containers.VBox;
import mx.core.Application;
import mx.core.Container;
import mx.core.UIComponent;
import mx.controls.Label;
import mx.controls.Spacer;

import flash.display.Graphics;
import flash.display.Loader;
import flash.net.URLRequest;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.MouseEvent;

[SWF(width="300", height="480")]
public class MapVisualizer extends Canvas
{
    public function MapVisualizer (app :Application)
    {
        var params :Object = app.loaderInfo.parameters;
        if (params != null && params.map != null) {
            _map = WhirledMap.fromParameters(params.map);
            _showHome = (params.guest == "false");
        } else {
            _map = WhirledMap.debugMap();
            _showHome = true;
        }
    }

    // from VBox
    override protected function createChildren () :void
    {
        super.createChildren();

        _canvas = new VBox();
        _canvas.styleName = "mapCanvas";
        _canvas.width = SWF_WIDTH;
        _canvas.height = SWF_HEIGHT;
        this.addChild(_canvas);

        var title :Label = FlexUtil.createLabel("Visit a Whirled --", "mapTitle");
        title.percentWidth = 100;
        _canvas.addChild(title);

        _map.whirleds.forEach(addWhirledBox);

        if (_showHome) {
            var homeBits :HBox = new HBox();
            homeBits.styleName = "mapHomeBits";
            homeBits.percentWidth = 100;
            enableHandCursor(homeBits);
            homeBits.addEventListener(MouseEvent.CLICK, function (evt :MouseEvent) :void {
                NetUtil.navigateToURL(DeploymentConfig.serverURL + "#world-h");
            });
            _canvas.addChild(homeBits);

            var spacer :Spacer = new Spacer();
            spacer.styleName = "mapHomeSpacer";
            spacer.percentWidth = 100;
            homeBits.addChild(spacer);

            var homeText :Label = FlexUtil.createLabel("-- or go home", "mapHomeText");
            homeBits.addChild(homeText);

            var homeIcon :Container = new Container();
            homeIcon.width = 17;
            homeIcon.height = 13;
            homeIcon.styleName = "mapHomeIcon";
            homeBits.addChild(homeIcon);
        }
    }

    protected static const SWF_WIDTH :int = 300;
    protected static const SWF_HEIGHT :int = 480;
    protected static const ENTRY_HEIGHT :int = 64;
    protected static const STROKE :int = 4;
    protected static const ROUNDING :int = 12;

    protected function addWhirledBox (whirled :Whirled, ix :int, arr :Array) :void
    {
        var logoWidth :int = MediaDescSize.getWidth(MediaDescSize.THUMBNAIL_SIZE);
        var logoHeight :int = MediaDescSize.getHeight(MediaDescSize.THUMBNAIL_SIZE);

        var whirledBox :HBox = new BorderedHBox();
        whirledBox.addEventListener(MouseEvent.CLICK, getClickHandler(whirled));
        whirledBox.percentWidth = 100;
        whirledBox.styleName = "mapEntry";
        enableHandCursor(whirledBox);
        _canvas.addChild(whirledBox);

        var logoHolder :ScalingMediaContainer =
            new ScalingMediaContainer(logoWidth, logoHeight, true);
        logoHolder.setMedia(whirled.logo.getMediaPath());

        var holderComponent :UIComponent = FlexUtil.wrap(logoHolder);
        holderComponent.styleName = "mapLogo";
        holderComponent.width = logoWidth;
        holderComponent.height = logoHeight;
        whirledBox.addChild(holderComponent);

        var infoBox :VBox = new VBox();
        whirledBox.addChild(infoBox);

        var name :Label = FlexUtil.createLabel(whirled.name, "mapName");
        name.truncateToFit = true;
        name.width = 210;
        infoBox.addChild(name);

        var population :Label = FlexUtil.createLabel(
            "Currently playing: " + whirled.population, "mapPopulation");
        population.truncateToFit = true;
        population.width = 210;
        infoBox.addChild(population);
    }

    protected function getClickHandler (whirled :Whirled) :Function
    {
        return function (event :MouseEvent) :void {
            NetUtil.navigateToURL(DeploymentConfig.serverURL + "#world-s" + (whirled.homeId));
        }
    }

    protected function enableHandCursor (obj :UIComponent) :void
    {
        obj.useHandCursor = true;
        obj.buttonMode = true;
        obj.mouseChildren = false;
    }

    // scales the loaded logo to the dimensions of the logo holder
    protected function logoLoaded (event: Event) :void
    {
        trace("Logo loaded: " + event);
    }

    protected function logoError (event :IOErrorEvent) :void
    {
        trace("Error loading URL: " + event.text);
    }

    protected var _map :WhirledMap;
    protected var _showHome :Boolean;
    protected var _canvas :VBox;
}
}
