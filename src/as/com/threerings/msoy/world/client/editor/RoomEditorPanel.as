//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.Bitmap;
import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.events.IEventDispatcher;
import flash.events.Event;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.geom.Rectangle;

import mx.containers.Box;
import mx.containers.Grid;
import mx.containers.HBox;
import mx.containers.TabNavigator;
import mx.containers.VBox;
import mx.controls.ComboBox;
import mx.controls.HRule;
import mx.controls.Label;
import mx.controls.Spacer;
import mx.core.UIComponent;
import mx.events.ListEvent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandCheckBox;
import com.threerings.flex.GridUtil;
import com.threerings.util.CommandEvent;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.SkinnableImage;

import com.threerings.msoy.client.HeaderBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.TopPanel;

import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.client.WorldController;
import com.threerings.msoy.world.data.FurniData;


/**
 * A separate room editing panel, which lets the player edit furniture inside the room.
 */
public class RoomEditorPanel extends FloatingPanel
{
    public function RoomEditorPanel (ctx :WorldContext, controller :RoomEditorController)
    {
        super(ctx, Msgs.EDITING.get("t.editor_title"));
        _controller = controller;

        styleName = "roomEditPanel";
        showCloseButton = true;
    }

    // @Override from FloatingPanel
    override public function open (
        modal :Boolean = false, parent :DisplayObject = null, avoid :DisplayObject = null) :void
    {
        super.open(modal, parent, avoid);

        this.x = stage.stageWidth - width - GAP;
        this.y = HeaderBar.HEIGHT + TopPanel.DECORATIVE_MARGIN_HEIGHT + GAP;
    }

    // @Override from FloatingPanel
    override public function close () :void
    {
        super.close();
        _controller.actionEditorClosed();
        (_ctx as WorldContext).getGameDirector().tutorialEvent("editorClosed");
    }

    /** Updates object data displayed on the editing panel. */
    public function updateDisplay (target :FurniSprite) :void
    {
        var data :FurniData = (target != null) ? target.getFurniData() : null;
        _details.updateDisplay(data);
        _action.updateDisplay(data);
        _room.updateDisplay(data);

        _custom.updateDisplay(target);
    }

    /** Updates the enabled status of the undo button (based on the size of the undo stack). */
    public function updateUndoStatus (enabled :Boolean) :void
    {
        for each (var button :CommandButton in _undoButtons) {
            button.enabled = enabled;
        }
    }

    /** Updates the enabled status of any buttons that require a target furni to be selected. */
    public function updateTargetSelected (target :FurniSprite) :void
    {
        for each (var button :CommandButton in _deleteButtons) {
            button.enabled = (target != null && target.isRemovable());
        }

        for each (button in _actionButtons) {
            button.enabled = (target != null && target.isActionModifiable());
        }

        for each (button in _targetButtons) {
            button.enabled = (target != null);
        }
    }

    /** Updates the name drop-down box with the selected item definitions. */
    public function updateNameList (defs :Array) :void
    {
        _namebox.dataProvider = defs;
    }

    /** Selects the specified item in the name list. */
    public function selectInNameList (def :Object) :void
    {
        if (def == null) {
            _namebox.selectedIndex = -1;
        } else {
            _namebox.selectedItem = def;
        }
    }

    public function setHomeButtonEnabled (enabled :Boolean) :void
    {
        _room.setHomeButtonEnabled(enabled);
    }

    /** Add or remove advanced panels from display. */
    public function displayAdvancedPanels (show :Boolean) :void
    {
        var containsDefault :Boolean = _switchablePanels.contains(_defaultPanel);
        var containsAdvanced :Boolean = _switchablePanels.contains(_advancedPanels);

        if (show) {
            if (containsDefault) { _switchablePanels.removeChild(_defaultPanel); }
            if (! containsAdvanced) { _switchablePanels.addChild(_advancedPanels); }
        } else {
            if (! containsDefault) { _switchablePanels.addChild(_defaultPanel); }
            if (containsAdvanced) { _switchablePanels.removeChild(_advancedPanels); }
        }
    }

    /** Displays the furniture inventory. */
    protected function displayFurnitureInventory () :void
    {
        CommandEvent.dispatch(this.parent, WorldController.VIEW_MY_FURNITURE);
        selectInNameList(null);
    }
    
