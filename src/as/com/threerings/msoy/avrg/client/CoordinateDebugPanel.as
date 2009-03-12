//
// $Id$

package com.threerings.msoy.avrg.client {

import flash.events.Event;
import flash.events.MouseEvent;
import flash.geom.Point;

import mx.containers.Grid;
import mx.containers.GridRow;
import mx.containers.HBox;
import mx.controls.ComboBox;
import mx.controls.Label;
import mx.controls.TextInput;
import mx.core.ScrollPolicy;

import com.threerings.flex.GridUtil;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.room.client.RoomView;
import com.threerings.msoy.room.data.MsoyLocation;

/**
 * Floating panel showing some useful information for debugging and understanding the various msoy
 * coordinate systems and transformations amongst them.
 */
public class CoordinateDebugPanel extends FloatingPanel
{
    /**
     * Shows a new panel.
     */
    public static function show (ctx :MsoyContext, avrgPanel :AVRGamePanel) :void
    {
        var panel :CoordinateDebugPanel = new CoordinateDebugPanel(ctx, avrgPanel);
        panel.open();
    }

    /**
     * Creates a new panel.
     */
    public function CoordinateDebugPanel (ctx :MsoyContext, avrgPanel :AVRGamePanel)
    {
        super(ctx);
        _view = ctx.getPlaceView() as RoomView;
        _avrgPanel = avrgPanel;
        _ctx.getTopPanel().addEventListener(MouseEvent.MOUSE_MOVE, onMouseMove);
        _avrgPanel.addEventListener(Event.REMOVED, onAVRGPanelRemoved);
    }

    override public function close () :void
    {
        super.close();
        _ctx.getTopPanel().removeEventListener(MouseEvent.MOUSE_MOVE, onMouseMove);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var title :Label = new Label();
        title.text = "Coordinates";
        addChild(title);

        var hbox :HBox = new HBox();
        _planes = new ComboBox();
        _planes.dataProvider = ["Height", "Depth"];
        _planes.selectedIndex = 0;
        hbox.addChild(_planes);

        _planeCoord = new TextInput();
        _planeCoord.text = "0.5";
        hbox.addChild(_planeCoord);
        addChild(hbox);

        var grid :Grid = new Grid();
        grid.maxHeight = 400;
        grid.horizontalScrollPolicy = ScrollPolicy.OFF;
        grid.verticalScrollPolicy = ScrollPolicy.AUTO;
        addChild(grid);

        addResultLine(grid, "target");
        addResultLine(grid, "curTarget");
        addResultLine(grid, "local");
        addResultLine(grid, "global");
        addResultLine(grid, "view.local");
        addResultLine(grid, "avrg.local");
        addResultLine(grid, "room");
        addResultLine(grid, "plane");
        addResultLine(grid, "location");

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

    protected function onMouseMove (evt: MouseEvent) :void
    {
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
            setResultText(name, "" + pt.x + ", " + pt.y);
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
            setResultText(name, "" + loc.x + ", " + loc.y + ", " + loc.z);
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
        p.x -= _view.getScrollOffset();
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

    protected var _view :RoomView;
    protected var _avrgPanel :AVRGamePanel;
    protected var _results :Object = {};
    protected var _planes :ComboBox;
    protected var _planeCoord :TextInput;
}
}
