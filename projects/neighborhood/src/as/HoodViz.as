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
import com.threerings.util.EmbeddedSwfLoader;

[SWF(width="640", height="480")]
public class HoodViz extends Sprite
{
    public function HoodViz ()
    {
        var data :Object;
        if (false) {
            data = new DebugData();
        } else {
            data = this.root.loaderInfo.parameters;
        }
        _hood = Neighborhood.fromParameters(data);

        _vizLoader = new URLLoader();
        _vizLoader.addEventListener(Event.COMPLETE, vizDone);
        _vizLoader.dataFormat = URLLoaderDataFormat.BINARY;
        _vizLoader.load(new URLRequest(data.skinURL));
    }
    
    protected function vizDone (event :Event) :void
    {
        _vizSplitter = new EmbeddedSwfLoader();
        _vizSplitter.addEventListener(Event.COMPLETE, eclDone);
        _vizSplitter.load((event.target as URLLoader).data);
    }
    
    protected function eclDone(event :Event) :void
    {
        var soy :Class = _vizSplitter.getClass("soy_master");

        _friend = new Building(_vizSplitter.getClass("house_tile"), _vizSplitter.getClass("populate_house"), soy);
        this.stage.addChild(_friend);
        _group = new Building(_vizSplitter.getClass("group_tile"), _vizSplitter.getClass("populate_group"), soy);
        this.stage.addChild(_group);

        _vacant = _vizSplitter.getClass("vacant_tile");
        _roadHouse = _vizSplitter.getClass("road_house_tile");
        _roadNS = _vizSplitter.getClass("road_ns_tile");
        _roadEW = _vizSplitter.getClass("road_ew_tile");
        _roadNSE = _vizSplitter.getClass("road_nse_tile");
        _roadNSW = _vizSplitter.getClass("road_nsw_tile");
        _road4Way = _vizSplitter.getClass("road_nsew_tile");
        _roadHouseEndW = _vizSplitter.getClass("road_end_w_tile");
        _roadHouseEndE = _vizSplitter.getClass("road_end_e_tile");

        _canvas = new Sprite();
        this.addChild(_canvas);

        // compute a very rough bounding rectangle for the visible houses
        var radius :int =
            3 + Math.ceil(Math.sqrt(Math.max(_hood.groups.length, _hood.friends.length)));

        var drawables :Array = new Array();
        var distances :Array = new Array();
        // draw the grid, building a metric mapping at the same time
        for (var y :int = -radius-1; y <= radius; y ++) {
            drawables[y] = new Array();
            for (var x :int = radius; x >= -radius; x --) {
                if (x == 0) {
                    continue;
                }
                if (y == 0 && ((_hood.centralMember != null && x == 1) ||
                               (_hood.centralGroup != null && x == -1))) {
                    continue;
                }
                if ((y % 2) == 0) {
                    var d :Number = x*x + y*y;
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
            for (x = radius; x >= -radius; x --) {
                if ((y % 2) == 0) {
                    if (x == 0) {
                        addBit(_roadNS, 0, y, false, null);
                    } else if (drawables[y][x] is NeighborMember) {
                        addBit(_friend, x, y, true, drawables[y][x]);
                    } else if (drawables[y][x] is NeighborGroup) {
                        addBit(_group, x, y, true, drawables[y][x]);
                    } else {
                        addBit(_vacant, x, y, false, null);
                    }
                } else {
                    if (x == 0) {
                        if (drawables[y-1][-1] == null) {
                            if (drawables[y-1][1] == null) {
                                addBit(_roadNS, 0, y, false, null);
                            } else {
                                addBit(_roadNSE, 0, y, false, null);
                            }
                        } else if (drawables[y-1][1] == null) {
                            addBit(_roadNSW, 0, y, false, null);
                        } else {
                            addBit(_road4Way, 0, y, false, null);
                        }
                    } else if (drawables[y-1][x] == null) {
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
                // if there is a logo, we dynamically load it
                var loader :Loader = new Loader();
                // first, find the designated logo-holding area in the tile
                var logoHolder :MovieClip = bit.getChildByName("logo_placer") as MovieClip;
                // if we did find a logoHolder, load the logo into it
                if (logoHolder != null) {
                    logoHolder.addChild(loader);
                    // set the scale to zero so that a large image doesn't resize the holder
                    loader.scaleX = loader.scaleY = 0;
                    // by default the image loaders in the center; shift it up and left
                    loader.x = -logoHolder.width/2;
                    loader.y = -logoHolder.height/2;

                    // we want to know when the logo is loaded so we can do resize magic
                    loader.contentLoaderInfo.addEventListener(Event.COMPLETE, logoLoaded);
                    // and we'll swallow IO errors rather than burden the user with them
                    loader.contentLoaderInfo.addEventListener(IOErrorEvent.IO_ERROR, logoError);
                    loader.load(new URLRequest("/media/" + logo));
                }
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

    // scales the loaded logo to the dimensions of the logo holder
    protected function logoLoaded(event: Event) :void
    {
        // get references to the loaded image, the image loader, and the logo holding clip
        var content: DisplayObject = event.target.content;
        var loader :DisplayObjectContainer = content.parent;
        var holder :DisplayObjectContainer = loader.parent;
        // now scale the image depending on which dimension is constrained
        var scale :Number = Math.min(holder.width / content.width, holder.height/content.height);
        // and center in either the x or y direction as needed
        content.x = (holder.width - scale*content.width)/2;
        content.y = (holder.height - scale*content.height)/2;
        // reset the loader's scale (it was set to zero during loading)
        loader.scaleX = loader.scaleY = 1;
        // and finally apply the image scale
        content.scaleX = content.scaleY = scale;
    }

    // the magic numbers that describe the drawn tiles' geometry
    protected function skew(x :Number, y :Number) :Point
    {
        var f :Number = 0.88;
        return new Point(f*x*174 + f*y*81, -f*x*69 + f*y*155);
    }

    protected function clickHandler (event :MouseEvent) :void
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

    protected function logoError (event :IOErrorEvent) :void
    {
        trace("Error loading URL: " + event.text);
        // do nothing else
    }

    protected function rollOverHandler (event :MouseEvent) :void
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

    protected function rollOutHandler (event :MouseEvent) :void
    {
        if (_tip is Sprite) {
            this.removeChild(_tip);
            _tip = null;
        }
    }

    protected var _vizSplitter :EmbeddedSwfLoader;
    protected var _vizLoader :URLLoader;

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
    protected var _roadNSE :Class;
    protected var _roadNSW :Class;
    protected var _roadHouse :Class;
    protected var _roadHouseEndW :Class;
    protected var _roadHouseEndE :Class;
}
}
