//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.Graphics;
import flash.external.ExternalInterface;
import flash.events.Event;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.ui.Mouse;

import mx.containers.Canvas;
import mx.containers.Grid;
import mx.containers.VBox;
import mx.controls.ComboBox;
import mx.controls.Label;
import mx.controls.Image;
import mx.controls.HSlider;
import mx.controls.TextInput;
import mx.core.Application;
import mx.core.Container;
import mx.core.UIComponent;
import mx.core.ScrollPolicy;

import mx.events.SliderEvent;

import com.threerings.util.Log;
import com.threerings.util.MessageManager;
import com.threerings.util.ParameterUtil;

import com.threerings.flash.MathUtil;

import com.threerings.flex.CommandCheckBox;
import com.threerings.flex.GridUtil;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyLogConfig;

import com.threerings.msoy.item.data.all.Decor;
 
public class DecorViewerComp extends Canvas
{
    public static const log :Log = Log.getLog(DecorViewerComp);

    public function DecorViewerComp ()
    {
        if (ExternalInterface.available) {
            try {
                // hook up our ffi
                ExternalInterface.addCallback("updateParameters", updateParameters);
                ExternalInterface.addCallback("updateMedia", updateMedia);
            } catch (err :Error) {
                dlog("External interface initialization failed: " + err);
            }
        } else {
            dlog("External interface not available!");
        }
    }

    /**
     * Called back from ParameterUtil when we've got the goods.
     */
    protected function gotParams (params :Object) :void
    {
        if (null != params["readonly"]) {
            FlexUtil.setVisible(_mousePanel, false);
            FlexUtil.setVisible(_optionPanel, false);
            for each (var input :TextInput in getTextInputs()) {
                input.editable = false;
            }
        }

        var media :String = params["media"] as String;
        if (media != null) {
            // Below, we mostly rely on flash's runtime type coercian
            // width :int, height :int, depth :int, horizon :Number, type :int,
            // offsetX :Number, offsetY :Number, hideWalls :Boolean
            updateParameters(params["width"], params["height"], params["depth"],
                params["horizon"], params["type"], params["offsetX"], params["offsetY"],
                ("true" == params["hideWalls"]));
            updateMedia(media);
        }
    }

