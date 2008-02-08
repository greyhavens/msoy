//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.events.Event;

import mx.containers.Grid;
import mx.containers.VBox;
import mx.controls.CheckBox;
import mx.controls.TextInput;
import mx.events.FlexEvent;

import com.threerings.flash.MathUtil;
import com.threerings.flex.CommandButton;
import com.threerings.flex.GridUtil;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyLocation;

/**
 * Displays details about the position, size, and other numeric values of the furni.
 */
public class DetailsPanel extends BasePanel
{
    public function DetailsPanel (controller :RoomEditorController)
    {
        super(controller);
    }

    // @Override from BasePanel
    override public function updateDisplay (data :FurniData) :void
    {
        super.updateDisplay(data);

        var trimmed :Function = function (v :Number) :String {
            return v.toFixed(2);
        };
        
        if (data == null) {
            _all.forEach(function (input :TextInput, ... rest) :void {
                    input.text = "0.0";
                });
        } else {
            _locx.text = trimmed(data.loc.x);
            _locy.text = trimmed(data.loc.y);
            _locz.text = trimmed(data.loc.z);
            _scalex.text = trimmed(data.scaleX);
            _scaley.text = trimmed(data.scaleY);
            _noscale.selected = data.isNoScale();
        }
    }

    // @Override from BasePanel
    override protected function getUserModifications () :FurniData
    {
        // no call to super - this is a replacement

        if (_furniData == null) {
            return null; // nothing to do!
        }

        var maybeReplace :Function = function (input :TextInput, previous :Number) :Number {
            var result :Number = Number(input.text);
            return (isNaN(result) ? previous : result);
        }

        // do a manual copy, remembering that FurniData.clone is a shallow copy!
        var data :FurniData = _furniData.clone() as FurniData;
        data.loc = _furniData.loc.clone() as MsoyLocation; 

        data.loc.x = maybeReplace(_locx, data.loc.x);
        data.loc.y = maybeReplace(_locy, data.loc.y);
        data.loc.z = maybeReplace(_locz, data.loc.z);
        data.scaleX = maybeReplace(_scalex, data.scaleX);
        data.scaleY = maybeReplace(_scaley, data.scaleY);
        data.setNoScale(_noscale.selected);

        if (! _furniData.equivalent(data)) {
            return data;
        } else {
            return null;
        }
    }

    // @Override from superclass
    override protected function createChildren () :void
    {
        super.createChildren();
        
        var grid :Grid = new Grid();
        grid.percentWidth = 100;
        grid.styleName = "roomEditDetailsPanelGrid";
        addChild(grid);

        _locs = new Array();
        _scales = new Array();
        _locs.push(_locx = new TextInput());
        _locs.push(_locy = new TextInput());
        _locs.push(_locz = new TextInput());
        _scales.push(_scalex = new TextInput());
        _scales.push(_scaley = new TextInput());
        _all = _locs.concat(_scales);        

        _all.forEach(function (input :TextInput, ... rest) :void {
                input.text = "0.0";
                input.restrict = "0123456789,.+\\-";
                input.width = 40;
            });

        GridUtil.addRow(grid, Msgs.EDITING.get("l.location"), [6, 1]);
        GridUtil.addRow(grid,
                        Msgs.EDITING.get("l.axis_x"), _locx,
                        Msgs.EDITING.get("l.axis_y"), _locy,
                        Msgs.EDITING.get("l.axis_z"), _locz);
        GridUtil.addRow(grid, Msgs.EDITING.get("l.scale"), [6, 1]);
        GridUtil.addRow(grid,
                        Msgs.EDITING.get("l.scale_x"), _scalex,
                        Msgs.EDITING.get("l.scale_y"), _scaley);
        GridUtil.addRow(grid, "", [6, 1]);

        // new grid for special buttons
        
        grid = new Grid();
        grid.percentWidth = 100;
        addChild(grid);

        _noscale = new CheckBox();
        _noscale.label = Msgs.EDITING.get("b.noscale");
        _noscale.toolTip = Msgs.EDITING.get("b.noscale_tip");
        _noscale.addEventListener(Event.CHANGE, applyHandler);

        GridUtil.addRow(grid,
                        new CommandButton(Msgs.EDITING.get("b.reset_location"),
                                          _controller.resetTarget, [ true, false ]),
                        new CommandButton(Msgs.EDITING.get("b.reset_scale"),
                                          _controller.resetTarget, [ false, true ]));
        GridUtil.addRow(grid, _noscale, [2, 1]);
        
        addChild(makePanelButtons());
    }

    // @Override from superclass
    override protected function childrenCreated () :void
    {
        super.childrenCreated();

        _all.forEach(function (input :TextInput, ... rest) :void {
                input.addEventListener(Event.CHANGE, changedHandler);
                input.addEventListener(FlexEvent.ENTER, applyHandler);
            });
    }

    protected var _locx :TextInput;
    protected var _locy :TextInput;
    protected var _locz :TextInput;
    protected var _scalex :TextInput;
    protected var _scaley :TextInput;
    protected var _noscale :CheckBox;
    protected var _locs :Array;
    protected var _scales :Array;
    protected var _all :Array;
}

}
