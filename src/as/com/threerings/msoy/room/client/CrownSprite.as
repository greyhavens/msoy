//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.MovieClip;
import flash.geom.Rectangle;
import com.threerings.display.DisplayUtil;
import flash.text.TextField;
import flash.display.Sprite;

public class CrownSprite extends Sprite
{
    public function get memberId () :int { return _memberId }

    public static function getBounds () :Rectangle
    {
        return new Rectangle(0, 0, SCALE*316, SCALE*324);
    }

    public function CrownSprite (memberId :int)
    {
        _memberId = memberId;

        _root.scaleX = _root.scaleY = SCALE;
        addChild(_root);

        // Play through the animation only once
        var crown :MovieClip = MovieClip(_root.getChildByName("crown"));
        crown.addFrameScript(crown.totalFrames - 1, function () :void {
            crown.gotoAndStop(1);
        });
    }

    public function setRating (rating :int) :void
    {
        var tf :TextField = TextField(DisplayUtil.findInHierarchy(_root, "rating"));
        tf.text = (rating > 0 ? "+" : "") + rating;
    }

    [Embed(source="../../../../../../../rsrc/media/crown.swf", symbol="dj_crown")]
    protected static const ASSET :Class;

    protected static const SCALE :Number = 0.2;

    protected var _root :Sprite = new ASSET();
    protected var _memberId :int;
}
}