    // @Override from Canvas
    override protected function createChildren () :void
    {
        super.createChildren();
        
        Msgs.init(new MessageManager());
        
        _results = new Label();
        addChild(_results);

        var vbox :VBox = new VBox();
        vbox.percentWidth = 100;
        addChild(vbox);

        // container for room display
        _preview = new Canvas();
        _preview.x = _preview.y = 50;
        _preview.width = PREVIEW_BOX_WIDTH;
        _preview.height = PREVIEW_BOX_HEIGHT;
        _preview.horizontalScrollPolicy = _preview.verticalScrollPolicy = ScrollPolicy.OFF;
        vbox.addChild(_preview);
        
        var mask :Canvas = new Canvas();
        mask.x = mask.y = 0;
        mask.width = PREVIEW_BOX_WIDTH;
        mask.height = PREVIEW_BOX_HEIGHT;
        var g :Graphics = mask.graphics;
        g.beginFill(0xffffff); 
        g.drawRect(0, 0, mask.width, mask.height);
        g.endFill();
        _preview.mask = mask;

        _backdropCanvas = new Canvas();
        _backdropCanvas.x = _backdropCanvas.y = 0;
        _backdropCanvas.width = PREVIEW_BOX_WIDTH;
        _backdropCanvas.height = PREVIEW_BOX_HEIGHT;

        // UIComponent wrapper is needed around a plain old sprite object
        _wrapper = new UIComponent();
        _media = new DecorMediaContainer(this);
        _wrapper.x = _backdropCanvas.x;
        _wrapper.y = _backdropCanvas.y;
        _wrapper.addChild(_media);

        _scaleLabel = new Label();
        _scaleLabel.setStyle("bottom", 0);
        _scaleLabel.setStyle("right", 0);
        
        _preview.addChild(mask);
        _preview.addChild(_wrapper);
        _preview.addChild(_backdropCanvas);
        _preview.addChild(_scaleLabel);

        // container for mouse options

        var mouseopts :Grid = new Grid();
        _mousePanel = mouseopts;
        vbox.addChild(mouseopts);
        
        _horizonMode = new CommandCheckBox(Msgs.EDITING.get("b.move_horizon"),
            horizonModeSelectionHandler);
        _horizonMode.styleName = "oldCheckBox";
        _offsetMode = new CommandCheckBox(Msgs.EDITING.get("b.move_offset"),
            offsetModeSelectionHandler);
        _offsetMode.styleName = "oldCheckBox";

        GridUtil.addRow(mouseopts, Msgs.EDITING.get("l.move_selection"),
                        _horizonMode, _offsetMode);

        // container for standard options
        
        var standard :Grid = new Grid();
        _optionPanel = standard;
        vbox.addChild(standard);

        var types :Array = [];
        for (var ii :int = 0; ii < Decor.TYPE_COUNT; ii++) {
            types.push({ label: Msgs.EDITING.get("m.scene_type_" + ii),
                        data: ii });
        }
        _types = new ComboBox();
        _types.dataProvider = types;
        _types.addEventListener(Event.CHANGE, regularOptionsChanged);

        _depthSlider = new HSlider();
        _depthSlider.minimum = 1;
        _depthSlider.maximum = 2000;
        _depthSlider.width = 100;
        _depthSlider.liveDragging = true;
        _depthSlider.addEventListener(SliderEvent.CHANGE, regularOptionsChanged);

        _hideWallsBox = new CommandCheckBox(Msgs.EDITING.get("l.hide_walls"),
            regularOptionsChanged);
        _hideWallsBox.styleName = "oldCheckBox";
            
        GridUtil.addRow(standard, Msgs.EDITING.get("l.scene_type"), _types,
                        Msgs.EDITING.get("l.scene_depth"), _depthSlider, _hideWallsBox);

        
        // container for advanced options

        var advanced :Grid = new Grid();
        vbox.addChild(advanced);

        GridUtil.addRow(advanced, Msgs.EDITING.get("l.scene_dimensions"),
                        _widthBox = new TextInput(), _heightBox = new TextInput(),
                        _depthBox = new TextInput(), Msgs.EDITING.get("l.horizon"),
                        _horizonXBox = new TextInput(), _horizonYBox = new TextInput(),
                        Msgs.EDITING.get("l.offset"), _offsetXBox = new TextInput(),
                        _offsetYBox = new TextInput());
        
        for each (var input :TextInput in getTextInputs()) {
            input.width = 40;
            input.addEventListener(Event.CHANGE, advancedOptionsChanged);
        }

        FlexUtil.setVisible(_horizonXBox, false); // for now
        
        // init pointer resources. produces a tree of the same topology as POINTERS,
        // only containing references to initialized Image objects.
        _pointers = new Array();
        for each (var def :Array in POINTERS) {
            var pointer :Array = new Array();
            for each (var c :Class in def) {
                if (c != null) {
                    var image :Image = new Image();
                    image.source = new c();
                    pointer.push(image);
                } else {
                    pointer.push(null);
                }
            }
            _pointers.push(pointer);
        }
        
        // send an initialization request to GWT
        if (ExternalInterface.available) {
            try {
                ExternalInterface.call("updateDecorInit");
            } catch (e :Error) {
                dlog("Unable to initialize updates with Javascript: " + e);
            }
        } 

        // and try to get any other params we may have
        ParameterUtil.getParameters(this, gotParams);
    }

    // @Override from Canvas
    override protected function childrenCreated () :void
    {
        super.childrenCreated();

        _preview.addEventListener(MouseEvent.MOUSE_DOWN, mouseDownHandler);
        _preview.addEventListener(MouseEvent.MOUSE_MOVE, mouseMoveHandler);
        _preview.addEventListener(MouseEvent.MOUSE_UP, mouseUpHandler);
        _preview.addEventListener(MouseEvent.ROLL_OVER, rollOverHandler);
        _preview.addEventListener(MouseEvent.ROLL_OUT, rollOutHandler);
    }

    /**
     * Return an array of all the text input fields.
     */
    protected function getTextInputs () :Array
    {
        return [ _widthBox, _heightBox, _depthBox, _horizonXBox, _horizonYBox,
            _offsetXBox, _offsetYBox ];
    }

