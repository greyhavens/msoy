package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.SimpleButton;
import flash.display.Sprite;
import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.geom.Rectangle;
import flash.ui.Keyboard;

import mx.containers.Canvas;
import mx.containers.HBox;
import mx.controls.Button;
import mx.core.Container;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.io.TypedArray;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.msoy.world.data.ModifyFurniUpdate;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;


/**
 * Handles "live-editing" of a piece of furni inside a room.
 */
public class FurniEditController
{
    /** States for this editor. */
    public static const EDIT_OFF :String = "EditOff";
    public static const EDIT_SHOWMENU :String = "EditShowMenu";
    public static const EDIT_MOVE :String = "EditMove";
    public static const EDIT_RESIZE :String = "EditResize";

    /** Constructor. */
    public function FurniEditController ()
    {
        // for each editing state, make a list of listeners that will be initialized
        // when we enter the state, and removed when we leave the state.
        _listeners = {
            EditOff:      [ ], // no special listeners
            EditShowMenu: [ [ KeyboardEvent.KEY_DOWN, handleKeyboardInMenuMode ] ],
            EditMove:     [ [ MouseEvent.MOUSE_MOVE, handleMouseInMoveMode ],
                            [ MouseEvent.CLICK, handleClickInMoveMode ],
                            [ KeyboardEvent.KEY_DOWN, handleKeyboardInMoveMode] ],
            EditResize:   [ [ MouseEvent.MOUSE_MOVE, handleMouseInResizeMode ],
                            [ MouseEvent.CLICK, handleClickInResizeMode ],
                            [ KeyboardEvent.KEY_DOWN, handleKeyboardInResizeMode ] ]
            };

        // create a collection of button definitions. handlers are closures on this instance.
        _menuButtons = [
            { button: null, icon: MOVE_ICON, text: null, handler: move },
            { button: null, icon: RESIZE_ICON, text: null, handler: resize },
            { button: null, icon: HFLIP_ICON, text: null, handler: hflip },
            { button: null, icon: CANCEL_ICON, text: null, handler: cancel },
            { button: null, icon: null, text: "b.edit_furni_ok", handler: commit } ];

        // now create button display objects for each definition
        _buttonPanel = new HBox();
        _buttonPanel.visible = false;
        _buttonPanel.setStyle("horizontalGap", 0);
        for each (var def :Object in _menuButtons) {
                var button :Button = new Button();
                button.styleName = "furniEditButton";    
                button.height = 20;
                if (def.icon != null) {
                    button.setStyle("icon", def.icon as Class);
                    button.width = 20;
                }
                if (def.text != null) {
                    button.label = Msgs.GENERAL.get(def.text);
                }
                if (def.handler != null) {
                    addButtonListener(button, def.handler);
                }
                def.button = button; // store this instance back in the button definition

                _buttonPanel.addChild(button);
        }

        _buttonPanel.width = 150; // oh flex, you're so silly. why must I set these manually?
        _buttonPanel.height = 20;

    }
    
    /** Wrapper around the event handler function generator */
    protected function addButtonListener (button :DisplayObject, thunk :Function) :void
    {
        // this silly wrapper is necessary because anonymous functions in AS are not
        // properly scoped, and they access variables from the parent stack frame.
        // so we need to manually create a fresh stack frame for each new function declaration,
        // by calling it through this wrapper.
        button.addEventListener(
            MouseEvent.CLICK,
            function (event :MouseEvent) :void
            {
                thunk();
                event.stopPropagation();
            });
    }

    /**
     * Returns info about the current editing mode, as one of the EDIT_* constants.
     */
    public function getMode () :String
    {
        return _mode;
    }

    /**
     * Switches to a new editing mode, where the mode is one of the EDIT_* constants.
     */
    protected function setMode (newMode :String) :void
    {
        var oldMode :String = _mode;
        _mode = newMode;

        addOrRemoveListeners (oldMode, false);
        addOrRemoveListeners (newMode, true);

        if (newMode == EDIT_SHOWMENU) {
            updatePanelPosition();
            _buttonPanel.visible = true;
        } else {
            _buttonPanel.visible = false;
        }
    }

