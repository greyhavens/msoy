package com.threerings.msoy.world.client {

import flash.events.EventDispatcher;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.ui.Keyboard;


/** Responsible for handling "live-editing" a piece of furni inside a room. */
public class FurniEditController
{
    public static const EDIT_OFF :int = 0;
    public static const EDIT_POSITION :int = 1;
    public static const EDIT_SCALE :int = 2;
    
    /**
     * Returns info about the current editing mode, as one of the EDIT_* constants.
     */
    public function get mode () :int
    {
        return _mode;
    }
    
    /**
     * Set up any notifications necessary to start editing a piece of furni.
     * @param furni an instance of FurniSprite being edited
     * @param roomView a room in which the sprite is edited
     * @param endCallback a function of the form: <pre>function () :void { }</pre>
     *        that will be called once the editing has ended (whether it was committed
     *        or cancelled).
     */
    public function start (
        furni :FurniSprite, roomView :RoomView, endCallback :Function) :void
    {
        _furni = furni;
        _roomView = roomView;
        _endCallback = endCallback;

        _roomView.addEventListener(KeyboardEvent.KEY_DOWN, keyboardHandler);
        _roomView.addEventListener(KeyboardEvent.KEY_UP, keyboardHandler);
        _roomView.addEventListener(MouseEvent.MOUSE_MOVE, furniEditMoveHandler);
        _roomView.addEventListener(MouseEvent.CLICK, furniEditClickHandler);

        _mode = EDIT_POSITION;
    }

    /**
     * Try to commit the edits to the database, and end editing.
     * This will save the results, and shut down the editor (calling the endCallback function).
     */
    public function commit () :void
    {
        // db stuff here
        deinit();
    }

    /**
     * Shut down any furni editing data, and call the endCallback function.
     */
    protected function deinit () :void
    {
        _roomView.removeEventListener(KeyboardEvent.KEY_DOWN, keyboardHandler);
        _roomView.removeEventListener(KeyboardEvent.KEY_UP, keyboardHandler);
        _roomView.removeEventListener(MouseEvent.MOUSE_MOVE, furniEditMoveHandler);
        _roomView.removeEventListener(MouseEvent.CLICK, furniEditClickHandler);

        _endCallback();

        _furni = null;
        _roomView = null;
        _endCallback = null;
        _mode = EDIT_OFF;
    }

    /**
     * Handles mouse movement during furni editing.
     */
    protected function furniEditMoveHandler (event :MouseEvent) :void
    {
        var cloc :ClickLocation = _roomView.pointToLocation(event.stageX, event.stageY);
        if (cloc.click == ClickLocation.FLOOR) {
            _furni.setLocation(cloc.loc);            
        }
    }

    /**
     * Handles mouse clicks during furni editing.
     */
    protected function furniEditClickHandler (event :MouseEvent) :void
    {
        var cloc :ClickLocation = _roomView.pointToLocation(event.stageX, event.stageY);
        if (cloc.click == ClickLocation.FLOOR) {
            commit();
        }
    }

    /**
     * Keeps track of the different keys used to modify edit settings.
     */
    protected function keyboardHandler (event :KeyboardEvent) :void
    {
        if (event.type == KeyboardEvent.KEY_DOWN) {
            _shift = (event.keyCode == Keyboard.SHIFT);
            // change points here
        } else {
            _shift = false;
        }

        event.updateAfterEvent();
    }
    


    /** Sprite being edited. */
    protected var _furni :FurniSprite;

    /** Room view in which the editing happens. */
    protected var _roomView :RoomView;

    /** Function to be called at the end of editing. */
    protected var _endCallback :Function;

    /** Current editing mode, as one of the EDIT_* constant values. */
    protected var _mode :int = EDIT_OFF;

    /** Is the mode modifier applied (e.g. by pressing Shift key)? */
    protected var _shift :Boolean;

    
}
}
    
