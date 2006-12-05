package {

import flash.display.*;
import flash.text.*;
import flash.events.*;
import flash.ui.*;
import flash.utils.*;
import flash.external.ExternalInterface;

import mx.core.SoundAsset;

import com.threerings.msoy.hood.Neighborhood;
import com.threerings.msoy.hood.NeighborGroup;
import com.threerings.msoy.hood.NeighborFriend;
import com.adobe.serialization.json.JSONDecoder;

[SWF(width="640", height="480")]
public class HoodViz extends Sprite
{
    public function HoodViz ()
    {
        var paramObj:Object = LoaderInfo(this.root.loaderInfo).parameters;
        var json:String;
        if (paramObj == null || paramObj.data == null) {
            // debug fun
            json = "{\"groups\":[{\"id\":3,\"members\":1,\"name\":\"Spud Muffins\"},{\"id\":2,\"members\":1,\"name\":\"Madison Bird Fondlers (MBF)\"},{\"id\":4,\"members\":1,\"name\":\"sdfsdf\"},{\"id\":5,\"members\":1,\"name\":\"Spam Spam Spam\"},{\"id\":6,\"members\":1,\"name\":\"A B C\"},{\"id\":3,\"members\":1,\"name\":\"Spud Muffins\"},{\"id\":2,\"members\":1,\"name\":\"Madison Bird Fondlers (MBF)\"},{\"id\":4,\"members\":1,\"name\":\"sdfsdf\"},{\"id\":5,\"members\":1,\"name\":\"Spam Spam Spam\"},{\"id\":6,\"members\":1,\"name\":\"A B C\"}],\"id\":1,\"friends\":[{\"id\":2,\"isOnline\":false,\"name\":\"elvis\"}, {\"id\":3,\"isOnline\":false,\"name\":\"santa\"},{\"id\":2,\"isOnline\":false,\"name\":\"elvis\"}, {\"id\":3,\"isOnline\":false,\"name\":\"santa\"},{\"id\":2,\"isOnline\":false,\"name\":\"elvis\"}, {\"id\":3,\"isOnline\":false,\"name\":\"santa\"}],\"name\":\"Zell\"}";
        } else {
            json = paramObj.data;
        }

        _hood = Neighborhood.fromJSON(new JSONDecoder(json).getObject());

        _canvas = new Sprite();
        this.addChild(_canvas);

        addBit(_myHouse, 1, 0, false, null);

        // compute a very rough bounding rectangle for the visible houses
        var radius:int = 3 + Math.ceil(Math.sqrt(Math.max(_hood.groups.length, _hood.friends.length)));

        var distances:Array = new Array();
        // draw the grid, building a metric mapping at the same time
        for (var y:int = -radius; y <= radius; y ++) {
            if ((y % 2) == 0) {
                addBit(_roadNS, 0, y, false, null);
            } else {
                addBit(_road4Way, 0, y, false, null);
            }
            for (var x:int = radius; x >= -radius; x --) {
                if (x == 0 || (y == 0 && x == 1)) {
                    continue;
                }

                if ((y % 2) == 0) {
                    var d:Number = (x-1)*(x-1) + y*y;
                    distances.push({ x:x, y:y, dist:d });
                } else {
                    addBit(_roadWE, x, y, false, null);
                }
            }
        }
        // sort the metric according to distance
        distances.sortOn([ "dist", "x", "y" ], Array.NUMERIC);

        var nextFriend:int = 0;
        var nextGroup:int = 0;
        for each (var tile:Object in distances) {
            if (tile.y < 0) {
                if (nextGroup < _hood.groups.length) {
                    var group:NeighborGroup = _hood.groups[nextGroup ++];
                    addBit(_group, tile.x, tile.y, true,
                           group.groupName + "\n" + "Members: " + group.members);
                }
            } else if (nextFriend < _hood.friends.length) {
                var friend:NeighborFriend = _hood.friends[nextFriend ++];
                addBit(_friend, tile.x, tile.y, true,
                       friend.memberName + " (" + (friend.isOnline ? "Online": "Offline") + ")");
            }
        }
        var scale :Number = Math.min(640 / (_bound.x.max - _bound.x.min),
                                     480 / (_bound.y.max - _bound.y.min));
        _canvas.x = -_bound.x.min * scale;
        _canvas.y = -_bound.y.min * scale;
        _canvas.scaleX = scale * 0.9;
        _canvas.scaleY = scale * 0.9;
    }

    protected function addBit (bitType :Class, x :Number, y :Number, update:Boolean,
                               toolTip:String) :void
    {
        var bit:MovieClip = new bitType();
        bit.width = 256;
        bit.height = 224;
        var bitHolder :ToolTipSprite = new ToolTipSprite();
        bitHolder.addChild(bit);
        bitHolder.x = y * 82 + x * 175;
        bitHolder.y = y * 156 - x * 69;
        if (toolTip != null) {
            bitHolder.toolTip = toolTip;
            bitHolder.addEventListener(MouseEvent.ROLL_OVER, rollOverHandler);
            bitHolder.addEventListener(MouseEvent.ROLL_OUT, rollOutHandler);
        }
        _canvas.addChild(bitHolder);

        if (update) {
            _bound.x.min = Math.min(_bound.x.min, bitHolder.x);
            _bound.x.max = Math.max(_bound.x.max, bitHolder.x + bit.width);
            _bound.y.min = Math.min(_bound.y.min, bitHolder.y);
            _bound.y.max = Math.max(_bound.y.max, bitHolder.y + bit.height);
        }
    }

    public function rollOverHandler (event :MouseEvent) :void
    {
        var text :String = (event.target as ToolTipSprite).toolTip;
        _tip = new Sprite();
        with (_tip.graphics) {
            clear();
            beginFill(0xFFFFFF);
            drawRoundRect(0, 0, 120, 60, 10, 10);
            endFill();
            lineStyle(2, 0x000000);
            drawRoundRect(0, 0, 120, 60, 10, 10);
        }
        _tip.x = event.stageX - 20;
        _tip.y = event.stageY - 20 - _tip.height;

        var tipText :TextField = new TextField();
        tipText.text = text;
        tipText.autoSize = TextFieldAutoSize.CENTER;
        tipText.wordWrap = true;
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
