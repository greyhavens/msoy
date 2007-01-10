package {

import flash.display.Sprite;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import com.threerings.ezgame.PropertyChangedEvent;

import com.threerings.msoy.export.WorldGameControl;

[SWF(width="400", height="100")]
public class Gotcha extends Sprite
{
    public function Gotcha ()
    {
        // create the game control
        _gameCtrl = new WorldGameControl(this);
        _gameCtrl.enteredRoom = enteredRoom;
        _gameCtrl.occupantEntered = occupantEntered;
        _gameCtrl.occupantMoved = occupantMoved;
        
        // add and update the label that shows who's it
        _itLabel = new TextField();
        _itLabel.autoSize = TextFieldAutoSize.LEFT;
        addChild(_itLabel);
        updateItLabel();
        
        // listen for property changes
        _gameCtrl.addEventListener(PropertyChangedEvent.TYPE, propChanged);
    }
    
    protected function enteredRoom () :void
    {
        updateItProperty();
    }
    
    protected function occupantEntered (occupant :int) :void
    {
        updateItProperty();
    }
    
    protected function occupantMoved (occupant :int) :void
    {
        updateItProperty();
    }
    
    protected function updateItProperty () :void
    {
        var myIdx :int = _gameCtrl.getMyIndex();
        if (myIdx != int(_gameCtrl.get(IT_INDEX))) {
            return; // only "it" tags others
        }
        var ownId :int = _gameCtrl.getMyOccupantId(),
            ownLoc :Array = _gameCtrl.getOccupantLocation(ownId);
        var occIds :Array = _gameCtrl.getPlayerOccupantIds();
        for (var ii :int = 0; ii < occIds.length; ii++) {
            if (ii == myIdx) {
                continue;
            }
            var occLoc :Array = _gameCtrl.getOccupantLocation(occIds[ii]);
            if (occLoc != null && getDistance(ownLoc, occLoc) <= TAG_DISTANCE) {
                _gameCtrl.set(IT_INDEX, ii); // you're it!
                return;
            }
        }
    }
    
    protected function propChanged (event :PropertyChangedEvent) :void
    {
        if (event.name == IT_INDEX) {
            updateItLabel();
        }
    }
    
    protected function updateItLabel () :void
    {
        var itIndex :int = int(_gameCtrl.get(IT_INDEX));
        _itLabel.text = _gameCtrl.getPlayerNames()[itIndex] + " is it.";
    }
    
    /**
     * Computes and returns the distance between two array coordinates.
     */
    protected function getDistance (p1 :Array, p2 :Array) :Number
    {
        var accum :Number = 0;
        for (var ii :int = 0; ii < p1.length && ii < p2.length; ii++) {
            accum += Math.pow(Number(p1[ii]) - Number(p2[ii]), 2);
        }
        return Math.sqrt(accum);
    }
    
    protected var _gameCtrl :WorldGameControl;
    
    protected var _itLabel :TextField;
    
    /** The property used to track the name of the player who's "it." */
    protected static const IT_INDEX :String = "itIndex";
    
    /** The distance at which we can tag other players. */
    protected static const TAG_DISTANCE :Number = 0.1;
}
}
