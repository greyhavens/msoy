package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.SimpleButton;
import flash.display.Sprite;
import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.ui.Keyboard;

import com.threerings.io.TypedArray;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.msoy.world.data.ModifyFurniUpdate;
import com.threerings.msoy.world.data.FurniData;
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
            EditShowMenu: [ [ MouseEvent.CLICK, handleClickOnMenu ] ],
            EditMove:     [ [ MouseEvent.MOUSE_MOVE, handleMouseInMoveMode ],
                            [ MouseEvent.CLICK, handleClickInMoveMode ] ]
            };

        // create a collection of button definitions. handlers are closures on this instance.
        _menuButtons = [
            { button: null, media: BUTTON_MOVE, handler: move },
            { button: null, media: BUTTON_RESIZE, handler: resize },
            { button: null, media: BUTTON_CANCEL, handler: cancel },
            { button: null, media: BUTTON_COMMIT, handler: commit } ];

        // now create button display objects for each definition
        _buttonPanel = new Sprite();
        var y :int = 0;
        for each (var def :Object in _menuButtons) {
            var button :SimpleButton = new SimpleButton();
            for (var prop :String in def.media) {
                button[prop] = new (def.media[prop] as Class)() as DisplayObject;
            }

            button.useHandCursor = true;
            button.y = y;

            def.button = button;
            _buttonPanel.addChild(button);
            y += button.height;
        }

       
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
    public function setMode (newMode :String) :void
    {
        var oldMode :String = _mode;
        _mode = newMode;

        removeListenersForMode (oldMode);
        addListenersForMode (newMode);

        if (newMode == EDIT_SHOWMENU) {
            _buttonPanel.visible = true;
            _buttonPanel.x = _furni.x;
            _buttonPanel.y = _furni.y;
        } else {
            _buttonPanel.visible = false;
        }
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
    public function start (
        furni :FurniSprite, roomView :RoomView, scene :MsoyScene, endCallback :Function) :void
    {
        _furni = furni;
        _roomView = roomView;
        _scene = scene;
        _endCallback = endCallback;
        _positionAtShift = null;

        _roomView.addChild(_buttonPanel);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_DOWN, keyboardHandler);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_UP, keyboardHandler);

        setMode(EDIT_SHOWMENU);

        // make a copy of furni data, so we can modify it in peas
        _furniData = furni.getFurniData().clone() as FurniData;
    }

    /**
     * Try to commit the edits to the server, and end editing.
     * This will save the results, and shut down the editor (calling the endCallback function).
     */
    protected function commit () :void
    {
        // tell the server
        if (! _furniData.equivalent(_furni.getFurniData())) {
            var edits :TypedArray = TypedArray.create(SceneUpdate);
            var changedFurni :TypedArray = TypedArray.create(FurniData);
            var sceneId :int = _scene.getId();
            var version :int = _scene.getVersion();
            var furniUpdate :ModifyFurniUpdate = new ModifyFurniUpdate();

            changedFurni.push(_furni.getFurniData());
            furniUpdate.initialize(sceneId, version++, null, changedFurni);
            edits.push(furniUpdate);
        }
        deinit(edits);
    }

    /**
     * Cancel whatever editing we've been doing.
     * This will restore last known sprite data, and shut down the editor
     * (calling the endCallback function).
     */
    protected function cancel () :void
    {
        // restore old furni data
        _furni.update(_furniData);
        
        deinit(null);
    }

    /** Start moving the sprite. */
    protected function move () :void {
        setMode(EDIT_MOVE);
    }
    
    /** Start resizing the sprite. */
    protected function resize () :void {
        // setMode(EDIT_RESIZE); - not hooked up yet
        setMode(EDIT_SHOWMENU);
    }

    /**
     * Shut down any furni editing data, and call the endCallback function.
     */
    protected function deinit (edits :TypedArray) :void
    {
        setMode(EDIT_OFF);

        _roomView.stage.removeEventListener(KeyboardEvent.KEY_DOWN, keyboardHandler);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_UP, keyboardHandler);
        _roomView.removeChild(_buttonPanel);
        
        _endCallback(edits);

        _furniData = null;
        _positionAtShift = null;
        _endCallback = null;
        _scene = null;
        _roomView = null;
        _furni = null;
    }


    
    /** Adds the appropriate set of listeners for this mode. */
    protected function addListenersForMode (mode :String) :void
    {
        modifyListeners(mode, _roomView.addEventListener);
    }

    /** Removes whatever listeners were added for this mode by addListenersForMode. */
    protected function removeListenersForMode (mode :String) :void
    {
        modifyListeners(mode, _roomView.removeEventListener);
    }

    /**
     * Given a mode name, looks up listener information in the _listeners collection,
     * and calls the specified listenerRegistrationFn function on each entry.
     */
    protected function modifyListeners (mode :String, listenerRegistrationFn :Function) :void
    {
        var listeners :Array = _listeners[mode];        // get listener entries for this mode
        if (listeners != null) {
            for each (var pair :Array in listeners) {   // iterate over all entries and...
                var type :String = pair[0] as String;
                var listener :Function = pair[1] as Function;
                listenerRegistrationFn(type, listener); // ... call the function on each entry
            }
        } else {
            Log.getLog(this).warning(
                "FurniEditController: could not find listeners for mode " + mode);
        }
    }

    
    /** Handles mouse movement during furni movement. */
    protected function handleMouseInMoveMode (event :MouseEvent) :void
    {
        var cloc :ClickLocation =
            _roomView.layout.pointToLocation(event.stageX, event.stageY, _positionAtShift);
        
        if (cloc.click == ClickLocation.FLOOR) {
            _furni.setLocation(cloc.loc);            
        }
    }

    /** Handles mouse clicks during furni movement. */
    protected function handleClickInMoveMode (event :MouseEvent) :void
    {
        var cloc :ClickLocation =
            _roomView.layout.pointToLocation(event.stageX, event.stageY, _positionAtShift);
        
        if (cloc.click == ClickLocation.FLOOR) {
            setMode(EDIT_SHOWMENU);
        }
    }

    /**
     * Handles mouse clicks in waiting mode.
     */
    protected function handleClickOnMenu (event :MouseEvent) :void
    {
        // Find the button that was clicked on. Yes, I'm searching for the button manually here,
        // and it sucks, but for some reason mouse events only get sent to the _roomView object,
        // but not its children. But why? 

        for each (var def :Object in _menuButtons) {
            var button :DisplayObject = def.button as DisplayObject;
            if (event.localX >= button.x &&
                event.localY >= button.y &&
                event.localX < button.x + button.width &&
                event.localY < button.y + button.height)
            {
                // button found, call the handler
                if (def.handler != null) {
                    (def.handler as Function)();
                }
            }
        }
        event.updateAfterEvent();
    }

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

    
    /** Sprite being edited. */
    protected var _furni :FurniSprite;

    /** Backup copy of the original furni data, before we started editing it. */
    protected var _furniData :FurniData;
    
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
     *   button - points to an instance of a DisplayObject for this button
     *   media - a collection of instances of Class for the button's states
     *   click - a MouseEvent handler that will process a mouse click on this button
     */
    protected var _menuButtons :Array; 

    /** Sprite that contains editing buttons. */
    protected var _buttonPanel :Sprite;
    
    /**
     * Collection of event listeners for each editing state. This collection maps from state
     * name to an array of listener entries, where each entry is an array of two elements:
     * first an event type string, and second an event handler function.
     */
    protected var _listeners :Object;

    // Button media. Right now we only have 'up' skins, but we also anticipate 'down' and 'over'
    [Embed(source="../../../../../../../rsrc/media/skins/button/furniture_edit_move_up.png")]
    protected static const BUTTON_MOVE_UP :Class;
    [Embed(source="../../../../../../../rsrc/media/skins/button/furniture_edit_resize_up.png")]
    protected static const BUTTON_RESIZE_UP :Class;
    [Embed(source="../../../../../../../rsrc/media/skins/button/furniture_edit_commit_up.png")]
    protected static const BUTTON_COMMIT_UP :Class;
    [Embed(source="../../../../../../../rsrc/media/skins/button/furniture_edit_cancel_up.png")]
    protected static const BUTTON_CANCEL_UP :Class;

    protected static const BUTTON_MOVE :Object = { upState: BUTTON_MOVE_UP };
    protected static const BUTTON_RESIZE :Object = { upState: BUTTON_RESIZE_UP };
    protected static const BUTTON_COMMIT :Object = { upState: BUTTON_COMMIT_UP };
    protected static const BUTTON_CANCEL :Object = { upState: BUTTON_CANCEL_UP };

}
}
    
