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
import mx.controls.Spacer;
import mx.core.Container;
import mx.core.ScrollPolicy;
import mx.events.FlexEvent;

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

    /** State modifiers. */
    public static const MOD_NONE :String = "ModNone";
    public static const MOD_SHIFT :String = "ModShift";
    public static const MOD_CTRL :String = "ModCtrl";
    
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
            [ { icon: MOVE_ICON, text: null, handler: move },
              { icon: MOVE_Y_ICON, text: null, handler: ymove },
              { icon: MOVE_Z_ICON, text: null, handler: zmove } ],
            [ { icon: RESIZE_ICON, text: null, handler: resize },
              { icon: RESIZE_CONST_ICON, text: null, handler: resize_const },
              { icon: RESIZE_DEF_ICON, text: null, handler: resize_defaults } ],
            [ { icon: HFLIP_ICON, text: null, handler: hflip },
              { icon: VFLIP_ICON, text: null, handler: vflip } ]
            ];
    }

    /** Creates the furni editor UI. */
    protected function makeButtonPanel () :Container
    {
        // create a canvas with a background that sits in the upper left.
        //
        // since we can't control background alignment from styles, we need to create a separate
        // widget just for the background, and drop it at the right location. nice.
        
        var buttonPanel :Container = new Canvas(); 
        buttonPanel.visible = false;
        buttonPanel.horizontalScrollPolicy = ScrollPolicy.OFF;
        buttonPanel.verticalScrollPolicy = ScrollPolicy.OFF;
        buttonPanel.width = 200;  // max width
        buttonPanel.height = 100; // more than necessary, to leave room for ribbons foldout

        var background :Container = new Canvas();
        background.horizontalScrollPolicy = ScrollPolicy.OFF;
        background.verticalScrollPolicy = ScrollPolicy.OFF;
        background.styleName = "furniEditPanel";
        background.x = background.y = 0;
        background.height = 44;
        buttonPanel.addChild(background);

        // cancel button in the upper right corner of the bitmap
        var bcancel :Button = new Button();
        bcancel.setStyle("icon", CANCEL_ICON);
        addButtonListener(bcancel, cancel);
        bcancel.styleName = "furniEditButton";
        bcancel.width = bcancel.height = 13;
        bcancel.setStyle("right", 2);
        bcancel.setStyle("top", 2);
        background.addChild(bcancel);

        // ok button and a bit of a spacer
        var elts :Container = new HBox();
        elts.styleName = "furniEditRibbon";
        elts.x = 66;
        elts.y = 20;
        background.addChild(elts);

        var bcommit :Button = new Button();
        bcommit.label = Msgs.GENERAL.get("b.edit_furni_ok");
        addButtonListener(bcommit, commit);
        bcommit.styleName = "furniEditButton";
        bcommit.height = 20;
        elts.addChild(bcommit);

        var spacer :Spacer = new Spacer;
        spacer.width = 4;
        elts.addChild(spacer);

        // a mini-panel for the ribbons
        var pulldowns :HBox = new HBox();
        buttonPanelHelper(_menuButtons, pulldowns);
        pulldowns.x = 3;
        pulldowns.y = 20;
        pulldowns.styleName = "furniEditRibbon";
        buttonPanel.addChild(pulldowns);
        
        
        return buttonPanel;
    }


    /** Helper function to create buttons. */
    protected function buttonPanelHelper (defs :Array, container :Container) :void
    {
        for each (var def :Object in defs) {
            if (def is Array) {  
                // create a new Ribbon container, and put all definitions in there
                var ribbon :Ribbon = new Ribbon();
                ribbon.styleName = "furniEditRibbon";
                buttonPanelHelper(def as Array, ribbon);
                container.addChild(ribbon);
                if (def.length > 0) {
                    ribbon.selectedChild = ribbon.getChildAt(0);
                    ribbon.collapsed = true;
                }
            } else {
                container.addChild(makeButton(def));
            }
        }
    }

    /** Create a Button instance based on the definition object. */
    protected function makeButton (def :Object) :Button
    {
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

        return button;
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
     * Switches to a new editing mode, where the mode is one of the EDIT_* constants,
     * and modifier is an optional value as one of the MOD_* constants (MOD_NONE by default).
     */
    protected function setMode (newMode :String, newModifier :String = MOD_NONE) :void
    {
        var oldMode :String = _mode;
        _mode = newMode;
        setModifier(newModifier);

        addOrRemoveListeners (oldMode, false);
        addOrRemoveListeners (newMode, true);

        if (newMode == EDIT_SHOWMENU) {
            updatePanelPosition();
            _buttonPanel.visible = true;
        } else {
            _buttonPanel.visible = false;
        }
    }

    /** Returns info about the current mode modifier, as one of the MOD_* constants. */
    public function getModifier () :String
    {
        return _modifier;
    }
    
    /** Sets the current mode modifier. */
    protected function setModifier (newModifier :String) :void
    {
        _modifier = newModifier;
        
        if (_modifier == MOD_NONE) {
            _modAnchor = null;
        } else {
            _modAnchor = _furni.getLocation();
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
        _modAnchor = null;
        _container = ctx.getTopPanel().getPlaceContainer();

        _buttonPanel = makeButtonPanel();
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
    protected function move () :void
    {
        setMode(EDIT_MOVE);
    }
    
    /** Start moving the sprite. */
    protected function ymove () :void
    {
        setMode(EDIT_MOVE, MOD_SHIFT);
    }
    
    /** Start moving the sprite. */
    protected function zmove () :void
    {
        setMode(EDIT_MOVE, MOD_CTRL);
    }
    
    /** Start resizing the sprite. */
    protected function resize () :void
    {
        setMode(EDIT_RESIZE);
    }

    /** Start resizing the sprite in proportion-constrained mode. */
    protected function resize_const () :void
    {
        setMode(EDIT_RESIZE, MOD_SHIFT);
    }

    /** Restore original resize values. */
    protected function resize_defaults () :void
    {
        scaleFurni(new Point(1, 1));
    }

    /** Invert the horizontal scaling factor. */
    protected function hflip () :void
    {
        scaleFurni(new Point(- _furni.getMediaScaleX(), _furni.getMediaScaleY()));
    }

    /** Invert the vertical scaling factor. */
    protected function vflip () :void
    {
        scaleFurni(new Point(_furni.getMediaScaleX(), - _furni.getMediaScaleY()));
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
        _buttonPanel = null;
        
        _endCallback(edits);

        _originalFurniData = null;
        _container = null;
        _modAnchor = null;
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

    protected function findNewFurniPosition (event :MouseEvent) :MsoyLocation
    {
        var currentPosition :Point = new Point(_roomView.stage.mouseX, _roomView.stage.mouseY);
        var cloc :ClickLocation = _roomView.layout.pointToFurniLocation(
            event.stageX, event.stageY, _modAnchor,
            (getModifier() == MOD_SHIFT ? RoomMetrics.N_UP : RoomMetrics.N_AWAY));
        
        return cloc.loc;
    }
    
    /** Handles mouse movement during furni movement. */
    protected function handleMouseInMoveMode (event :MouseEvent) :void
    {
        moveFurni(findNewFurniPosition(event), false);
    }

    /** Handles mouse clicks during furni movement. */
    protected function handleClickInMoveMode (event :MouseEvent) :void
    {
        moveFurni(findNewFurniPosition(event), true);
        setMode(EDIT_SHOWMENU);
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

    /**
     * Finds x and y scaling factors that will resize the current furni based on
     * mouse position.
     */
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
        // to reach that position
        var newwidth :Number = dx / px;
        var newheight :Number = dy / py;

        // if we're scaling proportionally, lock the two distances
        if (getModifier() == MOD_SHIFT) {
            // this math is broken and loses precision. todo: revisit.
            var proportion :Number =
                clampMagnitude (_furni.getActualWidth() / _furni.getActualHeight(), 0.01, 100);
            //trace("PROPORTION: " + proportion);
            
            if (Math.abs(newwidth) < Math.abs(newheight)) {
                newheight = clampMagnitude(newheight, 1, newwidth / proportion);
            } else {
                newwidth = clampMagnitude(newwidth, 1, newheight * proportion);
            }
        }

        // scale the furni!
        return computeScale(newwidth, newheight); 
    }

    /**
     * Returns the result of clamping the magnitude of /value/ to be no smaller than /lower/
     * magnitude, but no larger than /upper/ magnitude. Since only magnitude is clamped,
     * /lower/ and /higher/ sign values are ignored, and /value/'s original sign is preserved.
     */
    protected function clampMagnitude (value :Number, lower :Number, upper :Number) :Number
    {
        var sign :Number = value >= 0 ? 1 : -1;
        return sign * Math.max(Math.abs(lower), Math.min(Math.abs(value), Math.abs(upper)));
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
        if (event.type == KeyboardEvent.KEY_DOWN) {
            switch (event.keyCode) {
            case Keyboard.SHIFT:
                setModifier(MOD_SHIFT);
                break;
            case Keyboard.CONTROL:
                setModifier(MOD_CTRL);
                break;
            default:
                setModifier(MOD_NONE);
                break;
            }
        } else {
            setModifier(MOD_NONE);
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

    /** Current editing mode modifier, as one of the MOD_* constant values. */
    protected var _modifier :String = MOD_NONE;

    /**
     * The last furni position, in room coordinates, before the user hit a mod key
     * or selected a mod sub-option.
     */
    protected var _modAnchor :MsoyLocation;

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
    [Embed(source="../../../../../../../rsrc/media/skins/button/furniedit/move_Y.png")]
    protected static const MOVE_Y_ICON :Class;
    [Embed(source="../../../../../../../rsrc/media/skins/button/furniedit/move_Z.png")]
    protected static const MOVE_Z_ICON :Class;
    [Embed(source="../../../../../../../rsrc/media/skins/button/furniedit/resize_root.png")]
    protected static const RESIZE_ICON :Class;
    [Embed(source="../../../../../../../rsrc/media/skins/button/furniedit/resize_constrained.png")]
    protected static const RESIZE_CONST_ICON :Class;
    [Embed(source="../../../../../../../rsrc/media/skins/button/furniedit/resize_defaults.png")]
    protected static const RESIZE_DEF_ICON :Class;
    [Embed(source="../../../../../../../rsrc/media/skins/button/furniedit/flip_root.png")]
    protected static const HFLIP_ICON :Class;
    [Embed(source="../../../../../../../rsrc/media/skins/button/furniedit/flip_vertical.png")]
    protected static const VFLIP_ICON :Class;
    [Embed(source="../../../../../../../rsrc/media/skins/button/furniedit/close.png")]
    protected static const CANCEL_ICON :Class;
}
}
    


