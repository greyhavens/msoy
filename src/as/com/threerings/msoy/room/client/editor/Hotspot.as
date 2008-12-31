//
// $Id$

package com.threerings.msoy.room.client.editor {

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;
import flash.events.Event;
import flash.events.MouseEvent;
import flash.events.TimerEvent;
import flash.geom.Point;
import flash.utils.Timer;

import mx.core.IToolTip;
import mx.managers.ToolTipManager;

import com.threerings.util.Log;

/**
 * A single hotspot that processes mouse clicks on its surface, mouse movement and release.
 */
public class Hotspot extends Sprite
{
    /**
     * Constructor.
     *
     * @param editor Reference to the editor
     * @param advancedOnly If true, this hotspot will only be visible in advanced mode.
     */
    public function Hotspot (editor :FurniEditor, advancedOnly :Boolean)
    {
        _editor = editor;
        _displayInAdvancedOnly = advancedOnly;

        _tipTimer = new Timer(ToolTipManager.showDelay, 1);
        _tipTimer.addEventListener(TimerEvent.TIMER, handleTipTimer);

        addEventListener(Event.REMOVED_FROM_STAGE, closeToolTip);
    }

    /** Called when the editing UI is created. */
    public function init () :void
    {
        // register for mouse clicks on this hotspot
        addEventListener(MouseEvent.MOUSE_DOWN, startAction);
        addEventListener(MouseEvent.CLICK, clickSink);

        // register for mouse over and out, just for bitmap switching
        addEventListener(MouseEvent.ROLL_OVER, rollOver);
        addEventListener(MouseEvent.ROLL_OUT, rollOut);

        initializeDisplay();
    }

    /** Called before the editing UI is removed. */
    public function deinit () :void
    {
        removeEventListener(MouseEvent.MOUSE_DOWN, startAction);
        removeEventListener(MouseEvent.CLICK, clickSink);
        removeEventListener(MouseEvent.ROLL_OVER, rollOver);
        removeEventListener(MouseEvent.ROLL_OUT, rollOut);
        _currentDisplay = null;
        closeToolTip();
    }

    /** Returns true if the hotspot is currently being dragged around to perform some action. */
    public function isActive () :Boolean
    {
        return _anchor != null;
    }

    /**
     * Called when this hotspot should implicitly start its action because the user clicked and
     * dragged a piece of furni as they selected it.
     */
    public function implicitStartAction (event :MouseEvent) :void
    {
        startAction(event);
    }

    /**
     * This function is called every time the target's location or size change, and should be
     * used to adjust this hotspot's location relative to the target edges.
     * Subclasses should override it to provide their own functionality, in addition to
     * calling this (superclass) version.
     */
    public function updateDisplay (targetWidth :Number, targetHeight :Number) :void
    {
        // lazy initialization of first display bitmap. this needs to be done here, after
        // the editor's target variable has acquired a reference.
        if (_currentDisplay == null) {
            switchDisplay(_displayStandard);
        }
    }

    /**
     * Called whenever the target is selected or deselected, will display or hide
     * the hotspot, as appropriate for its advanced mode setting.
     */
    public function updateVisible (visible :Boolean) :void
    {
        this.visible = visible && (_displayInAdvancedOnly ? _advancedMode : true);
    }

    /**
     * Called when the user sets or clears advanced editing options.
     */
    public function setAdvancedMode (advanced :Boolean) :void
    {
        _advancedMode = advanced;
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

        // remember click and target location
        _anchor = new Point(event.stageX, event.stageY);
        _originalHotspot = _editor.target.localToGlobal(_editor.target.getLayoutHotSpot());

        // also, register for mouse moves and ups anywhere in the scene. if the player
        // pressed the button on the hotspot, we want to know about moves and the subsequent
        // mouse up regardless of where they happen.
        _editor.roomView.addEventListener(MouseEvent.MOUSE_MOVE, updateAction);
        _editor.roomView.addEventListener(MouseEvent.MOUSE_UP, endAction);

        closeToolTip();
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
        if (_editor.isIdle()) {
            Log.getLog(this).warning("Editor was idle before current hotspot finished: " + this);
        }

        // we are done, clean up. these events, for example - we no longer need them.
        _editor.roomView.removeEventListener(MouseEvent.MOUSE_MOVE, updateAction);
        _editor.roomView.removeEventListener(MouseEvent.MOUSE_UP, endAction);

        // maybe update the bitmap
        if (_delayedRollout) {
            switchDisplay(_displayStandard);
            closeToolTip();
            _delayedRollout = false;
        }

        _anchor = null;
        _originalHotspot = null;
        _editor.setActive(null);
    }

    /** Accepts mouse click events, and prevents them from propagating into the room view. */
    protected function clickSink (event :MouseEvent) :void
    {
        // don't let the room view see this click, otherwise it will think we're trying to
        // select another object, and all sorts of fun will ensue.
        event.stopPropagation();
    }

    /** Switches bitmaps on rollover. */
    protected function rollOver (event :MouseEvent) :void
    {
        // if this spurious rollover is caused by a mouse click, ignore it.
        if (event.relatedObject == null) {
            return;
        }

        // if the user rolled over while dragging any hotspot, ignore it.
        if (! _editor.isIdle()) {
            // but, if they rolled over while dragging this hotspot, this must mean that it was
            // preceded by a faulty rollout during the same dragging motion (see rollOut()).
            // in which case, clear the flag that remembered that faulty rollout.
            if (isActive()) {
                _delayedRollout = false;
            }
            return;
        }

        switchDisplay(_displayMouseOver);

        _tipTimer.reset();
        _tipTimer.start();
    }

    /** Switches bitmaps on rollout. */
    protected function rollOut (event :MouseEvent) :void
    {
        // if this spurious rollover is caused by a mouse click, ignore it.
        if (event.relatedObject == null) {
            return;
        }

        // if the user rolled out while dragging any hotspot, ignore it.
        if (! _editor.isIdle()) {
            // but, if they rolled out while dragging *this* hotspot, don't change the bitmap
            // just yet, just remember it for after the dragging is over.
            if (isActive()) {
                _delayedRollout = true;
            }
            return;
        }

        switchDisplay(_displayStandard);
        closeToolTip();
    }

    /**
     * If the new display differs from the current one, removes current display object
     * and inserts the specified one in its place.
     */
    protected function switchDisplay (display :DisplayObject) :void
    {
        if (_currentDisplay != display) {
            if (_currentDisplay != null) {
                removeChild(_currentDisplay);
                _currentDisplay = null;
            }
            if (display != null) {
                _currentDisplay = display;
                addChild(_currentDisplay);
                _currentDisplay.x = - _currentDisplay.width / 2;
                _currentDisplay.y = - _currentDisplay.height / 2;
                _currentDisplay.scaleX = 1 / _editor.roomView.scaleX;
                _currentDisplay.scaleY = 1 / _editor.roomView.scaleY;
            }
        }
    }

    /**
     * Called during init(), this function initializes the hotspot's _display* variables.
     * This default version draws a boring white square to represent the hotspot.
     * Subclasses should override it to provide their own functionality;
     * calling this superclass function is not necessary.
     */
    protected function initializeDisplay () :void
    {
        const SIZE :int = 9;
        var bitmap :Shape;
        var g :Graphics;

        bitmap = new Shape();
        g = bitmap.graphics;
        g.clear();
        g.lineStyle(0, 0x000000, 0.5, true);
        g.beginFill(0xffffff, 1.0);
        g.drawRect(0, 0, SIZE, SIZE);
        g.endFill();
        _displayStandard = bitmap;

        bitmap = new Shape();
        g = bitmap.graphics;
        g.clear();
        g.lineStyle(0, 0x000000, 0.5, true);
        g.beginFill(0xaaaaff, 1.0);
        g.drawRect(0, 0, SIZE, SIZE);
        g.endFill();
        _displayMouseOver = bitmap;
    }

    protected function getToolTip () :String
    {
        return null;
    }

    protected function handleTipTimer (event :TimerEvent) :void
    {
        // show the tooltip
        var tip :String = getToolTip();
        if (tip != null) {
            _tooltip = ToolTipManager.createToolTip(tip, this.stage.mouseX, this.stage.mouseY);
        }
    }

    protected function closeToolTip (... ignored) :void
    {
        _tipTimer.reset();

        if (_tooltip != null) {
            ToolTipManager.destroyToolTip(_tooltip);
            _tooltip = null;
        }
    }

    /** Reference to the editor. */
    protected var _editor :FurniEditor;

    /** Are we in advanced editing mode? */
    protected var _advancedMode :Boolean;

    /** Should we only display in advanced mode? */
    protected var _displayInAdvancedOnly :Boolean;

    /**
     * Mouse position at the beginning of the action. Also used to verify whether
     * a modification action is currently taking place (in which case its value is non-null).
     */
    protected var _anchor :Point;

    /**
     * Target sprite hotspot at the beginning of the action, in stage coordinates.
     */
    protected var _originalHotspot :Point;

    /** Bitmap used for hotspot display. */
    protected var _displayStandard :DisplayObject;

    /** Bitmap used for hotspot with mouseover. */
    protected var _displayMouseOver :DisplayObject;

    /** Currently used _display* bitmap. */
    protected var _currentDisplay :DisplayObject;

    /**
     * Display helper variable: remembers whether a rollout happened during dragging, and should
     * cause a bitmap update after dragging has finished.
     */
    protected var _delayedRollout :Boolean = false;

    protected var _tipTimer :Timer;

    /** Our currently displayed tooltip, if any. */
    protected var _tooltip :IToolTip;
}
}
