package {

import flash.display.Sprite;
import flash.display.Shape;

import flash.events.KeyboardEvent;

import flash.ui.Keyboard;

import com.threerings.ezgame.EZGameControl;

[SWF(width="400", height="400")]
public class UnderworldDrift extends Sprite
{
    /** width of the masked display */
    public static const DISPLAY_WIDTH :int = 711;

    /** height of the masked display */
    public static const DISPLAY_HEIGHT :int = 400;

    public function UnderworldDrift ()
    {
        var masker :Shape = new Shape();
        masker.graphics.beginFill(0xFFFFFF);
        masker.graphics.drawRect(0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        masker.graphics.endFill();
        this.mask = masker;
        addChild(masker);

        // "sky"
        var colorBackground :Shape = new Shape();
        colorBackground.graphics.beginFill(0x8888FF);
        colorBackground.graphics.drawRect(0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT / 2);
        colorBackground.graphics.endFill();
        addChild(colorBackground);

        _ground = new Ground();
        _ground.y = DISPLAY_HEIGHT / 2;
        addChild(_ground);

        _gameCtrl = new EZGameControl(this);
        if (_gameCtrl.getMyIndex() != -1) {
            _gameCtrl.addEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        }
    }

    /** 
     * Handles KEY_DOWN. 
     */
    protected function keyEvent(event :KeyboardEvent) :void
    {
        var action :int = getActionForKey(event.keyCode);
        switch (action) {
        case Action.NONE: break;

        case Action.FORWARD: 
            _ground.moveForward();
            break;

        case Action.BACKWARD:
            _ground.moveBackward();
            break;

        case Action.LEFT:
            _ground.turnLeft();
            break;

        case Action.RIGHT:
            _ground.turnRight();
            break;

        default:
            // do nothing
        }
    }

    /**
     * Get the action that corresponds to the specified key.
     */
    protected function getActionForKey (keyCode :int) :int
    {
        switch (keyCode) {
        case Keyboard.UP: return Action.FORWARD;
        case Keyboard.DOWN: return Action.BACKWARD;
        case Keyboard.LEFT: return Action.LEFT;
        case Keyboard.RIGHT: return Action.RIGHT;
        default: return Action.NONE;
        }
    }

    /** the game control. */
    protected var _gameCtrl :EZGameControl;

    /** The ground. */
    protected var _ground :Ground;
}
}