    /**
     * Called whenever any of the UI elements changes.
     */
    protected function regularOptionsChanged (... ignored) :void
    {
        // update data from sliders
        _decor.depth = _depthSlider.value;
        _decor.type = _types.selectedIndex;
        _decor.hideWalls = _hideWallsBox.selected;

        refreshAdvancedUI();
        refreshPreview();
        
        sendUpdateToJS();
    }

    /**
     * Called whenever text boxes change.
     */
    protected function advancedOptionsChanged (event :Event) :void
    {
        // update data from text boxes
        _decor.width = norm(int(_widthBox.text), 1);
        _decor.height = norm(int(_heightBox.text), 1);
        _decor.depth = norm(int(_depthBox.text), 1);
        _decor.horizon = norm(Number(_horizonYBox.text), 0);
        _decor.offsetX = norm(Number(_offsetXBox.text), 0);
        _decor.offsetY = norm(Number(_offsetYBox.text), 0);
        
        refreshStandardUI();
        refreshPreview();
        
        sendUpdateToJS();
    }
    
    /**
     * Called from JavaScript, updates this viewer's internal parameters (width, height, etc.)
     */
    public function updateParameters (
        width :int, height :int, depth :int, horizon :Number, type :int,
        offsetX :Number, offsetY :Number, hideWalls :Boolean) :void
    {
        // update storage
        _decor.width = width;
        _decor.height = height;
        _decor.depth = depth;
        _decor.type = type;
        _decor.horizon = horizon;
        _decor.offsetX = offsetX;
        _decor.offsetY = offsetY;
        _decor.hideWalls = hideWalls;

        refreshStandardUI();
        refreshAdvancedUI();
        refreshPreview();
    }

    /**
     * Called from JavaScript, updates this viewer's media.
     */
    public function updateMedia (mediaPath :String) :void
    {
        _mediaPath = mediaPath;
        _media.setMedia(mediaPath);
        _media.alpha = 0.7;
        refreshPreview();
    }

    /**
     * Refreshes standard UI controls from data.
     */
    protected function refreshStandardUI () :void
    {
        _depthSlider.value = _decor.depth;
        _types.selectedIndex = _decor.type;
        _hideWallsBox.selected = _decor.hideWalls;
    }

    /**
     * Refreshes advanced UI controls from data.
     */
    protected function refreshAdvancedUI () :void
    {
        _widthBox.text = String(_decor.width);
        _heightBox.text = String(_decor.height);
        _depthBox.text = String(_decor.depth);
        _horizonYBox.text = String(_decor.horizon);
        _offsetXBox.text = String(_decor.offsetX);
        _offsetYBox.text = String(_decor.offsetY);
    }
    
    /**
     * Refreshes the preview screen.
     */
    public function refreshPreview () :void
    {
        // redraw the backdrop image
        _backdrop.update(_decor);
        _backdrop.drawRoom(
            _backdropCanvas.graphics, _backdropCanvas.width, _backdropCanvas.height, true, false);
        
        // scale the preview box. ideally, we want a 50% scaling factor, but if that's too big,
        // just shrink the whole thing to fit.
        var scale :Number = 0.5;
        if (_decor.width * scale > PREVIEW_BOX_WIDTH ||
            _decor.height * scale > PREVIEW_BOX_HEIGHT)
        {
            scale = Math.min (
                PREVIEW_BOX_WIDTH / _decor.width, PREVIEW_BOX_HEIGHT / _decor.height);
        }
        
        // scale the bitmap container and the backdrop canvas
        _backdropCanvas.scaleX = _backdropCanvas.scaleY = scale;
        _wrapper.scaleX = _wrapper.scaleY = scale;

        // center the bitmap horizontally, and align vertically with the bottom of the room,
        // taking offsets into account
        var pixelOffsetX :Number = (_decor.offsetX + _deltaX) * _media.width;
        var pixelOffsetY :Number = (_decor.offsetY + _deltaY) * _media.height;
        _media.x = (_decor.width - _media.width) / 2 + pixelOffsetX;
        _media.y = _decor.height - _media.height - pixelOffsetY; 

        // update the text widget
        _scaleLabel.text = Msgs.EDITING.get("l.preview_scale", String(int(scale * 100)))
    }
    
