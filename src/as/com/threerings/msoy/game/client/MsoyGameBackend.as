//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.DisplayObject;
import flash.display.Loader;
import flash.geom.Point;
import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.utils.Dictionary;

import com.whirled.game.client.GameBackend;
import com.whirled.game.data.WhirledGameObject;

import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.PlayerInfo;

/**
 * Implements the various Msoy specific parts of the Whirled Game backend.
 */
public class MsoyGameBackend extends GameBackend
{
    public function MsoyGameBackend (
        ctx :GameContext, gameObj :WhirledGameObject, ctrl :MsoyGameController)
    {
        super(ctx, gameObj, ctrl);
    }

    override protected function getHeadShot_v2 (occupant :int) :DisplayObject
    {
        validateConnected();
        var info :PlayerInfo = _gameObj.occupantInfo.get(occupant) as PlayerInfo;
        if (info != null) {
            var headshot :Headshot = _headshots[occupant];
            if (headshot == null) {
                _headshots[occupant] = headshot = new Headshot(info.getHeadshot());
            }
            return headshot;
        }

        log.warning("Unable to find occupant: " + occupant);
        return super.getHeadShot_v2(occupant); // return something that works anyway
    }

    override protected function getSize_v1 () :Point
    {
        var p :Point = super.getSize_v1();
        p.x = Math.max(p.x, 700);
        p.y = Math.max(p.y, 500);
        return p;
    }

    // from WhirledGameBackend
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

import com.threerings.util.ValueEvent;

import com.threerings.flash.MediaContainer;

import com.threerings.msoy.ui.MsoyMediaContainer;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * We use a MsoyMediaContainer so that the headshot can be bleeped, but we wrap
 * it inside this class so that the usercode cannot retrieve and fuxor with anything.
 */
class Headshot extends Sprite
{
    public function Headshot (desc :MediaDesc)
    {
        _container = new MsoyMediaContainer(desc);
        _container.addEventListener(MediaContainer.SIZE_KNOWN, handleMediaSizeKnown);
        super.addChild(_container);
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
        nope();
        return null;
    }

    override public function getChildByName (name :String) :DisplayObject
    {
        nope();
        return null;
    }

    override public function getChildIndex (child :DisplayObject) :int
    {
        nope();
        return 0;
    }

    override public function getObjectsUnderPoint (point :Point) :Array
    {
        nope();
        return null;
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

    protected function handleMediaSizeKnown (event :ValueEvent) :void
    {
        _container.x = (this.width - Number(event.value[0])) / 2;
        _container.y = (this.height - Number(event.value[1])) / 2;
    }

    protected var _container :MsoyMediaContainer;
}
