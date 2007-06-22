//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.Graphics;
import flash.display.Sprite;
import flash.events.MouseEvent;
import flash.geom.Point;

/**
 * A single hotspot that processes mouse clicks on its surface, mouse movement and release.
 */
public class Hotspot extends Sprite
{
    public function Hotspot (editor :FurniEditor)
    {
        _editor = editor;
    }

    /** Called when the editing UI is created. */
    public function init () :void
    {
        // register for mouse clicks on this hotspot
        addEventListener(MouseEvent.MOUSE_DOWN, startAction);
        addEventListener(MouseEvent.CLICK, clickSink);

        initializeDisplay();
    }

    /** Called before the editing UI is removed. */
    public function deinit () :void
    {
        removeEventListener(MouseEvent.MOUSE_DOWN, startAction);
        removeEventListener(MouseEvent.CLICK, clickSink);
    }

    /** Returns true if the hotspot is currently being dragged around to perform some action. */
    public function isActive () :Boolean
    {
        return _anchor != null;
    }
    
    /**
     * This function is called when the user presses a mouse button on this hotspot.
     * Subclasses should override it to provide their own functionality,
     * but make sure to call this (superclass) handler as well.
     */
    protected function startAction (event :MouseEvent) :void
    {
        // user clicked on the hotspot. let the games begin!
        _editor.setActive(this);

        // remember click location
        _anchor = new Point(event.stageX, event.stageY);

        // also, register for mouse moves and ups anywhere in the scene. if the player
        // pressed the button on the hotspot, we want to know about moves and the subsequent
        // mouse up regardless of where they happen.
        _editor.roomView.addEventListener(MouseEvent.MOUSE_MOVE, updateAction);
        _editor.roomView.addEventListener(MouseEvent.MOUSE_UP, endAction);
    }
        
    /**
     * This function is called when the user moves the mouse while holding down the mouse button.
     * Subclasses should override it to provide their own functionality,
     * but make sure to call this (superclass) handler as well.
     */
    protected function updateAction (event :MouseEvent) :void
    {
        // no op.
        // subclasses, do something here! or don't. whatever. do what you want, you will anyway.
    }

    /**
     * This function is called when the user releases a button that was pressed on this hotspot.
     * Subclasses should override it to provide their own functionality,
     * but make sure to call this (superclass) handler as well.
     */
    protected function endAction (event :MouseEvent) :void
    {
        // we are done, clean up. these events, for example - we no longer need them.
        _editor.roomView.removeEventListener(MouseEvent.MOUSE_MOVE, updateAction);
        _editor.roomView.removeEventListener(MouseEvent.MOUSE_UP, endAction);

        if (_editor.isIdle()) {
            Log.getLog(this).warning("Editor was idle before current hotspot finished: " + this);
        }

        _anchor = null;
        _editor.setActive(null);
    }

    /** Accepts mouse click events, and prevents them from propagating into the room view. */
    protected function clickSink (event :MouseEvent) :void
    {
        // don't let the room view see this click, otherwise it will think we're trying to
        // select another object, and all sorts of fun will ensue.
        event.stopPropagation(); 
    }

    /**
     * Default display function, draws a boring white square to represent the hotspot.
     * Subclasses should override it to provide their own functionality;
     * calling this superclass function is not necessary.
     */
    protected function initializeDisplay () :void
    {
        const SIZE :int = 9;
        var g :Graphics = this.graphics;
        g.clear();
        g.lineStyle(0, 0x000000, 0.5, true);
        g.beginFill(0xffffff, 1.0);
        g.drawRect(-SIZE/2, -SIZE/2, SIZE, SIZE);
        g.endFill();
    }


    /** Reference to the editor. */
    protected var _editor :FurniEditor;
    
    /**
     * Mouse position at the beginning of the action. Also used to verify whether
     * a modification action is currently taking place (in which case its value is non-null).
     */
    protected var _anchor :Point;

    

}
}
