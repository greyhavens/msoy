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
            _gameCtrl.addEventListener(KeyboardEvent.KEY_DOWN, keyDownEvent);
            _gameCtrl.addEventListener(KeyboardEvent.KEY_UP, keyUpEvent);
        }
    }

    /** 
     * Handles KEY_DOWN. 
     */
    protected function keyDownEvent(event :KeyboardEvent) :void
    {
        switch (event.keyCode) {
        case Keyboard.UP:
            _ground.moveForward(true);
            break;

        case Keyboard.DOWN:
            _ground.moveBackward(true);
            break;

        case Keyboard.LEFT:
            _ground.turnLeft(true);
            break;

        case Keyboard.RIGHT:
            _ground.turnRight(true);
            break;

        default:
            // do nothing
        }
    }

    protected function keyUpEvent(event :KeyboardEvent) :void
    {
        switch (event.keyCode) {
        case Keyboard.UP:
            _ground.moveForward(false);
            break;

        case Keyboard.DOWN:
            _ground.moveBackward(false);
            break;

        case Keyboard.LEFT:
            _ground.turnLeft(false);
            break;

        case Keyboard.RIGHT:
            _ground.turnRight(false);
            break;

        default:
            // do nothing
        }
    }


    /** the game control. */
    protected var _gameCtrl :EZGameControl;

    /** The ground. */
    protected var _ground :Ground;
}
}
