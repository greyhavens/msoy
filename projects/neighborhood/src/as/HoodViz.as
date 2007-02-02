package {

import flash.display.*;
import flash.text.*;
import flash.geom.*;
import flash.events.*;
import flash.net.*;
import flash.ui.*;
import flash.utils.*;
import flash.external.ExternalInterface;

import mx.core.SoundAsset;

import com.threerings.msoy.hood.Building;
import com.threerings.msoy.hood.Neighbor;
import com.threerings.msoy.hood.Neighborhood;
import com.threerings.msoy.hood.NeighborGroup;
import com.threerings.msoy.hood.NeighborMember;
import com.adobe.serialization.json.JSONDecoder;
import com.threerings.util.EmbededClassLoader;

[SWF(width="640", height="480")]
public class HoodViz extends Sprite
{
    protected var _ecl :EmbededClassLoader;
    public function HoodViz ()
    {
        _ecl = new EmbededClassLoader();
        _ecl.addEventListener(Event.COMPLETE, eclDone);
        _ecl.load(new _viz());
    }
    
    protected function eclDone(event :Event) :void
    {
        var soy :Class = _ecl.getClass("soy_master");

        _friend = new Building(_ecl.getClass("house_tile"), _ecl.getClass("populate_house"), soy);
        this.stage.addChild(_friend);
        _group = new Building(_ecl.getClass("group_tile"), _ecl.getClass("populate_group"), soy);
        this.stage.addChild(_group);

        _vacant = _ecl.getClass("vacant_tile");
        _roadNS = _ecl.getClass("road_ns_tile");
        _roadEW = _ecl.getClass("road_ew_tile");
        _road4Way = _ecl.getClass("road_intersection_tile");
        _roadHouse = _ecl.getClass("road_house_tile");
        _roadHouseEndW = _ecl.getClass("road_end_w_tile");
        _roadHouseEndE = _ecl.getClass("road_end_e_tile");

        var data :Object;
        if (false) {
            data = new DebugData();
        } else {
            data = this.root.loaderInfo.parameters;
        }
        _hood = Neighborhood.fromParameters(data);

        _canvas = new Sprite();
        this.addChild(_canvas);

        // compute a very rough bounding rectangle for the visible houses
        var radius :int =
            3 + Math.ceil(Math.sqrt(Math.max(_hood.groups.length, _hood.friends.length)));

        var drawables :Array = new Array();
        var distances :Array = new Array();
        // draw the grid, building a metric mapping at the same time
        for (var y :int = -radius; y <= radius; y ++) {
            drawables[y] = new Array();
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
                    distances.push({ x: x, y: y, dist: d });
                }
            }
        }

        // sort the metric according to distance
        distances.sortOn([ "dist", "x", "y" ], Array.NUMERIC);

        // then go through houses in order of radial distance and register friends and groups
        var nextFriend :int = 0;
        var nextGroup :int = 0;

        // use a zig-zag algorithm (there's probably a name for it) to sprinkle houses and groups
        var selectionCount :Number = _hood.friends.length / 2;
        for each (var tile :Object in distances) {
            if (selectionCount > _hood.friends.length) {
                if (nextGroup < _hood.groups.length) {
                    drawables[tile.y][tile.x] = _hood.groups[nextGroup ++];
                }
                selectionCount -= _hood.friends.length;        
            } else {
                if (nextFriend < _hood.friends.length) {
                    drawables[tile.y][tile.x] = _hood.friends[nextFriend ++];
                }
                selectionCount += _hood.groups.length;
            }
            if (nextGroup == _hood.groups.length && nextFriend == _hood.friends.length) {
                break;
            }
        }

        if (_hood.centralMember != null) {
            drawables[0][1] = _hood.centralMember;
        }
        if (_hood.centralGroup != null) {
            drawables[0][-1] = _hood.centralGroup;
        }

        for (y = -radius; y <= radius; y ++) {
            if ((y % 2) == 0 || (drawables[y-1][-1] == null && drawables[y-1][1] == null)) {
                addBit(_roadNS, 0, y, false, null);
            } else {
                addBit(_road4Way, 0, y, false, null);
            }
            for (x = radius; x >= -radius; x --) {
                if (x == 0) {
                    continue;
                }
                if ((y % 2) == 0) {
                    if (drawables[y][x] is NeighborMember) {
                        addBit(_friend, x, y, true, drawables[y][x]);
                    } else if (drawables[y][x] is NeighborGroup) {
                        addBit(_group, x, y, true, drawables[y][x]);
                    } else {
                        addBit(_vacant, x, y, false, null);
                    }
                } else {
                    if (drawables[y-1][x] == null) {
//                        this bit has no purpose until we interject empty plots
//                        addBit(_roadEW, x, y, false, null);
                        addBit(_vacant, x, y, false, null);
                    } else if (x != 1 && drawables[y-1][x-1] == null) {
                        addBit(_roadHouseEndW, x, y, true, null);
                    } else if (x != -1 && drawables[y-1][x+1] == null) {
                        addBit(_roadHouseEndE, x, y, true, null);
                    } else {
                        addBit(_roadHouse, x, y, true, null);
                    }
                }
            }
        }

        var xScale :Number = 640 / (_bound.right - _bound.left);
        var yScale :Number = 480 / (_bound.bottom - _bound.top);
        _canvas.x = -_bound.left * xScale;
        _canvas.y = -_bound.top * yScale;
        var scale :Number = Math.min(xScale, yScale);
        _canvas.scaleX = scale * 0.9;
        _canvas.scaleY = scale * 0.9;
    }


    // funkily skews, scales, and translates the loaded logo into the billboard
    public function logoLoaded(event: Event) :void
    {
        var content: DisplayObject = event.target.content;
        // scale depending on which dimension is constrained
        var scale :Number = Math.min(50 / content.width, 45/content.height);
        // center vertically
        var zOff :Number = (45 - scale*content.height)/2;
        // center horizontally (the 175 is the scale imposed by skewX() et al)
        var xOff :Number = (50 - scale*content.width)/2/175;
        // map the ground coordinates to skewed display coordinates
        var p: Point = skew(0.058 + xOff, 0.94);
        // then simply apply the matrix, with -0.38 being the skew constant. note that
        // the zOffset is added raw; it's not a skewed coordinate
        content.transform.matrix = new Matrix(scale, -scale*0.38, 0, scale, p.x, p.y + zOff);
    }

    protected function addBit (bitType :Object, x :Number, y :Number, update:Boolean,
                               neighbor: Neighbor) :void
    {
        var bit :MovieClip;
        if (bitType is Class) {
            bit = new bitType();
            bit.gotoAndStop((int) (Math.random() * bit.totalFrames));
        } else {
            var building :Building = (bitType as Building);
            // let's illustrate the population of a place by the square root of its
            // actual population in soy figures, lest we utterly overwhelm the map;
            // thus 1 pop -> 1 soy, 25 pop -> 5 soys, 100 pop -> 10 soys.
            bit = building.getPopulatedTile(
                Math.random() * building.variationCount,
                Math.round(Math.sqrt(neighbor.population)));
        }

        if (neighbor is NeighborGroup) {
            var logo :String = (neighbor as NeighborGroup).groupLogo;
            if (logo != null) {
                // dynamically load the group's logo
                var loader :Loader = new Loader();
                loader.load(new URLRequest("/media/" + logo));
                // we want to know when the logo is loaded so we can do our magic
                loader.contentLoaderInfo.addEventListener(Event.COMPLETE, logoLoaded);
                bit.addChild(loader);
            }
        }

        var bitHolder :ToolTipSprite = new ToolTipSprite();
        bitHolder.addChild(bit);
        var p :Point = skew(x, y);
        bitHolder.x = p.x;
        bitHolder.y = p.y;
        if (neighbor != null) {
            bitHolder.neighbor = neighbor;
            bitHolder.addEventListener(MouseEvent.ROLL_OVER, rollOverHandler);
            bitHolder.addEventListener(MouseEvent.ROLL_OUT, rollOutHandler);
            bitHolder.addEventListener(MouseEvent.CLICK, clickHandler);
        }
        _canvas.addChild(bitHolder);

        if (update) {
            _bound = _bound.union(bitHolder.getBounds(_canvas));
            trace("new bound: " + _bound);
        }
    }

    // the magic numbers that describe the drawn tiles' geometry
    protected function skew(x :Number, y :Number) :Point
    {
        var f :Number = 0.88;
        return new Point(f*x*174 + f*y*81, -f*x*69 + f*y*155);
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
    protected var _bound :Rectangle = new Rectangle();

    protected var _friend :Building;
    protected var _group :Building;

    protected var _vacant :Class;
    protected var _roadNS :Class;
    protected var _roadEW :Class;
    protected var _road4Way :Class;
    protected var _roadHouse :Class;
    protected var _roadHouseEndW :Class;
    protected var _roadHouseEndE :Class;

    [Embed(source="viz.swf", mimeType="application/octet-stream")]
    protected const _viz:Class;
}
}
