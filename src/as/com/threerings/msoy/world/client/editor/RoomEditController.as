//
// $Id$

package com.threerings.msoy.world.client.editor {

import mx.controls.Button;
import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.ui.Keyboard;

import com.threerings.flash.Vector3;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.world.client.ClickLocation;
import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.client.DecorSprite;
import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.world.client.RoomMetrics;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyLocation;

/**
 * Controller for the room editing panel.
 */
public class RoomEditController
{
    // The controller processes mouse movement in the room, based on the current editing action
    // ("action"), and the particular stage this action has reached so far ("phase"). Not all
    // actions have all of the phases; movement includes all of them, but deleting only some.
    // Any mouse input needs to be processed based on the < action, phase, input type > triple,
    // which happens in two stages: first, an appropriate handler is found based on the 
    // < phase, input type > combination (see tables: MOVES, CLICKS, HOVERS), and then
    // this handler can do something special based on action type.


    /**
     * ACTION_* constants describe current action being performed.
     */
    public static const ACTION_ROOM :String = "Edit Room";
    public static const ACTION_MOVE :String = "Edit Move";
    public static const ACTION_SCALE :String = "Edit Scale";
    public static const ACTION_DELETE :String = "Edit Delete";
    public static const ACTION_UNDO :String = "Edit Undo";
    
    /**
     * PHASE_* constants describe the current editing phase. Each action can go through
     * the following phases:
     *   init    - resets targeting (all actions)
     *   acquire - target is acquired for editing mode (move, scale, delete)
     *   modify  - target is modified based on movement (move, scale)
     *   commit  - editing changes are committed (move, scale, delete)
     *   done    - editing is finalized (all actions)
     * If a new action is selected before the /done/ phase, the current phase is rolled back
     * to the latest phase valid for the new type of action. For example, if we're in the
     * /modify/ phase of a move action, switching to a delete action will revert us to the
     * /acquire/ phase, while switching to scale action won't modify the phase at all.
     */
    public static const PHASE_INIT    :int = 0;
    public static const PHASE_ACQUIRE :int = 1;
    public static const PHASE_MODIFY  :int = 2;
    public static const PHASE_COMMIT  :int = 3;
    public static const PHASE_DONE    :int = 4;

    public static const PHASEACTIONS :Array =
        [ /* init */    [ ACTION_MOVE, ACTION_SCALE, ACTION_DELETE, ACTION_UNDO, ACTION_ROOM ],
          /* acquire */ [ ACTION_MOVE, ACTION_SCALE, ACTION_DELETE ],
          /* modify */  [ ACTION_MOVE, ACTION_SCALE ],
          /* commit */  [ ACTION_MOVE, ACTION_SCALE, ACTION_DELETE ],
          /* done */    [ ACTION_MOVE, ACTION_SCALE, ACTION_DELETE, ACTION_UNDO, ACTION_ROOM ] ];

    // for debugging only
    protected static const PHASENAMES :Array = [ "init", "acquire", "modify", "commit", "done" ];


    /** Editing mode preferences. */
    public var moveYAxisOnly :Boolean;
    public var moveZAxisOnly :Boolean;
    
    public function RoomEditController (ctx :WorldContext, panel :RoomEditPanel)
    {
        _ctx = ctx;
        _panel = panel;
    }

    public function init () :void
    {
        _panel.roomView.stage.addEventListener(KeyboardEvent.KEY_DOWN, handleKeyboard);
        _panel.roomView.stage.addEventListener(KeyboardEvent.KEY_UP, handleKeyboard);
        _panel.roomView.setEditingOverlay(true);
    }

    public function deinit () :void
    {
        _panel.roomView.setEditingOverlay(false);
        _panel.roomView.stage.removeEventListener(KeyboardEvent.KEY_DOWN, handleKeyboard);
        _panel.roomView.stage.removeEventListener(KeyboardEvent.KEY_UP, handleKeyboard);

        switchToPhase(PHASE_DONE); // just end whatever was going on, skipping commit
    }

    /** Returns current editing phase, as one of the PHASE_* constants. */
    public function get currentPhase () :int
    {
        return _currentPhase;
    }


    // Panel accessors

    /** Receives mouse updates from the panel, with x, y values in scene coordinates. */
    public function mouseMove (x :Number, y :Number) :void
    {
        (MOVES[_currentPhase] as Function)(x, y);
    }

    /** Receives mouse updates from the panel, if a sprite is currently being selected. */
    public function mouseOverSprite (sprite :MsoySprite) :void
    {
        (HOVERS[_currentPhase] as Function)(sprite);
    }        

    /** Processes mouse clicks during target acquisition. */
    public function mouseClick (sprite :MsoySprite, event :MouseEvent) :void
    {
        (CLICKS[_currentPhase] as Function)(sprite, event);
    }

    /** Handle click on one of the action buttons. */
    public function handleActionSelection (action :String, button :Button, def :Object) :void
    {
        // update info bar when momentary buttons are pressed, or when toggle buttons are on
        var displayInfo :Boolean = (! button.toggle) || button.selected; 
        _panel.setInfoLabel (displayInfo ? def : null);
        
        processAction(action);
    }

    /**
     * Keeps track of the different keys used to modify edit settings.
     */
    protected function handleKeyboard (event :KeyboardEvent) :void
    {
        // this is very ad hoc right now. do we have any big plans for keyboard shortcuts?
        if (_currentAction == ACTION_MOVE && _currentPhase == PHASE_MODIFY) {
            if (event.type == KeyboardEvent.KEY_DOWN) {
                moveYAxisOnly = (event.keyCode == Keyboard.SHIFT);
                moveZAxisOnly = (event.keyCode == Keyboard.CONTROL);
            }
            if (event.type == KeyboardEvent.KEY_UP) {
                moveYAxisOnly = moveYAxisOnly && !(event.keyCode == Keyboard.SHIFT);
                moveZAxisOnly = moveZAxisOnly && !(event.keyCode == Keyboard.CONTROL);
            }
        }
    }

    // Phase: init

    protected function doInit () :void
    {
        switchToPhase(nextPhase());
    }

    // Phase: acquire

    protected function startAcquire () :void
    {
        if (_currentTarget != null && _originalTargetData != null) {
            // somehow we ended up here after a target was already selected! in this case,
            // restore the target to what it used to be, because we'll acquire a fresh one
            _currentTarget.update(_originalTargetData);
            _currentTarget = null;
            _originalTargetData = null;
        }

        if (_acquireCandidate != null) {
            _panel.clearFocus(_acquireCandidate);
            _acquireCandidate = null;
        }
    }

    protected function hoverAcquire (sprite :MsoySprite) :void
    {
        // treat non-furni sprites and decor sprites as if they were background
        if (! (sprite is FurniSprite) || sprite is DecorSprite) {
            sprite = null;
        }
        // and make sure it's not the same as what we hit on the last frame
        if (_acquireCandidate != sprite) {
            _panel.clearFocus(_acquireCandidate);
            _acquireCandidate = sprite as FurniSprite;
            _panel.updateFocus(_acquireCandidate, _currentAction);
        }
    }

    protected function clickAcquire (sprite :MsoySprite, event :MouseEvent) :void
    {
        // if we clicked on something, transition to the next phase, otherwise keep trying
        if (_acquireCandidate != null) {
            switchToPhase(nextPhase());
        }
    }

    protected function endAcquire () :void
    {
        _panel.clearFocus(_acquireCandidate);

        _currentTarget = _acquireCandidate;
        _acquireCandidate = null;

        if (_currentTarget != null) {
            _originalTargetData = _currentTarget.getFurniData();
        }
    }

    // Phase: modify
    
    protected function startModify () :void
    {
        // if we haven't acquired a target, skip this phase, even if this action supports it
        if (_currentTarget == null) {
            switchToPhase(nextPhase());
            return;
        }

        // otherwise start modification functionality
        _panel.updateFocus(_currentTarget, _currentAction);
    }

    protected function moveModify (x :Number, y :Number) :void
    {
        switch (_currentAction) {
        case ACTION_MOVE:
            moveFurni(_currentTarget, findNewFurniPosition(x, y), false);
            break;
        case ACTION_SCALE:
            scaleFurni(_currentTarget, x, y);
            break;
        }

        _panel.updateFocus(_currentTarget, _currentAction);
    }

    protected function clickModify (sprite :MsoySprite, event :MouseEvent) :void
    {
        switch (_currentAction) {
        case ACTION_MOVE:
            moveFurni(_currentTarget, findNewFurniPosition(event.stageX, event.stageY), true);
            break;
        case ACTION_SCALE:
            scaleFurni(_currentTarget, event.stageX, event.stageY);
            break;
        }

        switchToPhase(nextPhase());
    }

    protected function endModify () :void
    {
        _panel.clearFocus(_currentTarget);
    }

    // Phase: commit
    
    protected function startCommit () :void
    {
        // add undo stack functionality here
        
        switch (_currentAction) {
            // both the move and scale actions commit data immediately, then force a return
            // back to the init state, so the player can move or scale more objects
        case ACTION_MOVE:
        case ACTION_SCALE:
            if (_currentTarget != null && _originalTargetData != null) {
                commitFurniData(_originalTargetData, _currentTarget.getFurniData());
            }
            switchToPhase(PHASE_INIT);
            return;
        case ACTION_DELETE:
            // delete the old object
            if (_originalTargetData != null) {
                commitFurniData(_originalTargetData, null);
            }
            break;
        }

        // note: MOVE and SCALE never reach this line
        switchToPhase(nextPhase());
    }

    protected function endCommit () :void
    {
        // clean up state variables
        _currentTarget = null;
        _originalTargetData = null;
    }

    protected function doDone () :void
    {
        // no phase switches here. :)
    }


    // Movement only functions
    
    protected function moveFurni (
        target :FurniSprite, loc :MsoyLocation, updateFurniData :Boolean) :void
    {
        target.setLocation(loc);
        if (updateFurniData) {
            target.getFurniData().loc = loc;
        }
    }

    protected function findNewFurniPosition (x :Number, y :Number) :MsoyLocation
    {
        var anchor :MsoyLocation = ((moveYAxisOnly || moveZAxisOnly) && _currentTarget != null) ?
            _currentTarget.getLocation() : null;
        
        var direction :Vector3 = null;
        if (moveYAxisOnly) {
            direction = RoomMetrics.N_UP;
        }
        if (moveZAxisOnly) {
            direction = RoomMetrics.N_AWAY;
        }
            
        var cloc :ClickLocation = _panel.roomView.layout.pointToFurniLocation(
            x, y, anchor, direction);
        
        return cloc.loc;
    }

    // Scaling only functions

    protected function scaleFurni (furni :FurniSprite, x :Number, y :Number) :void
    {
        var scale :Point = findScale(furni, x, y);
        furni.setMediaScaleX(scale.x);
        furni.setMediaScaleY(scale.y);
    }

    /** Given some width, height values in screen coordinates, finds x and y scaling factors
     *  that would resize the current furni to those coordinates. */
    protected function computeScale (furni :FurniSprite, width :Number, height :Number) :Point
    {
        const e :Number = 0.1; // zero scale will get bumped up to this value
        
        // get current size info in pixels
        var oldwidth :Number = Math.max(furni.getActualWidth(), 1);
        var oldheight :Number = Math.max(furni.getActualHeight(), 1);

        // figure out the proportion of pixels per scaling unit that produced old width and height
        var xProportions :Number = Math.max(Math.abs(oldwidth / furni.getMediaScaleX()), 1);
        var yProportions :Number = Math.max(Math.abs(oldheight / furni.getMediaScaleY()), 1);

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
    protected function findScale (furni: FurniSprite, x :Number, y: Number) :Point
    {
        // find hotspot position in terms of sprite width and height
        var hotspot :Point = furni.getLayoutHotSpot();
        var px :Number = hotspot.x / furni.getActualWidth();  
        var py :Number = hotspot.y / furni.getActualHeight(); 

        // find pixel distance from hotspot to mouse pointer
        var pivot :Point = furni.localToGlobal(hotspot);      
        var dx :Number = x - pivot.x; // positive to the right of hotspot
        var dy :Number = pivot.y - y; // positive above hotspot

        // convert pixel position to how wide and tall the furni would have to be in order
        // to reach that position
        var newwidth :Number = dx / px;
        var newheight :Number = dy / py;

        // if we're scaling proportionally, lock the two distances
        /*
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
        */

        // scale the furni!
        return computeScale(furni, newwidth, newheight); 
    }

    // Helpers

    /** Sends an update to the server. /toRemove/ will be removed, and /toAdd/ added. */
    protected function commitFurniData (toRemove :FurniData, toAdd :FurniData) :void
    {
        var adds :Array = (toAdd != null) ? [ toAdd ] : null;
        var deletes :Array = (toRemove != null) ? [ toRemove ] : null;
        _panel.roomView.getRoomController().sendFurniUpdate(deletes, adds);
    }

    // Phase and action helpers

    /** Returns true if the given phase supports the given action. */
    protected function phaseSupports (phase :int, action :String) :Boolean {
        var actions :Array = PHASEACTIONS[phase];
        return (actions.indexOf(action) != -1);
    }
    
    /**
     * Given some phase, advances through the phase list forward or backward, searching for
     * the next phase supported by the specified action. Search direction is specified by
     * the /reverse/ flag (forward if false, backward if true).
     */
    protected function revertPhase (action :String) :int
    {
        // if we're done, just restart
        if (_currentPhase == PHASE_DONE) {
            return PHASE_INIT;
        }

        // okay, let's search manually, starting from *current* phase 
        var last :int = _currentPhase; 
        while (last > PHASE_INIT) {
            if (phaseSupports(last, action)) {
                return last;
            }
            last--;
        }
        return PHASE_INIT;
    }

    /**
     * Given current phase, finds the next phase for the current action.
     */
    protected function nextPhase () :int
    {
        var next :int = _currentPhase + 1; // start from next phase, and search forward
        while (next < PHASE_DONE) {
            if (phaseSupports(next, _currentAction)) {
                return next;
            }
            next++;
        }
        return PHASE_DONE;
    }

    
    /**
     * Start a completely new action. Finds the latest phase valid for this action,
     * and switches there.
     */        
    protected function processAction (action :String) :void
    {
        // get the latest phase supported by the current action
        var phase :int = revertPhase(action);
        _currentAction = action;
        switchToPhase(phase);
    }

    /** Switch to the new phase. */
    protected function switchToPhase (phase :int) :void
    {

        trace("*** switchToPhase from: " + PHASENAMES[_currentPhase] + 
             " to: " + PHASENAMES[phase]);
        if (phase != _currentPhase) {
            (DEINITS[_currentPhase] as Function)();
            _currentPhase = phase;
            (INITS[_currentPhase] as Function)();
        }
    }

    
    protected const none :Function = function (... args) :void { };

    /** Mouse input handlers for all actions, indexed by PHASE_* value. */
    protected const MOVES   :Array = [ none,   none,          moveModify,   none,     none   ];
    protected const HOVERS  :Array = [ none,   hoverAcquire,  none,         none,     none   ];
    protected const CLICKS  :Array = [ none,   clickAcquire,  clickModify,  none,     none   ];

    /** Phase initialization and shutdown helpers, indexed by PHASE_* value. */
    protected const INITS   :Array = [ doInit, startAcquire,  startModify,  startCommit, doDone ];
    protected const DEINITS :Array = [ none,   endAcquire,    endModify,    endCommit,   none   ];

    protected var _ctx :WorldContext;
    protected var _panel :RoomEditPanel;

    /** During acquisition phase, points to the different sprites as the mouse hovers over them. */
    protected var _acquireCandidate :FurniSprite;

    /** Result of the acquisition phase, points to the acquired sprite. */
    protected var _currentTarget :FurniSprite;
    
    /** Result of the acquisition phase, contains the acquired sprite's original data. */
    protected var _originalTargetData :FurniData;

    protected var _currentAction :String;
    protected var _currentPhase :int = PHASE_DONE;
}
}
