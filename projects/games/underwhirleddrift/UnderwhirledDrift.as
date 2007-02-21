package {

import flash.display.Sprite;
import flash.display.Shape;

import flash.geom.Point;

import flash.events.KeyboardEvent;

import flash.ui.Keyboard;

import com.threerings.ezgame.EZGameControl;

[SWF(width="711", height="400")]
public class UnderwhirledDrift extends Sprite
{
    /** width of the masked display */
    public static const DISPLAY_WIDTH :int = 711;

    /** height of the masked display */
    public static const DISPLAY_HEIGHT :int = 400;

    /** height of the sky */
    public static const SKY_HEIGHT :int = DISPLAY_HEIGHT * 0.4;

    /** Kart location, relative to the ground coordinates */
    public static const KART_LOCATION :Point = new Point (355, 200);

    public function UnderwhirledDrift ()
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
        colorBackground.graphics.drawRect(0, 0, DISPLAY_WIDTH, SKY_HEIGHT + 5);
        colorBackground.graphics.endFill();
        addChild(colorBackground);

        var camera :Camera = new Camera();

        _ground = new Ground(camera);
        _ground.y = SKY_HEIGHT;
        addChild(_ground);

        _kart = new Kart(camera, _ground);
        _kart.x = KART_LOCATION.x;
        _kart.y = KART_LOCATION.y + SKY_HEIGHT;
        addChild(_kart);

        _gameCtrl = new EZGameControl(this);
        _gameCtrl.addEventListener(KeyboardEvent.KEY_DOWN, keyDownEvent);
        _gameCtrl.addEventListener(KeyboardEvent.KEY_UP, keyUpEvent);
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

    /** The driving surface. */
    protected var _ground :Ground;

    /** The kart. */
    protected var _kart :Kart;
}
}