    /** Moves the button panel to somewhere near the furni being edited. */
    protected function updatePanelPosition () :void
    {
        var location :Point = _furni.getRect(_roomView).topLeft; // in room view coordinate space
        var room :Rectangle = _roomView.scrollRect;

        if (room != null) {
            // clamp the location to be always in view
            location.x = Math.max(Math.min(location.x, room.right - _buttonPanel.width), room.x);
            location.y = Math.max(Math.min(location.y, room.bottom - _buttonPanel.height), room.y);
        }
        
        var c :Point = _container.globalToLocal(_roomView.localToGlobal(location));
        _buttonPanel.x = c.x;  
        _buttonPanel.y = c.y;  
    }
    
    /**
     * Set up any notifications necessary to start editing a piece of furni.
     * @param furni an instance of FurniSprite being edited
     * @param roomView a room in which the sprite is edited
     * @param endCallback a function that takes a TypedArray of SceneUpdate instances:
     *           <pre>function (edits :TypedArray) :void { }</pre>
     *        that will be called once the editing has ended (whether it was committed
     *        or cancelled).
     */
    public function start (furni :FurniSprite, roomView :RoomView, scene :MsoyScene,
                           ctx :WorldContext, endCallback :Function) :void
    {
        _furni = furni;
        _roomView = roomView;
        _scene = scene;
        _endCallback = endCallback;
        _positionAtShift = null;
        _container = ctx.getTopPanel().getPlaceContainer();
        
        _container.addChild(_buttonPanel);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_DOWN, keyboardHandler);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_UP, keyboardHandler);
        _roomView.stage.addEventListener(Event.RESIZE, resizeHandler);

        setMode(EDIT_SHOWMENU);

        // make a copy of furni data, so we can modify it in peas
        _originalFurniData = furni.getFurniData().clone() as FurniData;
    }

    /**
     * Try to commit the edits to the server, and end editing.
     * This will save the results, and shut down the editor (calling the endCallback function).
     */
    protected function commit () :void
    {
        // tell the server
        if (! _originalFurniData.equivalent(_furni.getFurniData())) {
            var edits :TypedArray = TypedArray.create(SceneUpdate);
            var oldFurni :TypedArray = TypedArray.create(FurniData);
            var changedFurni :TypedArray = TypedArray.create(FurniData);
            var sceneId :int = _scene.getId();
            var version :int = _scene.getVersion();
            var furniUpdate :ModifyFurniUpdate = new ModifyFurniUpdate();

            oldFurni.push(_originalFurniData);
            changedFurni.push(_furni.getFurniData());
            furniUpdate.initialize(sceneId, version++, oldFurni, changedFurni);
            edits.push(furniUpdate);
        }
        deinit(edits);
    }

    /**
     * Cancel whatever editing we've been doing.
     * This will restore last known sprite data, and shut down the editor
     * (calling the endCallback function).
     */
    public function cancel () :void
    {
        moveFurni(_originalFurniData.loc, true);
        scaleFurni(new Point(_originalFurniData.scaleX, _originalFurniData.scaleY));
        deinit(null);
    }

    /** Start moving the sprite. */
    protected function move () :void {
        setMode(EDIT_MOVE);
    }
    
    /** Start resizing the sprite. */
    protected function resize () :void {
        setMode(EDIT_RESIZE);
    }

    /** Just invert the horizontal scaling factor. */
    protected function hflip () :void {
        scaleFurni(new Point(- _furni.getMediaScaleX(), _furni.getMediaScaleY()));
    }

    /**
     * Shut down any furni editing data, and call the endCallback function.
     */
    protected function deinit (edits :TypedArray) :void
    {
        setMode(EDIT_OFF);

        _roomView.stage.removeEventListener(KeyboardEvent.KEY_DOWN, keyboardHandler);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_UP, keyboardHandler);
        _roomView.stage.removeEventListener(Event.RESIZE, resizeHandler);
        _container.removeChild(_buttonPanel);
        
        _endCallback(edits);

        _originalFurniData = null;
        _container = null;
        _positionAtShift = null;
        _endCallback = null;
        _scene = null;
        _roomView = null;
        _furni = null;
    }
    
    /**
     * Given a mode name, looks up listener information in the _listeners collection,
     * and either adds or removes the appropriate listeners (based on the addListener variable).
     */
    protected function addOrRemoveListeners (mode :String, addListener :Boolean) :void
    {
        var listeners :Array = _listeners[mode];        // get listener entries for this mode
        if (listeners != null) {
            for each (var pair :Array in listeners)           // iterate over all entries
            {
                var type :String = pair[0] as String;         // get event type
                var listener :Function = pair[1] as Function; // get event handler
                
                // first, figure out which entity we're working with (keyboard events want
                // subscription on the stage object, everyone else on the room view).
                var target :DisplayObject =
                    (type == KeyboardEvent.KEY_DOWN || KeyboardEvent.KEY_UP)
                    ? _roomView.stage : _roomView;
                
                // second, are we adding or removing?
                var listenerSubscriptionFn :Function =
                    addListener ? target.addEventListener : target.removeEventListener;

                // now add or remove!
                listenerSubscriptionFn(type, listener);
            }
        } else {
            Log.getLog(this).warning(
                "FurniEditController: could not find listeners for mode " + mode);
        }
    }


    
    // FURNI MOVEMENT 

    protected function moveFurni (loc :MsoyLocation, updateFurniData :Boolean = false) :void
    {
        _furni.setLocation(loc);
        if (updateFurniData) {
            _furni.getFurniData().loc = loc;
        }
    }
    
    /** Handles mouse movement during furni movement. */
    protected function handleMouseInMoveMode (event :MouseEvent) :void
    {
        var cloc :ClickLocation =
            _roomView.layout.pointToLocation(event.stageX, event.stageY, _positionAtShift);
        
        if (cloc.click == ClickLocation.FLOOR) {
            moveFurni(cloc.loc, false);
        }
    }

    /** Handles mouse clicks during furni movement. */
    protected function handleClickInMoveMode (event :MouseEvent) :void
    {
        var cloc :ClickLocation =
            _roomView.layout.pointToLocation(event.stageX, event.stageY, _positionAtShift);
        
        if (cloc.click == ClickLocation.FLOOR) {
            moveFurni(cloc.loc, true);
            setMode(EDIT_SHOWMENU);
        }
    }

    /** Handles key presses during furni movement  */
    protected function handleKeyboardInMoveMode (event :KeyboardEvent) :void
    {
        if (event.keyCode == Keyboard.ESCAPE) {
            moveFurni(_furni.getFurniData().loc, false); // revert to the last stored value
            setMode(EDIT_SHOWMENU);
        }

        event.updateAfterEvent();
    }


    
    // FURNI RESIZE

    protected function scaleFurni (scale :Point) :void
    {
        _furni.setMediaScaleX(scale.x);
        _furni.setMediaScaleY(scale.y);
    }

    /** Given some width, height values in screen coordinates, finds x and y scaling factors
     *  that would resize the current furni to those coordinates. */
    protected function computeScale (width :Number, height :Number) :Point
    {
        const e :Number = 0.1; // zero scale will get bumped up to this value
        
        // get current size info in pixels
        var oldwidth :Number = Math.max(_furni.getActualWidth(), 1);
        var oldheight :Number = Math.max(_furni.getActualHeight(), 1);

        // figure out the proportion of pixels per scaling unit that produced old width and height
        var xProportions :Number = Math.max(Math.abs(oldwidth / _furni.getMediaScaleX()), 1);
        var yProportions :Number = Math.max(Math.abs(oldheight / _furni.getMediaScaleY()), 1);

        // find new scaling ratios for the desired width and height
        var newScaleX :Number = width / xProportions;
        var newScaleY :Number = height / yProportions;
        newScaleX = (newScaleX != 0 ? newScaleX : e);
        newScaleY = (newScaleY != 0 ? newScaleY : e);
        
        return new Point(newScaleX, newScaleY);
    }

    /** Finds x and y scaling factors that will resize the current furni based on mouse position. */
    protected function findScale (event :MouseEvent) :Point
    {
        // find hotspot position in terms of sprite width and height
        var hotspot :Point = _furni.getLayoutHotSpot();
        var px :Number = hotspot.x / _furni.getActualWidth();  
        var py :Number = hotspot.y / _furni.getActualHeight(); 

        // find pixel distance from hotspot to mouse pointer
        var pivot :Point = _furni.localToGlobal(hotspot);      
        var dx :Number = event.stageX - pivot.x; // positive to the right of hotspot
        var dy :Number = pivot.y - event.stageY; // positive above hotspot

        // convert pixel position to how wide and tall the furni would have to be in order
        // to reach that position - and pass it into the scaling function.
        return computeScale(dx / px, dy / py); 
    }
    
    /** Handles mouse movement during furni resize. */
    protected function handleMouseInResizeMode (event :MouseEvent) :void
    {
        scaleFurni(findScale(event));
    }

    /** Handles mouse clicks during furni resize. */
    protected function handleClickInResizeMode (event :MouseEvent) :void
    {
        scaleFurni(findScale(event));
        setMode(EDIT_SHOWMENU);
    }

    /** Handles key presses during furni resize.  */
    protected function handleKeyboardInResizeMode (event :KeyboardEvent) :void
    {
        if (event.keyCode == Keyboard.ESCAPE) {
            scaleFurni(new Point(_originalFurniData.scaleX, _originalFurniData.scaleY)); 
            setMode(EDIT_SHOWMENU);
        }

        event.updateAfterEvent();
    }


    // MENU MODE
    
    /** Handles key presses during furni resize.  */
    protected function handleKeyboardInMenuMode (event :KeyboardEvent) :void
    {
        if (event.keyCode == Keyboard.ESCAPE) {
            cancel()
        }
    }

    
    // GENERAL EVENT HANDLERS

    /**
     * Keeps track of the different keys used to modify edit settings.
     */
    protected function keyboardHandler (event :KeyboardEvent) :void
    {
        if (event.keyCode == Keyboard.SHIFT) {
            if (event.type == KeyboardEvent.KEY_DOWN) {
                _positionAtShift = new Point(_roomView.stage.mouseX, _roomView.stage.mouseY);
            } else {
                _positionAtShift = null;
            }
        }

        event.updateAfterEvent();
    }

    /**
     * When the window resizes, update the Flex UI manually.
     */
    protected function resizeHandler (event :Event) :void
    {
        if (_buttonPanel.visible = true) {
            updatePanelPosition();
        }
    }


    /** Flex container for the scene. */       
    protected var _container :Container;
    
    /** Sprite being edited. */
    protected var _furni :FurniSprite;

    /** Backup copy of the original furni data, before we started editing it. */
    protected var _originalFurniData :FurniData;
    
    /** Room view in which the editing happens. */
    protected var _roomView :RoomView;

    /** Pointer to the current scene. */
    protected var _scene :MsoyScene;

    /** Function to be called at the end of editing. */
    protected var _endCallback :Function;

    /** Current editing mode, as one of the EDIT_* constant values. */
    protected var _mode :String = EDIT_OFF;

    /** The last mouse position (in stage coordinates!) before the user hit "shift"
     *  to switch to vertical positioning. */
    protected var _positionAtShift :Point;

    /**
     * All editing buttons are defined in this array. Each array entry is an object
     * with the following fields:
     *   icon - a Class object of button's icon (can be null)
     *   text - a string that contains button label (can be null)
     *   click - a MouseEvent handler that will process a mouse click on this button
     *   button - points to an instance of a DisplayObject for this button
     */
    protected var _menuButtons :Array; 

    /** Canvas that contains editing buttons. */
    protected var _buttonPanel :Container;
    
    /**
     * Collection of event listeners for each editing state. This collection maps from state
     * name to an array of listener entries, where each entry is an array of two elements:
     * first an event type string, and second an event handler function.
     */
    protected var _listeners :Object;

    // Button media. 
    [Embed(source="../../../../../../../rsrc/media/skins/button/furniedit/move_root.png")]
    protected static const MOVE_ICON :Class;
    [Embed(source="../../../../../../../rsrc/media/skins/button/furniedit/resize_root.png")]
    protected static const RESIZE_ICON :Class;
    [Embed(source="../../../../../../../rsrc/media/skins/button/furniedit/flip_root.png")]
    protected static const HFLIP_ICON :Class;
    [Embed(source="../../../../../../../rsrc/media/skins/button/backtolobby.png")] // TEMP
    protected static const CANCEL_ICON :Class;
}
}
    