    /**
     * Sends the current viewer parameters to JavaScript. Only sends parameters that can change
     * in the viewer (width, height, etc.) - not the media, which can't be modified here.
     */
    public function sendUpdateToJS () :void
    {
        if (ExternalInterface.available) {
            try {
                ExternalInterface.call(
                    "updateDecor", _decor.width, _decor.height, _decor.depth, _decor.horizon,
                    _decor.type, _decor.offsetX, _decor.offsetY, _decor.hideWalls);
            } catch (e :Error) {
                dlog("Unable to send update to Javascript: " + e);
            }
        } else {
            dlog("External interface not available, " +
                 "while trying to send an update to Javascript");
        }
    }

    
    // Various functions for dealing with player's clicks and drags inside the preview window.

    /** Sets the current mouse mode, which influences how mouse movement should be interpreted. */
    protected function setMouseMode (newMode :int) :void
    {
        _mouseMode = newMode;
        updatePointerImage(true);
    }

    /** Changes mouse cursor to the right bitmap for the current mouse mode and button status. */
    protected function updatePointerImage (up :Boolean) :void
    {
        if (_pointer != null) {
            _preview.removeChild(_pointer);
        }

        // get a new image
        _pointer = (_pointers[_mouseMode] as Array)[(up ? 0 : 1)] as Image;
        
        if (_pointer != null) {
            _preview.addChild(_pointer);
            _pointer.visible = true;
        }
        
        updatePointerPosition();
    }

    protected function updatePointerPosition () :void
    {
        if (_pointer != null) {
            _pointer.x = (_preview.mouseX - _pointer.source.width / 2);
            _pointer.y = (_preview.mouseY - _pointer.source.height / 2);
        }
    }

    protected function rollOverHandler (event :MouseEvent) :void
    {
        // hide the mouse pointer and show our custom one (if specified).
        if (_pointer != null) {
            Mouse.hide();
            _pointer.visible = true;
        }
    }

    protected function rollOutHandler (event :MouseEvent) :void
    {
        // fake rollouts can happen when the mouse lands on a loader or some other piece of media
        // that the mx framework doesn't like. we sort these out manually.
        if (! (event.relatedObject is Application || event.relatedObject is UIComponent)) {
            return;
        }

        // kluge warning: some versions of the flash player also trigger an erroneous rollout
        // when we add the mouse cursor bitmap right underneath the mouse pointer (even though
        // the bitmap is a child object, and shouldn't cause a rollout, just a mouseout). 
        if (event.relatedObject == _pointer) {
            return;
        }

        // it's a real rollout. hide the custom pointer and show the standard one.
        _mouseDownAnchor = null;
        if (_pointer != null) {
            _pointer.visible = false;
        }
        Mouse.show();
    }

    protected function mouseDownHandler (event :MouseEvent) :void
    {
        _mouseDownAnchor = new Point(_wrapper.mouseX, _wrapper.mouseY);
        updatePointerImage(false);
    }
    
    protected function mouseMoveHandler (event :MouseEvent) :void
    {
        updatePointerPosition();

        // do mode-specific processing
        switch (_mouseMode) {
            
        case MOUSE_MODE_OFFSET:
            if (_mouseDownAnchor != null) {
                // get the mouse position delta in decor pixels
                var deltaPx :Point = new Point(_wrapper.mouseX - _mouseDownAnchor.x,
                                               _wrapper.mouseY - _mouseDownAnchor.y);
                // convert delta to be in room coordinates (normalize + vertical flip)
                _deltaX =   deltaPx.x / _decor.width;
                _deltaY = - deltaPx.y / _decor.height;   
                _offsetXBox.text = String(MathUtil.clamp(_decor.offsetX + _deltaX, -1, 1));
                _offsetYBox.text = String(MathUtil.clamp(_decor.offsetY + _deltaY, -1, 1));

                refreshPreview();
            }
            break;
            
        case MOUSE_MODE_HORIZON:
            if (event.buttonDown) {
                // set horizon level to the normalized mouse position
                _horizonYBox.text =
                    String(1 - MathUtil.clamp(_wrapper.mouseY / _decor.height, 0, 1));
                
                advancedOptionsChanged(null);
            }
            break;
        }
    }

    protected function mouseUpHandler (event :MouseEvent) :void
    {
        _mouseDownAnchor = null;
        
        if (_mouseMode == MOUSE_MODE_OFFSET) {
            // update the decor offset
            _deltaX = _deltaY = 0;
            advancedOptionsChanged(null);
        }
        
        updatePointerImage(true);
    }

