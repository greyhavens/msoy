//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.MovieClip;
import flash.display.Sprite;
import flash.geom.Rectangle;
import flash.text.TextField;

import com.threerings.display.DisplayUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.room.client.MemberSprite;

public class CrownDecoration
{
    public function get avatar () :MemberSprite { return _avatar }

    public function CrownDecoration (avatar :MemberSprite)
    {
        _avatar = avatar;
        _root.scaleX = _root.scaleY = SCALE;

        _avatar.addDecoration(_root, {
            toolTip: Msgs.WORLD.get("i.best_dj", avatar.getOccupantInfo().username),
            weight: 100,
            bounds: new Rectangle(0, 0, SCALE*316, SCALE*324)
        });

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

    public function shutdown () :void
    {
        _avatar.removeDecoration(_root);
    }

    [Embed(source="../../../../../../../rsrc/media/crown.swf", symbol="dj_crown")]
    protected static const ASSET :Class;

    protected static const SCALE :Number = 0.2;

    protected var _root :Sprite = new ASSET();
    protected var _avatar :MemberSprite;
}
}
