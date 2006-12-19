package {

import flash.display.*;
import flash.text.*;
import flash.events.*;
import flash.net.*;
import flash.ui.*;
import flash.utils.*;
import flash.external.ExternalInterface;

import mx.core.SoundAsset;

import com.threerings.msoy.hood.Neighbor;
import com.threerings.msoy.hood.Neighborhood;
import com.threerings.msoy.hood.NeighborGroup;
import com.threerings.msoy.hood.NeighborMember;
import com.adobe.serialization.json.JSONDecoder;

[SWF(width="640", height="480")]
public class HoodViz extends Sprite
{
    public function HoodViz ()
    {
        _hood = Neighborhood.fromLoaderInfo(this.root.loaderInfo);

        _canvas = new Sprite();
        this.addChild(_canvas);

        if (_hood.centralMember != null) {
            addBit(_myHouse, 1, 0, true, _hood.centralMember);
        }
        if (_hood.centralGroup != null) {
            addBit(_myHouse, -1, 0, true, _hood.centralGroup);
        }

        // compute a very rough bounding rectangle for the visible houses
        var radius :int =
            3 + Math.ceil(Math.sqrt(Math.max(_hood.groups.length, _hood.friends.length)));

        var distances :Array = new Array();
        // draw the grid, building a metric mapping at the same time
        for (var y :int = -radius; y <= radius; y ++) {
            if ((y % 2) == 0) {
                addBit(_roadNS, 0, y, false, null);
            } else {
                addBit(_road4Way, 0, y, false, null);
            }
            for (var x :int = radius; x >= -radius; x --) {
                if (x == 0) {
                    continue;
                }
                if (y == 0 && (_hood.centralMember != null && x == 1) ||
                              (_hood.centralGroup != null && x == -1)) {
                    continue;
                }
                if ((y % 2) == 0) {
                    var d :Number = (x-1)*(x-1) + y*y;
                    distances.push({ x:x, y:y, dist:d });
                } else {
                    addBit(_roadWE, x, y, false, null);
                }
            }
        }

        // sort the metric according to distance
        distances.sortOn([ "dist", "x", "y" ], Array.NUMERIC);

        // then go through houses in order of radial distance and register friends and groups
        var drawables :Array = new Array();
        var nextFriend :int = 0;
        var nextGroup :int = 0;
        for each (var tile :Object in distances) {
            if (tile.y < 0) {
                if (nextGroup < _hood.groups.length) {
                    var group :NeighborGroup = _hood.groups[nextGroup ++];
                    drawables.push({ bit: _group, x: tile.x, y: tile.y, neighbor: group });
                }
            } else if (nextFriend < _hood.friends.length) {
                var friend :NeighborMember = _hood.friends[nextFriend ++];
                drawables.push({ bit: _friend, x: tile.x, y: tile.y, neighbor: friend });
            }
        }

        // now sort the actual friends and groups by x for draw order to be correct
        drawables.sortOn([ "x", "y" ], Array.NUMERIC | Array.DESCENDING);

        // and finally draw'em all
        for each (var drawable :Object in drawables) {
            addBit(drawable.bit, drawable.x, drawable.y, true, drawable.neighbor);
        }
        var scale :Number = Math.min(640 / (_bound.x.max - _bound.x.min),
                                     480 / (_bound.y.max - _bound.y.min));
        _canvas.x = -_bound.x.min * scale;
        _canvas.y = -_bound.y.min * scale;
        _canvas.scaleX = scale * 0.9;
        _canvas.scaleY = scale * 0.9;
    }

    protected function addBit (bitType :Class, x :Number, y :Number, update:Boolean,
                               neighbor: Neighbor) :void
    {
        var bit :MovieClip = new bitType();
        bit.width = 256;
        bit.height = 224;
        var bitHolder :ToolTipSprite = new ToolTipSprite();
        bitHolder.addChild(bit);
        bitHolder.x = y * 82 + x * 175;
        bitHolder.y = y * 156 - x * 69;
        if (neighbor != null) {
            bitHolder.neighbor = neighbor;
            bitHolder.addEventListener(MouseEvent.ROLL_OVER, rollOverHandler);
            bitHolder.addEventListener(MouseEvent.ROLL_OUT, rollOutHandler);
            bitHolder.addEventListener(MouseEvent.CLICK, clickHandler);
        }
        _canvas.addChild(bitHolder);

        if (update) {
            _bound.x.min = Math.min(_bound.x.min, bitHolder.x);
            _bound.x.max = Math.max(_bound.x.max, bitHolder.x + bit.width);
            _bound.y.min = Math.min(_bound.y.min, bitHolder.y);
            _bound.y.max = Math.max(_bound.y.max, bitHolder.y + bit.height);
        }
    }

    public function clickHandler (event :MouseEvent) :void
    {
        var neighbor :Neighbor = (event.currentTarget as ToolTipSprite).neighbor;
        var url :String = "/world/#";
        if (neighbor is NeighborMember) {
            var friend :NeighborMember = neighbor as NeighborMember;
            if (_hood.centralMember != null &&
                _hood.centralMember.memberId == friend.memberId) {
                // clicking on the centre member takes us to their profile page
                url += "m" + friend.memberId;
            } else {
                // clicking on another member just goes to -their- neighborhood
                url += "nm" + friend.memberId;
            }
        } else {
            var group :NeighborGroup = neighbor as NeighborGroup;
            if (_hood.centralGroup != null &&
                _hood.centralGroup.groupId == group.groupId) {
                // clicking on the centre group takes us to their profile page
                url += "g" + group.groupId;
            } else {
                // clicking on another group just goes to -their- neighborhood
                url += "ng" + group.groupId;
            }
        }
        navigateToURL(new URLRequest(url), "_self");
    }

    public function rollOverHandler (event :MouseEvent) :void
    {

        var neighbor :Neighbor = (event.target as ToolTipSprite).neighbor;
        var text :String;
        if (neighbor is NeighborMember) {
            var friend :NeighborMember = neighbor as NeighborMember;
            text = friend.memberName + " (" + (friend.isOnline ? "online": "offline") + ")";
            if (friend.created != null) {
                text += "\n\n" + "Created: " + friend.created.toLocaleDateString();
            }
            if (friend.lastSession != null) {
                text += "\n" + "Last on: " + friend.lastSession.toLocaleDateString();
            }
        } else {
            var group :NeighborGroup = neighbor as NeighborGroup;
            text = group.groupName + "\n" + "Members: " + group.members;
        }

        _tip = new Sprite();
        with (_tip.graphics) {
            clear();
            beginFill(0xFFFFFF);
            drawRoundRect(0, 0, 180, 80, 10, 10);
            endFill();
            lineStyle(2, 0x000000);
            drawRoundRect(0, 0, 180, 80, 10, 10);
        }
        _tip.x = event.stageX - 20;
        _tip.y = event.stageY - 20 - _tip.height;

        var tipText :TextField = new TextField();
        tipText.text = text;
        tipText.autoSize = TextFieldAutoSize.CENTER;
        tipText.wordWrap = false;
        _tip.addChild(tipText);
        tipText.y = (_tip.height - tipText.height)/2;
        tipText.x = (_tip.width - tipText.width)/2;

        this.addChild(_tip);
    }

    public function rollOutHandler (event :MouseEvent) :void
    {
        if (_tip is Sprite) {
            this.removeChild(_tip);
            _tip = null;
        }
    }

    protected var _tip :Sprite;
    protected var _hood :Neighborhood;
    protected var _canvas :Sprite;
    protected var _bound :Object = { x: { min: 0, max: 0 }, y: { min: 0, max: 0 } };

    [Embed(source="myhouse.swf")]
    protected static const _myHouse :Class;

    [Embed(source="group.swf")]
    protected static const _group :Class;

    [Embed(source="friend.swf")]
    protected static const _friend :Class;

    [Embed(source="road_ns.swf")]
    protected static const _roadNS :Class;

    [Embed(source="road_we.swf")]
    protected static const _roadWE :Class;

    [Embed(source="road_4way.swf")]
    protected static const _road4Way :Class;
}
}