    protected function horizonModeSelectionHandler (selected :Boolean) :void
    {
        _offsetMode.selected = false;
        setMouseMode(selected ? MOUSE_MODE_HORIZON : MOUSE_MODE_DEFAULT);
    }
    
    protected function offsetModeSelectionHandler (selected :Boolean) :void
    {
        _horizonMode.selected = false;
        setMouseMode(selected ? MOUSE_MODE_OFFSET : MOUSE_MODE_DEFAULT);
    }

    
    // TEMP: helper function
    protected function dlog (message :String) :void
    {
        if (_testing) {
            _results.text = message;
        }
    }

    /** Helper function - converts any NaN values into defaults */
    protected function norm (value :*, defaultValue :*) :* {
        return isNaN(value) ? defaultValue : value;
    }

    protected static const PREVIEW_BOX_WIDTH :Number = 550;
    protected static const PREVIEW_BOX_HEIGHT :Number = 300;

    protected static const MOUSE_MODE_DEFAULT :int = 0;
    protected static const MOUSE_MODE_OFFSET :int = 1;
    protected static const MOUSE_MODE_HORIZON :int = 2;

    protected var _testing :Boolean = true;

    // panels containing other control options
    protected var _mousePanel :Container;
    protected var _optionPanel :Container;

    // standard options
    protected var _results :Label;
    protected var _scaleLabel :Label;
    protected var _depthSlider :HSlider;
    protected var _types :ComboBox;
    protected var _offsetMode :CommandCheckBox;
    protected var _horizonMode :CommandCheckBox;

    // advanced options
    protected var _widthBox :TextInput;
    protected var _heightBox :TextInput;
    protected var _depthBox :TextInput;
    protected var _horizonYBox :TextInput;
    protected var _horizonXBox :TextInput;
    protected var _offsetXBox :TextInput;
    protected var _offsetYBox :TextInput;
    protected var _hideWallsBox :CommandCheckBox;

    // room preview
    protected var _preview :Canvas;
    protected var _mediaPath :String;
    protected var _media :DecorMediaContainer;
    protected var _wrapper :UIComponent;
    protected var _backdropCanvas :Canvas;
    protected var _mouseMode :int = MOUSE_MODE_DEFAULT;
    protected var _mouseDownAnchor :Point;

    // misc
    protected var _decor :Decor = new Decor();
    protected var _backdrop :RoomBackdrop = new RoomBackdrop();
    protected var _pointer :Image;
    protected var _pointers :Array; // of Array[2] of Image, built from POINTERS definitions

    // temporary display-only values, used while dragging the background
    protected var _deltaX :Number = 0;
    protected var _deltaY :Number = 0;

    // configure log levels
    MsoyLogConfig.init();

    [Embed(source="../../../../../../../rsrc/media/mouse_pointers/hand_open.png")]
    protected static const HAND_OPEN :Class;
    [Embed(source="../../../../../../../rsrc/media/mouse_pointers/hand_closed.png")]
    protected static const HAND_CLOSED :Class;
    [Embed(source="../../../../../../../rsrc/media/mouse_pointers/horizon_target_up.png")]
    protected static const HORIZON_TARGET_UP :Class;
    [Embed(source="../../../../../../../rsrc/media/mouse_pointers/horizon_target_down.png")]
    protected static const HORIZON_TARGET_DOWN :Class;

    protected static const POINTERS :Array =
        [ [ null, null ], // default
          [ HAND_OPEN, HAND_CLOSED ], // offset
          [ HORIZON_TARGET_UP, HORIZON_TARGET_DOWN ] // horizon
            ];
}
}


import com.threerings.msoy.room.client.DecorViewerComp;
import com.threerings.flash.MediaContainer;

/**
 * Helper class, extends MediaContainer by notifying listeners once the media was loaded.
 */
internal class DecorMediaContainer extends MediaContainer
{
    public function DecorMediaContainer (viewer :DecorViewerComp)
    {
        _viewer = viewer;
    }

    override protected function stoppedLoading () :void
    {
        _viewer.refreshPreview();
    }

    public var _viewer :DecorViewerComp;
}


