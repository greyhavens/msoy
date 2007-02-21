package {

import flash.display.*;
import flash.text.*;
import flash.geom.*;
import flash.events.*;
import flash.net.*;
import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.ui.*;
import flash.utils.*;
import flash.external.ExternalInterface;

import com.threerings.msoy.hood.*;
import com.threerings.util.NetUtil;
import com.threerings.util.Random;
import com.threerings.util.StringUtil;
import com.threerings.util.DateUtil;

import com.adobe.serialization.json.JSONDecoder;

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

        var seed :int = 0;
        if (_hood.centralMember != null) {
            seed = _hood.centralMember.memberId;
        } else if (_hood.centralGroup != null) {
            seed = _hood.centralGroup.groupId;
        }
        _random = new Random(seed);

        _loader = new Loader();
        _loader.contentLoaderInfo.addEventListener(Event.COMPLETE, eclDone);
        var context :LoaderContext = new LoaderContext();
        context.applicationDomain = ApplicationDomain.currentDomain;
        _loader.load(new URLRequest(data.skinURL), context);
    }

    protected function getClass(name :String) :Class
    {
        return getDefinitionByName(name) as Class;
    }

    protected function eclDone(event :Event) :void
    {
        var soy :Class = getClass("soy_master");

        _house = new Building(getClass("house_tile"), getClass("populate_house"), soy);
        stage.addChild(_house);
        _plaqueHouse = getClass("plaque_house");

        _group = new Building(getClass("group_tile"), getClass("populate_group"), soy);
        stage.addChild(_group);
        _plaqueGroup = getClass("plaque_group");

        _game = new Building(getClass("game_tile"), getClass("populate_game"), soy);
        stage.addChild(_game);
        _plaqueGame = getClass("plaque_game");

        _vacant = getClass("vacant_tile");
        _roadHouse = getClass("road_house_tile");
        _roadNS = getClass("road_ns_tile");
        _roadEW = getClass("road_ew_tile");
        _roadNSE = getClass("road_nse_tile");
        _roadNSW = getClass("road_nsw_tile");
        _road4Way = getClass("road_nsew_tile");
        _roadHouseEndW = getClass("road_end_w_tile");
        _roadHouseEndE = getClass("road_end_e_tile");

        _rule = getClass("rule");

        _canvas = new Sprite();
        this.addChild(_canvas);

        // compute a very rough bounding rectangle for the visible houses
        var radius :int =
            3 + Math.ceil(Math.sqrt(_hood.groups.length +
                                    _hood.houses.length +
                                    _hood.games.length));

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

        // then go through buildings in order of radial distance and register houses and groups
        // pick tiles randomly from weighted intervals - generalizes to N tile types
        var housesLeft :int = _hood.houses.length;
        var groupsLeft :int = _hood.groups.length;
        var gamesLeft :int = _hood.games.length;
        var totalLeft :int = housesLeft + groupsLeft + gamesLeft;
        while (totalLeft > 0) {
            // pick a spot within [0, totalLeft)
            var rnd :Number = totalLeft * _random.nextNumber();
            // grab the tile to place
            var tile :Object = distances[--totalLeft];
            // and figure out which tile type's interval the spot's in
            if (rnd < groupsLeft) {
                drawables[tile.y][tile.x] = _hood.groups[--groupsLeft];
            } else if (rnd - groupsLeft < housesLeft) {
                drawables[tile.y][tile.x] = _hood.houses[--housesLeft];
            } else {
                drawables[tile.y][tile.x] = _hood.games[--gamesLeft];
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
                        addBit(_house, x, y, true, drawables[y][x]);
                    } else if (drawables[y][x] is NeighborGroup) {
                        addBit(_group, x, y, true, drawables[y][x]);
                    } else if (drawables[y][x] is NeighborGame) {
                        addBit(_game, x, y, true, drawables[y][x]);
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
        _canvas.graphics.beginFill(0xCBFE98);
        _canvas.graphics.drawRect(-_canvas.width, -_canvas.height,
                                  _canvas.width*2, _canvas.height*2);
        _canvas.graphics.endFill();

        var xScale :Number = stage.stageWidth / (160 + _bound.right - _bound.left);
        var yScale :Number = stage.stageHeight / (120 + _bound.bottom - _bound.top);
        var scale :Number = Math.min(xScale, yScale);
        _canvas.x = (80 -_bound.left) * scale;
        _canvas.y = (60 -_bound.top) * scale;
        _canvas.scaleX = scale;
        _canvas.scaleY = scale;
    }


    protected function addBit (bitType :Object, x :Number, y :Number, update:Boolean,
                               neighbor: Neighbor) :void
    {
        var bit :MovieClip;
        if (bitType is Class) {
            bit = new bitType();
            bit.gotoAndStop(1+_random.nextInt(bit.totalFrames));
        } else {
            var building :Building = (bitType as Building);
            // let's illustrate the population of a place by the square root of its
            // actual population in soy figures, lest we utterly overwhelm the map;
            // thus 1 pop -> 1 soy, 25 pop -> 5 soys, 100 pop -> 10 soys.
            bit = building.getPopulatedTile(
                1+_random.nextInt(building.variationCount),
                Math.round(Math.sqrt(neighbor.population)));
        }

        if (neighbor is LogoHolder) {
            var logo :String = (neighbor as LogoHolder).getLogoHash();
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
            _bound = _bound.union(bitHolder.getBounds(stage));
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
        if (neighbor.sceneId > 0) {
            url = "/world/#s" + neighbor.sceneId;
        } else if (neighbor is NeighborMember) {
            var house :NeighborMember = neighbor as NeighborMember;
            // clicking on a member takes us to their home scene
            url = "/world/#m" + house.memberId;
        } else if (neighbor is NeighborGroup) {
            var group :NeighborGroup = neighbor as NeighborGroup;
            // clicking on a group takes us to that group's home scene
            url = "/world/#g" + group.groupId;
        } else {
            var game :NeighborGame = neighbor as NeighborGame;
            // clicking on a game takes us to that game's lobby
            url = "/game/#" + game.gameId;
        }
        NetUtil.navigateToURL(url);
    }

    protected function logoError (event :IOErrorEvent) :void
    {
        trace("Error loading URL: " + event.text);
        // do nothing else
    }

    protected function rollOverHandler (event :MouseEvent) :void
    {
        var tile :ToolTipSprite = (event.target as ToolTipSprite);
        var neighbor :Neighbor = tile.neighbor;
        var above :String, below :String;
        if (neighbor is NeighborMember) {
            var house :NeighborMember = neighbor as NeighborMember;
            above = house.memberName;
            if (house.isOnline) {
                below = "Online";
            } else if (house.lastSession != null) {

                below = "Last on: " + DateUtil.getConversationalDateString(house.lastSession);
            }
            _tip = new _plaqueHouse();
        } else if (neighbor is NeighborGroup) {
            var group :NeighborGroup = neighbor as NeighborGroup;
            // TODO: Logo
            above = group.groupName;
            below = "Members: " + group.members;
            _tip = new _plaqueGroup();
        } else {
            var game :NeighborGame = neighbor as NeighborGame;
            // TODO: thumbnail?
            above = game.gameName;
            // below = "Players: " + game.population;
            _tip = new _plaqueGame();
        }

        tile.addChild(_tip);

        var right :Number = tile.localToGlobal(_tip.getBounds(stage).bottomRight).x;
        var left :Number = tile.localToGlobal(_tip.getBounds(stage).topLeft).x;
        _tip.scaleX = _tip.scaleY = 160 * _canvas.scaleX / (right - left);

//        throw new Error("gbr: " + tile.localToGlobal(_tip.getBounds(stage).bottomRight) +
//                        ", gtl: " + tile.localToGlobal(_tip.getBounds(stage).topLeft) +
//                        "_tip.scaleX = " + _tip.scaleX);

//        var p :Point = tile.globalToLocal(_canvas.localToGlobal(new Point(tile.x, tile.y)));
//        var p :Point = tile.globalToLocal(new Point(event.stageX, event.stageY));

//        _tip.x = p.x;
//        _tip.y = p.y;

        var tipText :TextField = new TextField();
        tipText.text = above;
        tipText.autoSize = TextFieldAutoSize.CENTER;
        tipText.y = -75;
        tipText.x = -tipText.width/2;
        _tip.addChild(tipText);

        if (below != null) {
            var rule :Sprite = new _rule();
            rule.y = -50;
            _tip.addChild(rule);

            tipText = new TextField();
            tipText.text = below;
            tipText.autoSize = TextFieldAutoSize.CENTER;
            tipText.y = -35;
            tipText.x = -tipText.width/2;
            _tip.addChild(tipText);
        }
    }

    protected function rollOutHandler (event :MouseEvent) :void
    {
        if (_tip is Sprite) {
            _tip.parent.removeChild(_tip);
            _tip = null;
        }
    }

    protected var _random :Random;

    protected var _loader :Loader;

    protected var _tip :Sprite;
    protected var _hood :Neighborhood;
    protected var _canvas :Sprite;
    protected var _bound :Rectangle = new Rectangle();

    protected var _house :Building;
    protected var _group :Building;
    protected var _game :Building;

    protected var _plaqueHouse :Class;
    protected var _plaqueGroup :Class;
    protected var _plaqueGame :Class;

    protected var _rule :Class;

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
