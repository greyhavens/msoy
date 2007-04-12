//
// $Id$

package artvatar {

import flash.display.Shape;
import flash.display.Sprite;

import flash.events.MouseEvent;

import com.threerings.flash.SimpleTextButton;

/**
 * Displays our drawing controls.
 */
public class ControlPanel extends Sprite
{
    public function ControlPanel (canvas :Canvas, doneFunc :Function)
    {
        var done :SimpleTextButton = new SimpleTextButton("Done");
        done.addEventListener(MouseEvent.CLICK, function (event: MouseEvent) :void {
            doneFunc(true);
        });
        addChild(done);

        var cancel :SimpleTextButton = new SimpleTextButton("Cancel");
        cancel.addEventListener(MouseEvent.CLICK, function (event: MouseEvent) :void {
            doneFunc(false);
        });
        addChild(cancel);
    }

    protected var _canvas :Canvas;
}
}
