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

        var camera :Camera = new Camera();

        _ground = new Ground(camera);
        _ground.y = DISPLAY_HEIGHT / 4;
        addChild(_ground);
        camera.setGround(_ground);

        // create ground and position with some magic numbers
        _kart = new Kart(camera);
        _kart.x = 355;
        _kart.y = 300;
        addChild(_kart);

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
            _kart.moveForward(true);
            break;

        case Keyboard.DOWN:
            _kart.moveBackward(true);
            break;

        case Keyboard.LEFT:
            _kart.turnLeft(true);
            break;

        case Keyboard.RIGHT:
            _kart.turnRight(true);
            break;

        case Keyboard.SPACE:
            _kart.jump();
            break;

        default:
            // do nothing
        }
    }

    protected function keyUpEvent(event :KeyboardEvent) :void
    {
        switch (event.keyCode) {
        case Keyboard.UP:
            _kart.moveForward(false);
            break;

        case Keyboard.DOWN:
            _kart.moveBackward(false);
            break;

        case Keyboard.LEFT:
            _kart.turnLeft(false);
            break;

        case Keyboard.RIGHT:
            _kart.turnRight(false);
            break;

        default:
            // do nothing
        }
    }


    /** the game control. */
    protected var _gameCtrl :EZGameControl;

    /** The ground. */
    protected var _ground :Ground;

    /** The kart. */
    protected var _kart :Kart;
}
}
