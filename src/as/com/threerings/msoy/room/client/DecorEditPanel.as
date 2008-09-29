//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.DisplayObject;

import flash.external.ExternalInterface;

import mx.binding.utils.BindingUtils;

import mx.controls.VSlider;

import mx.events.CloseEvent;

import com.threerings.util.Log;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.item.data.all.Decor;

import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.MsoySceneModel;

/**
 * Widgets used for editing a backdrop definition.
 */
public class DecorEditPanel extends FloatingPanel
{
    public function DecorEditPanel (ctx :MsoyContext, studioView :RoomStudioView)
    {
        super(ctx, Msgs.WORLD.get("t.edit_backdrop"));
        _studioView = studioView;
        _decor = studioView.getScene().getDecor();
        showCloseButton = true;
    }

    override public function open (
        modal :Boolean = false, parent :DisplayObject = null, center :Boolean = false) :void
    {
        super.open(modal, parent, center);

        // position ourselves
        this.x = stage.stageWidth - width - 10;
        this.y = 10;

        callLater(doHookup);
    }

    protected function doHookup () :void
    {
        // register everything. We are never actually closed
        try {
            ExternalInterface.addCallback("updateMedia", updateMedia);
            ExternalInterface.addCallback("updateParameters", updateParameters);

            ExternalInterface.call("updateDecorInit");

        } catch (e :Error) {
            log.warning("Unable to configure js bridge", e);
        }
    }

    override protected function handleClose (event :CloseEvent) :void
    {
        // don't close, just hide
        visible = false;
    }

    protected function saveChanges (... ignored) :void
    {
        if (_suppressSaves) {
            return;
        }

        trace("Save Changes....");
        _decor.horizon = _horizon.value;

        updateDecorInViewer(false);
        updateDecorOnPage();
    }

    protected function updateMedia (path :String) :void
    {
        trace("updateMedia called: " + path);

        _decor.furniMedia = new StudioMediaDesc(path);

        updateDecorInViewer(true);
    }

    protected function updateParameters (
        width :int, height :int, depth :int, horizon :Number, type :int,
        offsetX :Number, offsetY :Number, hideWalls :Boolean) :void
    {
        trace("updateParameters called: " + width + ", " + height + " horz: " + horizon);
        _decor.width = width;
        _decor.height = height;
        _decor.depth = depth;
        _decor.type = type;
        _decor.horizon = horizon;
        _decor.offsetX = offsetX;
        _decor.offsetY = offsetY;
        _decor.hideWalls = hideWalls;


        _suppressSaves = true;
        try {
            _horizon.value = horizon;
        } finally {
            _suppressSaves = false;
        }

        updateDecorInViewer(false);
    }

    protected function updateDecorInViewer (mediaUpdated :Boolean) :void
    {
//        _studioView.sceneEdited(_decor);
        var newScene :MsoyScene = _studioView.getScene().clone() as MsoyScene;
        var newModel :MsoySceneModel = newScene.getSceneModel() as MsoySceneModel;
        newModel.decor = _decor;

        _studioView.setScene(newScene);
        //_studioView.setBackground(_decor);
        _studioView.updateBackground();
    }

    protected function updateDecorOnPage () :void
    {
        try {
            if (!ExternalInterface.available) {
                log.warning("External interface not available, can't save decor.");
                return;
            }

            ExternalInterface.call("updateDecor",
                _decor.width, _decor.height, _decor.depth, _decor.horizon,
                _decor.type, _decor.offsetX, _decor.offsetY, _decor.hideWalls);

        } catch (e :Error) {
            log.warning("Unable to send decor to hosting page.", e);
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _horizon = new VSlider();
        _horizon.liveDragging = true;
        _horizon.maximum = 1;
        _horizon.minimum = 0;
        addChild(_horizon);


        // now bind everything up so that these widgets change things
        _suppressSaves = true;
        try {
            BindingUtils.bindSetter(saveChanges, _horizon, "value");
        } finally {
            _suppressSaves = false;
        }
    }

    protected var log :Log = Log.getLog(this);

    protected var _suppressSaves :Boolean;

    protected var _decor :Decor;

    protected var _studioView :RoomStudioView;

    protected var _horizon :VSlider;
}
}
