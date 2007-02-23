package minigames.keyjam {

import flash.display.Sprite;

import flash.ui.Keyboard;

public class KeySprite extends Sprite
{
    public static const WIDTH :int = 40;
    public static const HEIGHT :int = 40;

    public static const PAD :int = 9;

    public function KeySprite (key :int)
    {
        _key = key;

        _arrow = new ArrowSprite(WIDTH - PAD, HEIGHT - PAD);
        _arrow.x = WIDTH/2;
        _arrow.y = HEIGHT/2;
        addChild(_arrow);

        updateVis();
    }

    public function getKey () :int
    {
        return _key;
    }

    public function setHit (hit :Boolean) :void
    {
        _hit = hit;
        updateVis();
    }

    protected function updateVis () :void
    {
        graphics.clear();
        graphics.beginFill(_hit ? 0xFFFFFF : 0xFF9999);
        graphics.drawRect(0, 0, WIDTH, HEIGHT);
        graphics.endFill();
        graphics.lineStyle(1.5, 0);
        graphics.drawRect(0, 0, WIDTH, HEIGHT);

//        graphics.lineStyle(4, _hit ? 0x333399 : 0xFF0000);
        switch (_key) {
        case Keyboard.UP:
            _arrow.rotation = 0;
            break;

        case Keyboard.RIGHT:
            _arrow.rotation = 90;
            break;

        case Keyboard.DOWN:
            _arrow.rotation = 180;
            break;

        case Keyboard.LEFT:
            _arrow.rotation = 270;
            break;
        }
    }

    /** The key we're representing. */
    protected var _key :int;

    protected var _arrow :ArrowSprite;

    protected var _hit :Boolean = false;
}
}

import flash.display.Sprite;

internal class ArrowSprite extends Sprite
{
    public function ArrowSprite (w :int, h :int)
    {
        graphics.clear();
        graphics.lineStyle(4);

        // note: we draw such that 0,0 is the center of the arrow
        // for easy rotation
        graphics.moveTo(0, h/2);
        graphics.lineTo(0, -h/2);

        graphics.moveTo(-w/2, -h/6);
        graphics.lineTo(0, -h/2);
        graphics.lineTo(w/2, -h/6);
    }
}