    /** Handler for dealing with changes in the name selection box. */
    protected function nameListChanged (event :ListEvent) :void
    {
        if (_namebox.selectedItem != null) {
            _controller.findAndSetTarget(_namebox.selectedItem.data);
        }
    }
        
    // from superclasses
    override protected function createChildren () :void
    {
        super.createChildren();

        var makeActionButton :Function = function (
            fn :Function, style :String, tooltip :String,
            buttonlist :Array, enabled :Boolean = true) :UIComponent
        {
            var c :VBox = new VBox();
            c.setStyle("horizontalAlign", "center");
            
            var b :CommandButton = new CommandButton(null, fn);
            b.styleName = style;
            b.toolTip = Msgs.EDITING.get(tooltip);
            b.enabled = enabled;
            if (buttonlist != null) {
                buttonlist.push(b);
            }
            c.addChild(b);

            var img :SkinnableImage = new SkinnableImage();
            img.styleName = style + "Text";
            c.addChild(img);
            
            return c;
        }

        var noop :Function = function (... ignore) :void { }

        _actionButtons = new Array();
        _deleteButtons = new Array();
        _undoButtons = new Array();
        _targetButtons = new Array();

        // container for room name
        var namebar :VBox = new VBox();
        namebar.styleName = "roomEditNameBar";
        namebar.percentWidth = 100;
        namebar.addChild(_room = new RoomPanel(_controller));
        addChild(namebar);

        // container for item stuffs
        
        var contents :Grid = new Grid();
        contents.styleName = "roomEditContents";
        contents.percentWidth = 100;
        addChild(contents);
        
        // item name combo box
        
        var box :Box = new HBox();
        box.styleName = "roomEditButtonBar";
        box.percentWidth = 100;
        GridUtil.addRow(contents, box, [3, 1]);
        
        _namebox = new ComboBox();
        _namebox.percentWidth = 100;
        _namebox.maxWidth = 300;
        _namebox.prompt = Msgs.EDITING.get("l.select_item");
        _namebox.toolTip = Msgs.EDITING.get("i.namebox_tip");
        _namebox.addEventListener(ListEvent.CHANGE, nameListChanged);
        box.addChild(_namebox);

        var spacer :VBox = new VBox();
        spacer.percentWidth = 100;
        spacer.height = 10;
        GridUtil.addRow(contents, spacer, [3, 1]);

        // two containers, one for basic/advanced buttons, the other for
        // buttons that show up in all contexts
        
        _switchablePanels = new VBox();

        var div :SkinnableImage = new SkinnableImage();
        div.styleName = "roomEditDiv";

        var right :VBox = new VBox();
        right.styleName = "roomEditRight";

        GridUtil.addRow(contents, _switchablePanels, div, right);
                
        _defaultPanel = new HBox();
        _switchablePanels.addChild(_defaultPanel);

        
        // now let's populate the basic buttons panel
        
        var leftgrid :Grid = new Grid();
        leftgrid.styleName = "roomEditLeft";
        leftgrid.percentWidth = 100;
        _defaultPanel.addChild(leftgrid);

        var makeYFn :Function = function (yoffset :Number) :Function {
            return function () :void { _controller.actionAdjustYPosition(yoffset); };
        };

        var makeScaleFn :Function = function (x :Number, y :Number) :Function {
            return function () :void { _controller.actionAdjustScale(x, y); };
        };
            
        var makeRotateFn :Function = function (rotation :Number) :Function {
            return function () :void { _controller.actionAdjustRotation(rotation); };
        };

        
        GridUtil.addRow(leftgrid,
                        makeActionButton(
                            makeScaleFn(1 / SCALEMULTI, 1 / SCALEMULTI),
                            "roomEditScaleDown", "b.scale_down", _targetButtons),
                        makeActionButton(
                            makeScaleFn(SCALEMULTI, SCALEMULTI),
                            "roomEditScaleUp", "b.scale_up", _targetButtons));
        GridUtil.addRow(leftgrid,
                        makeActionButton(
                            makeYFn(- Y_DELTA), "roomEditMoveDown", "b.move_down", _targetButtons),
                        makeActionButton(
                            makeYFn(Y_DELTA), "roomEditMoveUp", "b.move_up", _targetButtons));
        GridUtil.addRow(leftgrid,
                        makeActionButton(
                            makeRotateFn(- ROTATE_DELTA),
                            "roomEditRotateLeft", "b.rotate_left", _targetButtons),
                        makeActionButton(
                            makeRotateFn(ROTATE_DELTA),
                            "roomEditRotateRight", "b.rotate_right", _targetButtons));
        GridUtil.addRow(leftgrid,
                        makeActionButton(
                            makeScaleFn(-1, 1), "roomEditFlipH", "b.flip_h", _targetButtons),
                        makeActionButton(
                            makeScaleFn(1, -1), "roomEditFlipV", "b.flip_v", _targetButtons));

        // divider
        div = new SkinnableImage();
        div.styleName = "roomEditDiv";
        _defaultPanel.addChild(div);
        
        // item actions
        var middle :VBox = new VBox();
        middle.styleName = "roomEditRight";
        _defaultPanel.addChild(middle);

        middle.addChild(makeActionButton(_controller.actionDelete, "roomEditTrash",
                                         "b.put_away", _deleteButtons));
        middle.addChild(makeActionButton(noop, "roomEditDoor",
                                         "b.make_door", _actionButtons));
        middle.addChild(makeActionButton(noop, "roomEditLink",
                                         "b.make_link", _actionButtons));
        

        right.addChild(makeActionButton(displayFurnitureInventory, "roomEditAdd",
                                        "b.add_item", null));
        right.addChild(makeActionButton(_controller.actionUndo, "roomEditUndo",
                                        "b.undo", _undoButtons));
        right.addChild(makeActionButton(_controller.actionUndo, "roomEditUndoAll",
                                        "b.undo_all", null, false));

        updateTargetSelected(null); // disable most buttons

        
        // now populate advanced settings panel

        _advancedPanels = new VBox();
        _advancedPanels.styleName = "roomEditAdvanced";
        _advancedPanels.percentWidth = 100;
        _switchablePanels.addChild(_advancedPanels);

        var addPanel :Function = function (label :String, panel :UIComponent) :Array {
            var hr :HRule = new HRule();
            hr.percentWidth = 100;
            _advancedPanels.addChild(hr);

            var c :CollapsingContainer = new CollapsingContainer(label);
            c.setContents(panel);
            _advancedPanels.addChild(c);

            return [ hr, c ];
        }

        var c :Array = null;

        addPanel(Msgs.EDITING.get("t.item_prefs"), _details = new DetailsPanel(_controller));
        addPanel(Msgs.EDITING.get("t.item_action"), _action = new ActionPanel(_controller)); 
        c = addPanel(Msgs.EDITING.get("t.item_custom"), _custom = new CustomPanel());
        _custom.setHiders(c);
        
        // invader zim says: "it's not stupid, it's advanced!"

        box = new VBox();
        box.styleName = "roomEditContents";
        box.percentWidth = 100;
        addChild(box);

        spacer = new VBox();
        spacer.percentWidth = 100;
        spacer.height = 10;
        box.addChild(spacer);

        box.addChild(new CommandCheckBox(Msgs.EDITING.get("l.advanced_editing"),
                                         _controller.actionAdvancedEditing));

    }

    protected static const Y_DELTA :Number = 0.1;
    protected static const ROTATE_DELTA :Number = 45;
    protected static const SCALEMULTI :Number = 1.2;
        
    protected var _switchablePanels :Box;
    protected var _undoButtons :Array; // of CommandButton
    protected var _deleteButtons :Array; // of CommandButton
    protected var _actionButtons :Array; // of CommandButton
    protected var _targetButtons :Array; // of CommandButton
    
    protected var _details :DetailsPanel;
    protected var _action :ActionPanel;
    protected var _custom :CustomPanel;

    protected var _defaultPanel :Box;
    protected var _advancedPanels :Box;
    protected var _genericIcons :Box;
    
    protected var _room :RoomPanel;
    protected var _namebox :ComboBox;
    protected var _controller :RoomEditorController;

    protected static const GAP :int = 10;
}
}
