//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.DisplayObject;
import flash.display.Loader;

import flash.geom.Point;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;

import flash.utils.Dictionary;

import com.threerings.crowd.data.OccupantInfo;

import com.whirled.game.client.FlashGameBackend;

import com.whirled.game.data.WhirledGameObject;

import com.threerings.msoy.data.VizMemberName;

import com.threerings.msoy.game.data.MsoyGameConfig;

/**
 * Implements the various Msoy specific parts of the Whirled Game backend.
 */
public class MsoyGameBackend extends FlashGameBackend
{
    public function MsoyGameBackend (
        ctx :GameContext, gameObj :WhirledGameObject, ctrl :MsoyGameController)
    {
        super(ctx, gameObj, ctrl);
    }

    // from FlashGameBackend
    override protected function getHeadShot_v2 (occupant :int) :DisplayObject
    {
        validateConnected();
        var info :OccupantInfo = _gameObj.occupantInfo.get(occupant) as OccupantInfo;
        if (info != null) {
            var vizName :VizMemberName = info.username as VizMemberName;
            if (vizName != null) {
                var headshot :Headshot = _headshots[occupant];
                if (headshot == null) {
                    _headshots[occupant] = headshot = new Headshot(vizName.getPhoto());
                }
                return headshot;
            }
        }

        log.warning("Unable to find occupant, or username is not a VizMemberName: " + occupant);
        return super.getHeadShot_v2(occupant); // return something that works anyway
    }

    // from FlashGameBackend
    override protected function getSize_v1 () :Point
    {
        var p :Point = super.getSize_v1();
        p.x = Math.max(p.x, 700);
        p.y = Math.max(p.y, 500);
        return p;
    }

    // from BaseGameBackend
    override protected function playerOwnsData (type :int, ident :String) :Boolean
    {
        var cfg :MsoyGameConfig = (_ctrl.getPlaceConfig() as MsoyGameConfig);
        return (_ctx as GameContext).getPlayerObject().ownsGameContent(cfg.getGameId(), type, ident)
    }

    /** A cache of loaded avatar headshots, indexed by occupant id. */
    protected var _headshots :Dictionary = new Dictionary();
}
}

import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.geom.Point;

import com.threerings.util.Log;

import com.threerings.msoy.ui.ScalingMediaContainer;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * We use a MsoyMediaContainer so that the headshot can be bleeped, but we wrap
 * it inside this class so that the usercode cannot retrieve and fuxor with anything.
 */
class Headshot extends Sprite
{
    public function Headshot (desc :MediaDesc)
    {
        super.addChild(ScalingMediaContainer.createView(desc));
    }

    override public function get width () :Number
    {
        return MediaDesc.THUMBNAIL_WIDTH;
    }

    override public function get height () :Number
    {
        return MediaDesc.THUMBNAIL_HEIGHT;
    }

    override public function addChild (child :DisplayObject) :DisplayObject
    {
        nope();
        return null;
    }

    override public function addChildAt (child :DisplayObject, index :int) :DisplayObject
    {
        nope();
        return null;
    }

    override public function contains (child :DisplayObject) :Boolean
    {
        return (child == this); // make it only work for us..
    }

    override public function getChildAt (index :int) :DisplayObject
    {
        return null;
    }

    override public function getChildByName (name :String) :DisplayObject
    {
        return null;
    }

    override public function getChildIndex (child :DisplayObject) :int
    {
        return -1;
    }

    override public function getObjectsUnderPoint (point :Point) :Array
    {
        return [];
    }

    override public function removeChild (child :DisplayObject) :DisplayObject
    {
        nope();
        return null;
    }

    override public function removeChildAt (index :int) :DisplayObject
    {
        nope();
        return null;
    }

    override public function setChildIndex (child :DisplayObject, index :int) :void
    {
        nope();
    }

    override public function swapChildren (child1 :DisplayObject, child2 :DisplayObject) :void
    {
        nope();
    }

    override public function swapChildrenAt (index1 :int, index2 :int) :void
    {
        nope();
    }

    protected function nope () :void
    {
        throw new Error("Operation not permitted.");
    }
}
