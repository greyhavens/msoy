//
// $Id$

package com.threerings.msoy.avrg.client {

import flash.events.Event;
import flash.events.MouseEvent;
import flash.display.DisplayObject;
import flash.geom.Point;

import mx.containers.Grid;
import mx.containers.GridRow;
import mx.containers.HBox;
import mx.controls.ComboBox;
import mx.controls.Label;
import mx.controls.TextInput;
import mx.core.ScrollPolicy;

import com.threerings.presents.dobj.SetListener;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.crowd.client.LocationObserver;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.spot.data.SpotSceneObject;
import com.threerings.whirled.spot.data.SceneLocation;

import com.threerings.flex.GridUtil;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.room.client.RoomView;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.RoomObject;

/**
 * Floating panel showing some useful information for debugging and understanding the various msoy
 * coordinate systems and transformations amongst them.
 */
public class CoordinateDebugPanel extends FloatingPanel
    implements LocationObserver, SetListener
{
    /**
     * Shows a new panel.
     */
    public static function show (ctx :MsoyContext, avrgPanel :AVRGamePanel) :void
    {
        var panel :CoordinateDebugPanel = new CoordinateDebugPanel(ctx, avrgPanel, "default");
        panel.open();
    }

    /**
     * Creates a new panel.
     */
    public function CoordinateDebugPanel (ctx :MsoyContext, avrgPanel :AVRGamePanel, mode :String)
    {
        super(ctx);

        _mode = mode == "default" ? "avatar" : mode;
        _view = ctx.getPlaceView() as RoomView;
        _avrgPanel = avrgPanel;
        _avrgPanel.addEventListener(Event.REMOVED, onAVRGPanelRemoved);

        _ctx.getTopPanel().addEventListener(MouseEvent.MOUSE_MOVE, mouseMove);
        _ctx.getLocationDirector().addLocationObserver(this);
        locationDidChange(_ctx.getLocationDirector().getPlaceObject());
    }

    override public function close () :void
    {
        super.close();
        locationDidChange(null);
        _ctx.getLocationDirector().removeLocationObserver(this);
        _ctx.getTopPanel().removeEventListener(MouseEvent.MOUSE_MOVE, mouseMove);
    }

    // from LocationObserver
    public function locationMayChange (placeId :int) :Boolean
    {
        return true;
    }

    // from LocationObserver
    public function locationDidChange (place :PlaceObject) :void
    {
        if (_room != null) {
            _room.removeListener(this);
        }
        _room = place as RoomObject;
        if (_room != null) {
            _room.addListener(this);
        }
    }

    // from LocationObserver
    public function locationChangeFailed (placeId :int, reason :String) :void
    {
    }

    // from SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        if (event.getName() == SpotSceneObject.OCCUPANT_LOCS) {
            updateLocation(event.getEntry() as SceneLocation);
        }
    }

    // from SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        if (event.getName() == SpotSceneObject.OCCUPANT_LOCS) {
            updateLocation(event.getEntry() as SceneLocation);
        }
    }

    // from SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var title :Label = new Label();
        title.text = "Coordinates";
        addChild(title);

        if (_mode == "mouse") {
            var hbox :HBox = new HBox();
            _planes = new ComboBox();
            _planes.dataProvider = ["Height", "Depth"];
            _planes.selectedIndex = 0;
            hbox.addChild(_planes);

            _planeCoord = new TextInput();
            _planeCoord.text = "0.5";
            hbox.addChild(_planeCoord);
            addChild(hbox);
        }

        var grid :Grid = new Grid();
        grid.maxHeight = 400;
        grid.horizontalScrollPolicy = ScrollPolicy.OFF;
        grid.verticalScrollPolicy = ScrollPolicy.AUTO;
        addChild(grid);

        if (_mode == "mouse") {
            addResultLine(grid, "target");
            addResultLine(grid, "curTarget");
            addResultLine(grid, "local");
            addResultLine(grid, "global");
            addResultLine(grid, "view.local");
            addResultLine(grid, "avrg.local");
            addResultLine(grid, "room");
            addResultLine(grid, "plane");
            addResultLine(grid, "location");

        } else if (_mode == "avatar") {
            addResultLine(grid, "avatar");
            addResultLine(grid, "room");
            //addResultLine(grid, "panel");
            //addResultLine(grid, "panel.parent");
            //addResultLine(grid, "view");
            //addResultLine(grid, "view.parent");
            addResultLine(grid, "paintable");
        }

        showCloseButton = true;
    }

    /**
     * When creating children, adds in a row on the grid that can later be set by name.
     */
    protected function addResultLine (grid :Grid, name :String) :void
    {
        var row :GridRow = new GridRow();
        grid.addChild(row);

        var col1 :Label = new Label();
        col1.text = name;
        GridUtil.addToRow(row, col1);

        var col2 :Label = new Label();
        col2.width = 300;
        GridUtil.addToRow(row, col2);
        _results[name] = col2;
    }

    protected function onAVRGPanelRemoved (evt: Event) :void
    {
        close();
    }

    protected function updateLocation (loc :SceneLocation) :void
    {
        if (_mode != "avatar") {
            return;
        }

        if (loc.bodyOid != _ctx.getClient().getClientObject().getOid()) {
            return;
        }

        var mloc :MsoyLocation = loc.loc as MsoyLocation;
        var p :Point;
        setResultLocation("avatar", mloc);
        setResultPoint("room", p = locationToRoom(mloc.x, mloc.y, mloc.z));
        setResultText("panel", transSummary(_avrgPanel));
        setResultText("view", "scroll=" + fixed(_view.getScrollOffset()) + ", " +
                      transSummary(_view));
        setResultText("view.parent", transSummary(_view.parent));
        setResultText("panel.parent", transSummary(_avrgPanel.parent));
        setResultPoint("paintable", p = roomToStage(p));
    }

    protected function mouseMove (evt: MouseEvent) :void
    {
        if (_mode != "mouse") {
            return;
        }

        setResultText("target", evt.target.toString());
        setResultText("curTarget", evt.currentTarget.toString());

        var pt :Point;
        var pt2 :Point;
        var loc :MsoyLocation;
        setResultPoint("local", pt = new Point(evt.localX, evt.localY));
        setResultPoint("global", pt = evt.target.localToGlobal(pt));
        setResultPoint("view.local", pt2 = _view.globalToLocal(pt));
        setResultPoint("avrg.local", pt2 = _avrgPanel.globalToLocal(pt));
        setResultPoint("room", stageToRoom(pt2));
        setResultLocation("location", loc = stageToLocation(pt2));
    }

    /**
     * Sets the line on our grid with the given name to show the given point.
     */
    protected function setResultPoint (name :String, pt :Point) :void
    {
        if (pt == null) {
            setResultText(name, "null");
        } else {
            setResultText(name, "" + fixed(pt.x) + ", " + fixed(pt.y));
        }
    }

    /**
     * Sets the line on our grid with the given name to show the given location.
     */
    protected function setResultLocation (name :String, loc :MsoyLocation) :void
    {
        if (loc == null) {
            setResultText(name, "null");
        } else {
            setResultText(name, "" + fixed(loc.x) + ", " + fixed(loc.y) + ", " + fixed(loc.z));
        }
    }

    /**
     * Sets the line on our grid with the given name to show the given text.
     */
    protected function setResultText (name :String, str :String) :void
    {
        var result :Label = Label(_results[name]);
        if (result == null) {
            return;
        }
        result.text = str;
    }

    // copied from AVRGameBackend
    protected function stageToRoom (p :Point) :Point
    {
        p = _ctx.getTopPanel().getPlaceContainer().localToGlobal(p);
        p = _view.globalToLocal(p);
        return p;
    }

    // copied from AVRGameBackend
    protected function stageToLocation (p :Point) :MsoyLocation
    {
        var msoyLoc :MsoyLocation = null;
        var plane :int = _planes.selectedIndex;
        var coord :Number = Number(_planeCoord.text);
        if (plane == 0) { // height
            setResultText("plane", "height " + coord);
            msoyLoc = _view.layout.pointToLocationAtHeight(p.x, p.y, height);
        } else if (plane == 1) { // depth
            setResultText("plane", "depth " + coord);
            msoyLoc = _view.layout.pointToLocationAtDepth(p.x, p.y, height);
        }
        return msoyLoc;
    }

    // copied from AVRGameBackend
    protected function locationToRoom (x :Number, y :Number, z :Number) :Point
    {
        return _view.layout.locationToPoint(new MsoyLocation(x, y, z));
    }

    // copied from AVRGameBackend
    protected function roomToStage (p :Point) :Point
    {
        return _ctx.getTopPanel().getPlaceContainer().globalToLocal(
            _view.localToGlobal(p));
    }

    protected static function transSummary (dsp :DisplayObject) :String
    {
        return "scale=" + fixed(dsp.scaleX) + ", " + fixed(dsp.scaleY) +
               ", pos=" + fixed(dsp.x) + ", " + fixed(dsp.y);
    }

    protected static function fixed (n :Number) :String
    {
        return n.toFixed(2);
    }

    protected var _view :RoomView;
    protected var _avrgPanel :AVRGamePanel;
    protected var _mode :String;
    protected var _results :Object = {};
    protected var _planes :ComboBox;
    protected var _planeCoord :TextInput;
    protected var _room :RoomObject;
}
}
