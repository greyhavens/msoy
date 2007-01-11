package {

import flash.display.Sprite;
import flash.display.DisplayObject;
import flash.display.InteractiveObject;
import flash.events.MouseEvent;

public class WonderlandScroller extends Sprite
{
    public function WonderlandScroller (parent :DisplayObject) {
        _parent = parent;

        // The card back
        var iobj :InteractiveObject = new CardBack();
        /* This, for some reason doesn't seem to work, so I'm just commenting it out for now
        iobj.addEventListener(MouseEvent.MOUSE_WHEEL, mouseWheel);
        */
        iobj.addEventListener(MouseEvent.MOUSE_DOWN, mouseDown);
        iobj.addEventListener(MouseEvent.MOUSE_UP, mouseUp);
        addChild(iobj);

        // Pan
        addChild(new PanControl(new PanUp(),    parent, 0, STEP_SIZE));
        addChild(new PanControl(new PanDown(),  parent, 0, -STEP_SIZE));
        addChild(new PanControl(new PanLeft(),  parent,  STEP_SIZE, 0));
        addChild(new PanControl(new PanRight(), parent, -STEP_SIZE, 0));

        // Zoom
        addChild(new ZoomControl(new ZoomIn(),  parent,  ZOOM_PERCENT/100));
        addChild(new ZoomControl(new ZoomOut(), parent, -ZOOM_PERCENT/100));
    }

    protected function mouseWheel (event :MouseEvent) :void
    {
        ZoomControl.zoom(_parent, ZOOM_PERCENT/100);
    }

    protected function mouseDown (event :MouseEvent) :void
    {
        startDrag();
    }

    protected function mouseUp (event :MouseEvent) :void
    {
        stopDrag();
    }

    protected var _parent :DisplayObject;

    // How far to move on each click on an arrow
    protected static const STEP_SIZE :int = 50;

    // How many percent to nudge our zoom by
    protected static const ZOOM_PERCENT :Number = 2.5;

    [Embed(source="rsrc/compass.swf#cardtallyback")]
    protected static var CardBack :Class;
    [Embed(source="rsrc/compass.swf#panup")]
    protected static var PanUp :Class;
    [Embed(source="rsrc/compass.swf#pandown")]
    protected static var PanDown :Class;
    [Embed(source="rsrc/compass.swf#panleft")]
    protected static var PanLeft :Class;
    [Embed(source="rsrc/compass.swf#panright")]
    protected static var PanRight :Class;
    [Embed(source="rsrc/compass.swf#zoomin")]
    protected static var ZoomIn :Class;
    [Embed(source="rsrc/compass.swf#zoomout")]
    protected static var ZoomOut :Class;
}
}

import flash.display.DisplayObject;
import flash.display.Sprite;
import flash.events.MouseEvent;
import flash.events.Event;

class ScrollerControl extends Sprite
{
    public function ScrollerControl (art :DisplayObject, parent :DisplayObject)
    {
        _parent = parent;
        addChild(art);
        addEventListener(MouseEvent.MOUSE_DOWN, mouseDown);
    }

    protected function mouseDown (event :MouseEvent) :void
    {
        addEventListener(Event.ENTER_FRAME, enterFrame);
        addEventListener(MouseEvent.MOUSE_UP, mouseUp);

        // And call it once immediately
        enterFrame(null);
    }

    protected function mouseUp (event :MouseEvent) :void
    {
        removeEventListener(MouseEvent.MOUSE_UP, mouseUp);
        removeEventListener(Event.ENTER_FRAME, enterFrame);
    }

    protected function enterFrame (event :Event) :void
    {
        // Nothing by default
    }

    protected var _parent :DisplayObject;
}

class ZoomControl extends ScrollerControl
{
    public function ZoomControl (art :DisplayObject, parent :DisplayObject, zoomPercent :Number)
    {
        super(art, parent);
        _zoomPercent = zoomPercent;
    }

    public static function zoom (parent :DisplayObject, zoomPercent :Number) :void
    {
        // FIXME: We potentially want to zoom about the center of the board, or some such.
        var scale :Number = parent.scaleX + zoomPercent;

        scale = Math.max(0.1,  scale);
        scale = Math.min(2, scale);

        parent.scaleX = parent.scaleY = scale;
    }

    override protected function enterFrame (event :Event) :void
    {
        zoom(_parent, _zoomPercent);
    }

    protected var _zoomPercent :Number;
}

class PanControl extends ScrollerControl
{
    public function PanControl (art :DisplayObject, parent :DisplayObject, dx :int, dy :int)
    {
        super(art, parent);
        _dx = dx;
        _dy = dy;
    }

    override protected function enterFrame (event :Event) :void
    {
        _parent.x += _dx;
        _parent.y += _dy;

        // TODO: lock this to a reasonable border around the parent
    }

    protected var _dx :int;
    protected var _dy :int;
}


