//
// $Id$

package com.threerings.msoy.world.client {

import flash.events.Event;

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.Loader;
import flash.display.Shape;

import flash.net.URLRequest;

import flash.system.LoaderContext;

import flash.utils.ByteArray;

import mx.containers.Canvas;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.ui.FloatingPanel;

/**
 * A pop-up containing a user-code display object of some sort, to display
 * detailed information, or a configuration panel. Only one EntityPopup may be
 * displayed at a time, and only in response to a direct user click.
 */
public class EntityPopup extends FloatingPanel
{
    public function EntityPopup (
        ctx :WorldContext, entitySprite :MsoySprite, ctrl :RoomController,
        title :String, userPanel :DisplayObject, panelWidth :Number, panelHeight :Number)
    {
        super(ctx, title);
        _ctrl = ctrl;
        _entitySprite = entitySprite;
        _userPanel = userPanel;

        // TODO: style the title bar so that it cannot look like a whirled interface...
        showCloseButton = true;
        // TODO: a nice pop-up effect when this thing comes up. ZoomEffect.

        // don't let us get too big, we'd rather scroll dahling
        maxWidth = 400;
        maxHeight = 300;

        // set up the canvas and the mask
        _canvas = new Canvas();
        _canvas.width = panelWidth;
        _canvas.height = panelHeight;
        var mask :Shape = new Shape();
        mask.graphics.beginFill(0xFFFFFF);
        mask.graphics.drawRect(0, 0, panelWidth, panelHeight);
        mask.graphics.endFill();
        _canvas.rawChildren.addChild(mask);
        _canvas.mask = mask;
    }

    public function getOwningEntity () :MsoySprite
    {
        return _entitySprite;
    }

    override public function close () :void
    {
        super.close();
        _ctrl.entityPopupClosed();
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        addChild(_canvas);

        _loader = new Loader();
        _loader.contentLoaderInfo.addEventListener(Event.COMPLETE, handleLoadingComplete);
        _loader.loadBytes(new STUB() as ByteArray);
        _canvas.rawChildren.addChild(_loader);
    }

    protected function handleLoadingComplete (event :Event) :void
    {
        DisplayObjectContainer(_loader.content).addChild(_userPanel);
    }

    /** We use this to control the size allocated for the displayed content. */
    protected var _canvas :Canvas;

    protected var _ctrl :RoomController;

    /** The sprite that owns this. */
    protected var _entitySprite :MsoySprite;

    protected var _loader :Loader;

    protected var _userPanel :DisplayObject;

    [Embed(source="../../../../../../../rsrc/media/Stub.swf", mimeType="application/octet-stream")]
    protected static const STUB :Class;
}
}
