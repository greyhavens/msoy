//
// $Id$

package com.threerings.msoy.world.client.editor {

import mx.controls.Button;
import flash.events.Event;
import flash.events.MouseEvent;
import flash.geom.Point;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.world.client.ClickLocation;
import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.client.DecorSprite;
import com.threerings.msoy.world.client.MsoySprite;
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
    
    public function RoomEditController (ctx :WorldContext, panel :RoomEditPanel)
    {
        _ctx = ctx;
        _panel = panel;
    }

    public function init () :void
    {
    }

    public function deinit () :void
    {
        switchToPhase(PHASE_DONE); // just end whatever was going on, skipping commit
    }

    /** Returns current editing phase, as one of the PHASE_* constants. */
    public function get currentPhase () :int
    {
        return _currentPhase;
    }


    // Panel accessors for activity coming in from the room

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


    // Panel click handlers

    /** Handle click on the move button. */
    public function moveButtonClick (button :Button, def :Object) :void
    {
        _panel.setInfoLabel (button.selected ? def : null);
        processAction(ACTION_MOVE);
    }

    /** Handle click on the scale button. */
    public function scaleButtonClick (button :Button, def :Object) :void
    {
        _panel.setInfoLabel(button.selected ? def : null);
        processAction(ACTION_SCALE);
    }

    /** Handle click on the room edit button. */
    public function roomButtonClick (button :Button, def :Object) :void
    {
        _panel.setInfoLabel(def);
        processAction(ACTION_ROOM);
    }


    // Input handlers based on the current phase

    protected function doInit () :void
    {
        trace("*** startInit, current action: " + _currentAction);
        switchToPhase(nextPhase());
    }

    // Phase: acquire

    protected function startAcquire () :void
    {
        trace("*** startAcquire, current action: " + _currentAction);
        _panel.updateFocus(_lastAcquisition, false);
        _lastAcquisition = null;
    }

    protected function hoverAcquire (sprite :MsoySprite) :void
    {
        // treat non-furni sprites and decor sprites as if they were background
        if (! (sprite is FurniSprite) || sprite is DecorSprite) {
            sprite = null;
        }
        // and make sure it's not the same as what we hit on the last frame
        if (_lastAcquisition != sprite) {
            trace("SPRITE: " + sprite);
            _panel.updateFocus(_lastAcquisition, false);
            _lastAcquisition = sprite as FurniSprite;
            _panel.updateFocus(_lastAcquisition, true);
        }
    }

    protected function clickAcquire (sprite :MsoySprite, event :MouseEvent) :void
    {
        // if we clicked on something, transition to the next phase, otherwise keep trying
        if (_lastAcquisition != null) {
            switchToPhase(nextPhase());
        }
    }

    protected function endAcquire () :void
    {
        _panel.updateFocus(_lastAcquisition, false);

        _currentFurni = _lastAcquisition;
        _lastAcquisition = null;

        if (_currentFurni != null) {
            _originalFurniData = _currentFurni.getFurniData();
        }
    }

    // Phase: modify
    
    protected function startModify () :void
    {
        trace("*** startModify, current action: " + _currentAction);

        // if we haven't acquired a target, skip this phase, even if this action supports it
        if (_currentFurni == null) {
            switchToPhase(nextPhase());
            return;
        }

        // otherwise start modification functionality
        _panel.updateFocus(_currentFurni, true);
    }

    protected function moveModify (x :Number, y :Number) :void
    {
        switch (_currentAction) {
        case ACTION_MOVE:
            moveFurni(findNewFurniPosition(x, y), false);
            break;
        case ACTION_SCALE:
            scaleFurni(_currentFurni, x, y);
            break;
        }

        _panel.updateFocus(_currentFurni, true);
    }

    protected function clickModify (sprite :MsoySprite, event :MouseEvent) :void
    {
        switch (_currentAction) {
        case ACTION_MOVE:
            moveFurni(findNewFurniPosition(event.stageX, event.stageY), true);
            break;
        case ACTION_SCALE:
            scaleFurni(_currentFurni, event.stageX, event.stageY);
            break;
        }

        switchToPhase(nextPhase());
    }

    protected function endModify () :void
    {
        _panel.updateFocus(_currentFurni, false);
    }

    // Phase: commit
    
    protected function doCommit () :void
    {
        // add undo stack functionality here
        
        trace("*** startCommit, current action: " + _currentAction);
        switch (_currentAction) {
            // both move and scale actions commit data immediately, then force a return
            // back to the init state, so the player can move or scale more objects
        case ACTION_MOVE:
        case ACTION_SCALE:
            commitFurniData();
            switchToPhase(PHASE_INIT);
            break;
        default:
            switchToPhase(nextPhase());
        }
    }

    protected function doDone () :void
    {
        trace("*** startDone, current action: " + _currentAction);
        // no phase switches here. :)
    }


    // Movement only functions
    
    protected function moveFurni (loc :MsoyLocation, updateFurniData :Boolean = false) :void
    {
        _currentFurni.setLocation(loc);
        if (updateFurniData) {
            _currentFurni.getFurniData().loc = loc;
        }
    }

    protected function findNewFurniPosition (x :Number, y :Number) :MsoyLocation
    {
        var cloc :ClickLocation = _panel.roomView.layout.pointToFurniLocation(
            x, y, null, null);
            //_modAnchor,
            //(getModifier() == MOD_SHIFT ? RoomMetrics.N_UP : RoomMetrics.N_AWAY));
        
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


    // Phase and action helpers

    /** Sends an update to the server, updating furni information. */
    protected function commitFurniData () :void
    {
        if (_originalFurniData != null && _currentFurni != null) {
            _panel.roomView.getRoomController().sendFurniUpdate(
                [ _originalFurniData ], [ _currentFurni.getFurniData() ]);
        }
    }

    /** Returns true if the given phase supports the given action. */
    protected function phaseSupports (phase :int, action :String) :Boolean {
        trace("*** testing if phase " + PHASENAMES[phase] + " supports " + action);
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
        trace("*** processAction: " + action + " found new phase: " + PHASENAMES[phase]);
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
    protected const INITS   :Array = [ doInit, startAcquire,  startModify,  doCommit, doDone ];
    protected const DEINITS :Array = [ none,   endAcquire,    endModify,    none,     none   ];

    protected var _ctx :WorldContext;
    protected var _panel :RoomEditPanel;

    protected var _lastAcquisition :FurniSprite;
    
    protected var _currentFurni :FurniSprite;
    protected var _originalFurniData :FurniData;

    protected var _currentAction :String;
    protected var _currentPhase :int = PHASE_DONE;
}
}
