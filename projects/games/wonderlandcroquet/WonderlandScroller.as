package {

import flash.display.Sprite;

public class WonderlandScroller extends Sprite
{
    public function WonderlandScroller (spr :Sprite) {
        _spr = spr;
        
        // Add triangles pointing each way

        // Up
        spr = new Triangle(_spr, 0, -STEP_SIZE);
        addChild(spr);
        spr.x = 0;
        spr.y = spr.height / 2;

        // Down
        spr = new Triangle(_spr, 0, STEP_SIZE);
        addChild(spr);
        spr.x = 0;
        spr.y = - spr.height / 2;
        spr.rotation = 180;

        // Left
        spr = new Triangle(_spr, STEP_SIZE, 0);
        addChild(spr);
        spr.x = - spr.height / 2;
        spr.y = 0;
        spr.rotation = 90;

        // Right
        spr = new Triangle(_spr, -STEP_SIZE, 0);
        addChild(spr);
        spr.x = spr.height / 2;
        spr.y = 0;
        spr.rotation = -90;
    }

    // The sprite we scroll around
    protected var _spr :Sprite;

    // How far to move on each click on an arrow
    protected static const STEP_SIZE :int = 50;
}
}

import flash.display.Sprite;
import flash.events.MouseEvent;
import flash.events.Event;

class Triangle extends Sprite
{
    public function Triangle (spr :Sprite, dx :int, dy: int) {
        _spr = spr;
        _dx = dx;
        _dy = dy;
        graphics.beginFill(0xff0000);
        graphics.moveTo(0, 15);
        graphics.lineTo(15, 0);
        graphics.lineTo(-15, 0);
        graphics.lineTo(0, 15);
        graphics.endFill();
        addEventListener(MouseEvent.CLICK, scrollSprite);
    }

    protected function scrollSprite (event :MouseEvent) :void
    {
        _spr.x += _dx;
        _spr.y += _dy;

        if (_spr.x > 0) _spr.x = 0;
        if (_spr.y > 0) _spr.y = 0;
        // TODO: Lock the other direction onto our screen
    }

    protected var _dx :int;
    protected var _dy :int;
    protected var _spr :Sprite;
}


