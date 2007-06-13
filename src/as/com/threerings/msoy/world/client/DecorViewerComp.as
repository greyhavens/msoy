//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.Graphics;
import flash.external.ExternalInterface;
import flash.events.Event;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.ui.Mouse;
import flash.utils.ByteArray;

import mx.containers.Canvas;
import mx.containers.Grid;
import mx.containers.VBox;
import mx.controls.CheckBox;
import mx.controls.ComboBox;
import mx.controls.Label;
import mx.controls.Image;
import mx.controls.HSlider;
import mx.controls.TextInput;
import mx.core.Application;
import mx.core.BitmapAsset;
import mx.core.UIComponent;
import mx.core.ScrollPolicy;
import mx.events.SliderEvent;
import mx.resources.ResourceBundle;

import com.threerings.flash.MathUtil;
import com.threerings.flex.GridUtil;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.util.MessageManager;
import com.threerings.msoy.client.Msgs;

 
public class DecorViewerComp extends Canvas
{
    [ResourceBundle("global")]
    [ResourceBundle("editing")]

    public static const log :Log = Log.getLog(DecorViewerComp);

    public function DecorViewerComp ()
    {
        if (ExternalInterface.available) {
            try {
                // hook up our ffi
                ExternalInterface.addCallback("updateParameters", updateParameters);
                ExternalInterface.addCallback("updateMedia", updateMedia);
            } catch (err :Error) {
                log.warning("External interface initialization failed: " + err);
            }
        } else {
            log.warning("External interface not available!");
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
        vbox.addChild(mouseopts);
        
        _horizonMode = new CheckBox();
        _horizonMode.label = Msgs.EDITING.get("b.move_horizon");
        _horizonMode.addEventListener(MouseEvent.CLICK, horizonModeSelectionHandler);
        _offsetMode = new CheckBox();
        _offsetMode.label = Msgs.EDITING.get("b.move_offset");
        _offsetMode.addEventListener(MouseEvent.CLICK, offsetModeSelectionHandler);
        _offsetMode.enabled = false; // for now :)

        GridUtil.addRow(mouseopts, Msgs.EDITING.get("l.move_selection"),
                        _horizonMode, _offsetMode);


        // container for standard options
        
        var standard :Grid = new Grid();
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

        _hideWallsBox = new CheckBox();
        _hideWallsBox.label = Msgs.EDITING.get("l.hide_walls");
        _hideWallsBox.addEventListener(Event.CHANGE, regularOptionsChanged);
        _hideWallsBox.enabled = false;
            
        GridUtil.addRow(standard, Msgs.EDITING.get("l.scene_type"), _types,
                        Msgs.EDITING.get("l.scene_depth"), _depthSlider, _hideWallsBox);

        
        // container for advanced options

        var advanced :Grid = new Grid();
        vbox.addChild(advanced);

        GridUtil.addRow(advanced, Msgs.EDITING.get("l.scene_dimensions"),
                        _widthBox = new TextInput(), _heightBox = new TextInput(),
                        _depthBox = new TextInput(), Msgs.EDITING.get("l.horizon"),
                        _horizonXBox = new TextInput(), _horizonYBox = new TextInput());
        
        for each (var input :TextInput in
                  [ _widthBox, _heightBox, _depthBox, _horizonXBox, _horizonYBox ]) {
            input.width = 40;
            input.addEventListener(Event.CHANGE, advancedOptionsChanged);
        }

        _horizonXBox.visible = _horizonXBox.includeInLayout = false; // for now
        
        // init pointer resources. produces a tree of the same topology as POINTERS,
        // only containing references to initialized Image objects.
        _pointers = new Array();
        for each (var def :Array in POINTERS) {
            var pointer :Array = new Array();
            for each (var c :Class in def) {
                if (c != null) {
                    var image :Image = new Image();
                    image.source = new c() as BitmapAsset;
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
                log.warning("Unable to initialize updates with Javascript: " + e);
            }
        } 
    }

    // @Override from Canvas
    override protected function childrenCreated () :void
    {
        _preview.addEventListener(MouseEvent.MOUSE_DOWN, mouseDownHandler);
        _preview.addEventListener(MouseEvent.MOUSE_MOVE, mouseMoveHandler);
        _preview.addEventListener(MouseEvent.MOUSE_UP, mouseUpHandler);
        _preview.addEventListener(MouseEvent.ROLL_OVER, rollOverHandler);
        _preview.addEventListener(MouseEvent.ROLL_OUT, rollOutHandler);
    }

      
            
    /**
     * Called whenever any of the UI elements changes.
     */
    protected function regularOptionsChanged (event :Event) :void
    {
        // update data from sliders
        _data.depth = _depthSlider.value;
        _data.type = _types.selectedIndex;
        _data.hideWalls = _hideWallsBox.selected;

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
        _data.width = norm(int(_widthBox.text), 1);
        _data.height = norm(int(_heightBox.text), 1);
        _data.depth = norm(int(_depthBox.text), 1);
        _data.horizon = norm(Number(_horizonYBox.text), 0);

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
        _data.width = width;
        _data.height = height;
        _data.depth = depth;
        _data.type = type;
        _data.horizon = horizon;
        _data.offsetX = offsetX;
        _data.offsetY = offsetY;
        _data.hideWalls = hideWalls;

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
        _depthSlider.value = _data.depth;
        _types.selectedIndex = _data.type;
        _hideWallsBox.selected = _data.hideWalls;
    }

    /**
     * Refreshes advanced UI controls from data.
     */
    protected function refreshAdvancedUI () :void
    {
        _widthBox.text = String(_data.width);
        _heightBox.text = String(_data.height);
        _depthBox.text = String(_data.depth);
        _horizonYBox.text = String(_data.horizon);
    }
    
    /**
     * Refreshes the preview screen.
     */
    public function refreshPreview () :void
    {
        // redraw the room backdrop
        _backdrop.setRoom(_data.width, _data.height, _data.depth, _data.horizon, _data.type);
        _backdrop.drawRoom(
            _backdropCanvas.graphics, _backdropCanvas.width, _backdropCanvas.height, true, false);

        // scale the preview box. ideally, we want a 50% scaling factor, but if that's too big,
        // just shrink the whole thing to fit.
        var scale :Number = 0.5;
        if (_data.width * scale > PREVIEW_BOX_WIDTH || _data.height * scale > PREVIEW_BOX_HEIGHT) {
            scale = Math.min (PREVIEW_BOX_WIDTH / _data.width, PREVIEW_BOX_HEIGHT / _data.height);
        }
        
        // scale the bitmap container and the backdrop canvas
        _backdropCanvas.scaleX = _backdropCanvas.scaleY = scale;
        _wrapper.scaleX = _wrapper.scaleY = scale;

        // center the bitmap horizontally, and align vertically with the bottom of the room
        _media.x = (_data.width - _media.width) / 2;
        _media.y = _data.height - _media.height;

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
                    "updateDecor", _data.width, _data.height, _data.depth, _data.horizon,
                    _data.type, _data.offsetX, _data.offsetY, _data.hideWalls);
            } catch (e :Error) {
                log.warning("Unable to send update to Javascript: " + e);
            }
        } else {
            log.warning("External interface not available, " +
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
        if (! (event.relatedObject is Application)) {
            // ignore any phantom rollouts that happen when the mouse lands on a loader or
            // other piece of media the mx framework doesn't like. we only care about
            // rollouts back to the application background.
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
    
    protected function mouseUpHandler (event :MouseEvent) :void
    {
        _mouseDownAnchor = null;
        updatePointerImage(true);
    }

    protected function mouseMoveHandler (event :MouseEvent) :void
    {
        updatePointerPosition();

        // do mode-specific processing
        switch (_mouseMode) {
            
        case MOUSE_MODE_OFFSET:
            if (_mouseDownAnchor != null) {
                // get the mouse position delta in decor pixels
                var deltaPx :Point = new Point(
                    _mouseDownAnchor.x - _wrapper.mouseX, _mouseDownAnchor.y - _wrapper.mouseY);
                // normalize the delta
                var delta :Point = new Point(deltaPx.x / _data.width, deltaPx.y / _data.height);
                // dlog("D: " + delta); // todo
            }
            break;
            
        case MOUSE_MODE_HORIZON:
            if (event.buttonDown) {
                // set horizon level to the normalized mouse position
                _horizonYBox.text =
                    String(1 - MathUtil.clamp(_wrapper.mouseY / _data.height, 0, 1));
                
                advancedOptionsChanged(null);
            }
            break;
        }
    }

    protected function horizonModeSelectionHandler (event :MouseEvent) :void
    {
        _offsetMode.selected = false;
        setMouseMode(_horizonMode.selected ? MOUSE_MODE_HORIZON : MOUSE_MODE_DEFAULT);
    }
    
    protected function offsetModeSelectionHandler (event :MouseEvent) :void
    {
        _horizonMode.selected = false;
        setMouseMode(_offsetMode.selected ? MOUSE_MODE_OFFSET : MOUSE_MODE_DEFAULT);
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
    
    protected var _results :Label;
    protected var _scaleLabel :Label;
    protected var _depthSlider :HSlider;
    protected var _types :ComboBox;
    protected var _widthBox :TextInput;
    protected var _heightBox :TextInput;
    protected var _depthBox :TextInput;
    protected var _horizonYBox :TextInput;
    protected var _horizonXBox :TextInput;
    protected var _hideWallsBox :CheckBox;
    protected var _offsetMode :CheckBox;
    protected var _horizonMode :CheckBox;
    
    protected var _preview :Canvas;
    protected var _mediaPath :String;
    protected var _media :DecorMediaContainer;
    protected var _wrapper :UIComponent;
    protected var _backdropCanvas :Canvas;
    protected var _mouseMode :int = MOUSE_MODE_DEFAULT;
    protected var _mouseDownAnchor :Point;

    protected var _data :DecorStorage = new DecorStorage();
    protected var _backdrop :RoomBackdrop = new RoomBackdrop();
    protected var _pointer :Image;

    protected var _pointers :Array; // of Array[2] of Image, built from POINTERS definitions
        
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


import com.threerings.msoy.world.client.DecorViewerComp;
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

/**
 * Helper class, encapsulates relevant Decor parameters.
 */
internal class DecorStorage
{
    public var width :int;
    public var height :int;
    public var depth :int;
    public var horizon :Number;
    public var type :int;
    public var offsetX :Number;
    public var offsetY :Number;
    public var hideWalls :Boolean;
}

