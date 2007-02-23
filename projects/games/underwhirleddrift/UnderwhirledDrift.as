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

    /** Kart offset from its effective location */
    public static const KART_OFFSET :int = 32;

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
        colorBackground.graphics.drawRect(0, 0, DISPLAY_WIDTH, SKY_HEIGHT + 10);
        colorBackground.graphics.endFill();
        addChild(colorBackground);

        var camera :Camera = new Camera();

        var ground :Ground = new Ground(camera);
        ground.y = SKY_HEIGHT;
        addChild(ground);

        _kart = new Kart(camera, ground);
        _kart.x = KART_LOCATION.x;
        // tack on a few pixels to account for the front of the kart
        _kart.y = KART_LOCATION.y + SKY_HEIGHT + KART_OFFSET;
        addChild(_kart);

        _gameCtrl = new EZGameControl(this);
        _gameCtrl.addEventListener(KeyboardEvent.KEY_DOWN, keyDownEvent);
        _gameCtrl.addEventListener(KeyboardEvent.KEY_UP, keyUpEvent);
        _gameCtrl.localChat("My display name is " + _gameCtrl.getOccupantName(_gameCtrl.getMyId()) +
            "!");
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

    /** The kart. */
    protected var _kart :Kart;
}
}
