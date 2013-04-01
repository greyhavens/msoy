//
// $Id$

package com.threerings.msoy.room.client.editor {

import flash.events.Event;
import flash.geom.Rectangle;

import mx.containers.Box;
import mx.containers.Grid;
import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.ColorPicker;
import mx.controls.HRule;
import mx.controls.Label;
import mx.controls.Text;
import mx.core.Container;
import mx.core.UIComponent;
import mx.events.ColorPickerEvent;

import com.threerings.util.CommandEvent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandCheckBox;
import com.threerings.flex.CommandComboBox;
import com.threerings.flex.FlexUtil;
import com.threerings.flex.GridUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.room.client.FurniSprite;
import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.ui.FlyingPanel;
import com.threerings.msoy.ui.SkinnableImage;
import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.client.WorldController;

/**
 * A separate room editing panel, which lets the player edit furniture inside the room.
 */
public class RoomEditorPanel extends FlyingPanel
{
    public function RoomEditorPanel (ctx :WorldContext, controller :RoomEditorController)
    {
        super(ctx, Msgs.EDITING.get("t.editor"));
        _wctx = ctx;
        _controller = controller;

        styleName = "roomEditPanel";
        showCloseButton = true;
        setButtonWidth(0); // free-width
    }

    // @Override from FloatingPanel
    override public function close () :void
    {
        super.close();
        _controller.actionEditorClosed();
    }

    /** Updates object data displayed on the editing panel. */
    public function updateDisplay (target :FurniSprite) :void
    {
        var data :FurniData = (target != null) ? target.getFurniData() : null;
        _details.updateDisplay(data);
        _action.updateDisplay(data);
        _room.updateDisplay(data);
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
        var selected :Boolean = (target != null);

        for each (var button :CommandButton in _deleteButtons) {
            button.enabled = (selected && target.isRemovable());
        }

        for each (button in _actionButtons) {
            button.enabled = (selected && target.isActionModifiable());
        }

        for each (button in _targetButtons) {
            button.enabled = (selected);
        }

        if (_middle != null) {
            // display the correct link button
            if (selected && target.getFurniData().actionType == FurniData.ACTION_URL) {
                swapButtons(_middle, _makeLinkButton, _removeLinkButton);
            } else {
                swapButtons(_middle, _removeLinkButton, _makeLinkButton);
            }
            // same for the door button
            if (selected && target.getFurniData().actionType == FurniData.ACTION_PORTAL) {
                swapButtons(_middle, _makeDoorButton, _removeDoorButton);
            } else {
                swapButtons(_middle, _removeDoorButton, _makeDoorButton);
            }
        }

        // For the custom config panel stuff, we need to avoid repeatedly watching
        // the same sprite
        if (target != _curTarget) {
            if (_curTarget != null) {
                _curTarget.viz.removeEventListener(Event.INIT, handleTargetInit);
            }
            _curTarget = target;
            checkCustomPanel();
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

    public function setDecor (decor :Decor) :void
    {
        _decorLabel.text = Msgs.EDITING.get("m.decor", decor.name || Msgs.EDITING.get("m.none"));
    }

    public function updatePlaylistControl (playlistControl :int) :void
    {
        _playlistControl.selected = (playlistControl == MsoySceneModel.ACCESS_EVERYONE);
    }

    public function setBackgroundColor (color :uint) :void
    {
        _backgroundColor.selectedColor = color;
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

    /**
     * Sets whether or not we should show the puppet control check box. Note that this is intended
     * for use during initialization of editing mode and should only be called once prior to
     * opening the panel.
     */
    public function setMayHavePuppet (visible :Boolean) :void
    {
        // remove the control so layout looks right
        if (!visible && _removePuppetControl != null) {
            _removePuppetControl();
            _removePuppetControl = null;
        }
    }

    /**
     * Sets whether or not the puppet control check box is selected.
     */
    public function setPuppetEnabled (enabled :Boolean) :void
    {
        _puppetControl.selected = enabled;
    }

    /**
     * Gets the button which, when pressed, will start the door creation wizard.
     */
    public function getMakeDoorButton () :UIComponent
    {
        return _makeDoorButton;
    }

    /**
     * Displays a modal box to enter the url. When the user clicks OK,
     * it will set the currently selected item to link at the given url.
     */
    protected function makeUrl () :void
    {
        var dialog :UrlDialog = new UrlDialog(_controller.ctx,
            function (url :String, tip :String) :void {
                _controller.actionTargetLink(url, tip);
                swapButtons(_middle, _makeLinkButton, _removeLinkButton);
            });
    }

    /** Removes a link action from the currently selected furni. */
    protected function removeUrl () :void
    {
        _controller.actionTargetClear();
        swapButtons(_middle, _removeLinkButton, _makeLinkButton);
    }

    /** Starts the door creation wizard. */
    protected function makeDoor () :void
    {
        swapButtons(_middle, _makeDoorButton, _removeDoorButton);
        _controller.actionTargetDoor();
    }

    /** Removes a door action from the currently selected furni. */
    protected function removeDoor () :void
    {
        _controller.actionTargetClear();
        swapButtons(_middle, _removeDoorButton, _makeDoorButton);
    }

    /** Shows the custom config panel. */
    protected function showCustomConfig () :void
    {
        _controller.roomView.getRoomController().showConfigPopup(_curTarget);
    }

    /** Swaps two UI components in a container. */
    protected function swapButtons (
        container :Container, oldButton :UIComponent, newButton :UIComponent) :void
    {
        try {
            var index :int = _middle.getChildIndex(oldButton);
            if (index >= 0) {
                _middle.removeChildAt(index);
                _middle.addChildAt(newButton, index);
            }
        } catch (ae :ArgumentError) {
            // the old button hasn't been added - ignore...
        }
    }

    /** Displays the furniture inventory. */
    protected function displayFurnitureInventory () :void
    {
        CommandEvent.dispatch(this, WorldController.VIEW_STUFF, Item.FURNITURE);
        selectInNameList(null);
    }

    /** See if we should display the custom panel button for the specified furni. */
    protected function checkCustomPanel () :void
    {
        var hasPanel :Boolean = false;
        if (_curTarget != null) {
            hasPanel = _curTarget.hasCustomConfigPanel();
            if (!hasPanel && !_curTarget.viz.isContentInitialized()) {
                _curTarget.viz.addEventListener(Event.INIT, handleTargetInit);
            }
        }

        // assume we won't show it
        _customConfigButton.visible = hasPanel;
    }

    /** The current target is now initialized. */
    protected function handleTargetInit (event :Event) :void
    {
        _curTarget.viz.removeEventListener(Event.INIT, handleTargetInit);
        checkCustomPanel();
    }

    // from superclasses
    override protected function createChildren () :void
    {
        super.createChildren();

        var makeActionButton :Function = function (
            fn :Function, style :String, translationBase :String,
            buttonlist :Array, makeLabel :Boolean = false) :UIComponent
        {
            var c :VBox = new VBox();
            c.setStyle("horizontalAlign", "center");

            var b :CommandButton = new CommandButton(null, fn);
            b.styleName = style;
            b.toolTip = Msgs.EDITING.get("i." + translationBase);
            if (buttonlist != null) {
                buttonlist.push(b);
            }
            c.addChild(b);

            if (makeLabel) {
                var l :Text = new Text();
                l.selectable = false;
                l.styleName = "roomEditButtonLabel";
                l.text = Msgs.EDITING.get("l." + translationBase);
                c.addChild(l);
            }

            return c;
        }

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

        // playlist control
        _playlistControl = new CommandCheckBox(Msgs.EDITING.get("l.playlist"),
            _controller.setPlaylistControl);
        // GridUtil.addRow(contents, _playlistControl, [ 3, 1 ]);

        // puppet control
        var puppetControlRow :UIComponent = GridUtil.addRow(contents,
            _puppetControl = new CommandCheckBox(Msgs.EDITING.get("l.show_puppet"),
                _controller.setPuppetEnabled), [ 3, 1 ]);
        _removePuppetControl = function () :void {
            contents.removeChild(puppetControlRow);
        };

        // decor name
        var decorBox :Box = new HBox();
        decorBox.percentWidth = 100;
        decorBox.addChild(_decorLabel = FlexUtil.createLabel(""));
        _decorLabel.width = 200;
        decorBox.addChild(new CommandButton(
            Msgs.EDITING.get("b.change"), WorldController.VIEW_STUFF, Item.DECOR));
        GridUtil.addRow(contents, decorBox, [3, 1]);

        // background color
        var colorBox :Box = new HBox();
        colorBox.styleName = "roomEditBackgroundColorRow";
        colorBox.percentWidth = 100;
        var colorLabel :Label = FlexUtil.createLabel("Background color");
        colorBox.addChild(colorLabel);
        colorLabel.width = 200;
        _backgroundColor = new ColorPicker();
        colorBox.addChild(_backgroundColor);
        _backgroundColor.addEventListener(ColorPickerEvent.CHANGE,
            function (evt :ColorPickerEvent) :void {
                _controller.updateBackgroundColor(evt.color);
            });
        GridUtil.addRow(contents, colorBox, [3, 1]);

        // item name combo box
        var box :Box = new HBox();
        box.styleName = "roomEditButtonBar";
        box.percentWidth = 100;
        GridUtil.addRow(contents, box, [3, 1]);

        _namebox = new CommandComboBox(_controller.findAndSetTarget);
        _namebox.percentWidth = 100;
        _namebox.maxWidth = 300;
        _namebox.prompt = Msgs.EDITING.get("l.select_item");
        _namebox.toolTip = Msgs.EDITING.get("i.namebox");
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
        var makeRotateFn :Function = function (rotation :Number, snapangle :Number) :Function {
            return function () :void {
                _controller.actionAdjustRotation(rotation, true, snapangle);
            };
        };

        GridUtil.addRow(leftgrid,
                        makeActionButton(
                            makeScaleFn(1 / SCALEMULTI, 1 / SCALEMULTI),
                            "roomEditScaleDown", "scale_down", _targetButtons),
                        makeActionButton(
                            makeScaleFn(SCALEMULTI, SCALEMULTI),
                            "roomEditScaleUp", "scale_up", _targetButtons));
        GridUtil.addRow(leftgrid,
                        makeActionButton(
                            makeYFn(- Y_DELTA), "roomEditMoveDown", "move_down", _targetButtons),
                        makeActionButton(
                            makeYFn(Y_DELTA), "roomEditMoveUp", "move_up", _targetButtons));
        GridUtil.addRow(leftgrid,
                        makeActionButton(
                            makeRotateFn(- ROTATE_DELTA, ROTATE_DELTA),
                            "roomEditRotateLeft", "rotate_left", _targetButtons),
                        makeActionButton(
                            makeRotateFn(ROTATE_DELTA, ROTATE_DELTA),
                            "roomEditRotateRight", "rotate_right", _targetButtons));
        GridUtil.addRow(leftgrid,
                        makeActionButton(
                            makeScaleFn(-1, 1), "roomEditFlipH", "flip_h", _targetButtons),
                        makeActionButton(
                            makeScaleFn(1, -1), "roomEditFlipV", "flip_v", _targetButtons));

        // divider
        div = new SkinnableImage();
        div.styleName = "roomEditDiv";
        _defaultPanel.addChild(div);

        // item actions
        _middle = new VBox();
        _middle.styleName = "roomEditRight";
        _defaultPanel.addChild(_middle);

        _middle.addChild(
            makeActionButton(_controller.actionDelete, "roomEditTrash",
                             "put_away", _deleteButtons, true));
        _middle.addChild(
            _makeDoorButton = makeActionButton(makeDoor, "roomEditDoor",
                                               "make_door", _actionButtons, true));
        _middle.addChild(
            _makeLinkButton = makeActionButton(makeUrl, "roomEditLink",
                                               "make_link", _actionButtons, true));

        _removeDoorButton = makeActionButton(
            removeDoor, "roomRemoveDoor", "remove_door", _actionButtons, true);
        _removeLinkButton = makeActionButton(
            removeUrl, "roomRemoveLink", "remove_link", _actionButtons, true);

        // generic actions
        right.addChild(makeActionButton(displayFurnitureInventory, "roomEditAdd",
                                        "add_item", null, true));
        right.addChild(makeActionButton(_controller.actionUndo, "roomEditUndo",
                                        "undo", _undoButtons, true));

        // commented out for beta - rz, 3/19/08
        //   the "undo all" functionality requires some way to consolidate all undo updates
        //   into one, so that we don't flood the server with a ton of messages
        // var undoall :UIComponent = makeActionButton(
        //    _controller.actionUndo, "roomEditUndoAll", "undo_all", null, true);
        // undoall.enabled = false;
        // right.addChild(undoall);

        // Instead, we're putting a custom config button in its place
        _customConfigButton = makeActionButton(
            showCustomConfig, "roomEditCustom", "item_custom", null, true);
        _customConfigButton.visible = false;
        right.addChild(_customConfigButton);

        // TEMP: adjust the size, since we have no icon
        var b :CommandButton = _customConfigButton.getChildAt(0) as CommandButton;
        b.width = 42;
        b.height = 42;
        // END: temp

        updateTargetSelected(null); // disable most buttons

        // now populate advanced settings panel

        _advancedPanels = new VBox();
        _advancedPanels.styleName = "roomEditAdvanced";
        _advancedPanels.percentWidth = 100;
        _switchablePanels.addChild(_advancedPanels);

        var addPanel :Function = function (label :String, panel :UIComponent) :void {
            var hr :HRule = new HRule();
            hr.percentWidth = 100;
            _advancedPanels.addChild(hr);

            var c :CollapsingContainer = new CollapsingContainer(label);
            c.setContents(panel);
            _advancedPanels.addChild(c);
        }

        addPanel(Msgs.EDITING.get("t.item_prefs"), _details = new DetailsPanel(_controller));
        addPanel(Msgs.EDITING.get("t.item_action"), _action = new ActionPanel(_controller));

        // invader zim says: "it's not stupid, it's advanced!"
        box = new VBox();
        box.styleName = "roomEditContents";
        box.percentWidth = 100;
        addChild(box);

        spacer = new VBox();
        spacer.percentWidth = 100;
        spacer.height = 10;
        box.addChild(spacer);

        box.addChild(new CommandCheckBox(
            Msgs.EDITING.get("l.advanced_editing"), _controller.actionAdvancedEditing));

        addButtons(OK_BUTTON, DONE_BUTTON);
        showCloseButton = true;

        // we have no border so we need to manually give our buttons some breathing room
        _buttonBar.setStyle("paddingRight", 5);
        _buttonBar.setStyle("paddingBottom", 5);
    }

    override protected function getButtonLabel (buttonId :int) :String
    {
        switch (buttonId) {
        case OK_BUTTON: return Msgs.EDITING.get("b.publish");
        case DONE_BUTTON: return Msgs.EDITING.get("b.end_editing");
        default: return super.getButtonLabel(buttonId);
        }
    }

    override protected function buttonClicked (id :int) :void
    {
        if (id == DONE_BUTTON) {
            close();
        } else {
            super.buttonClicked(id);
        }
    }

    override protected function okButtonClicked () :void
    {
        new PublishPanel(_wctx, _controller.roomView);
    }

    // @Override from FloatingPanel
    override protected function didOpen () :void
    {
        var r :Rectangle = _ctx.getTopPanel().getPlaceViewBounds();
        this.x = r.right - width - PADDING;
        this.y = r.y + PADDING;

        super.didOpen();
    }

    protected static const Y_DELTA :Number = 0.1;
    protected static const ROTATE_DELTA :Number = 45;
    protected static const SCALEMULTI :Number = 1.2;

    protected static const DONE_BUTTON :int = 1;

    protected var _wctx :WorldContext;

    protected var _switchablePanels :Box;
    protected var _undoButtons :Array; // of CommandButton
    protected var _deleteButtons :Array; // of CommandButton
    protected var _actionButtons :Array; // of CommandButton
    protected var _targetButtons :Array; // of CommandButton

    protected var _makeDoorButton :UIComponent;
    protected var _removeDoorButton :UIComponent;
    protected var _makeLinkButton :UIComponent;
    protected var _removeLinkButton :UIComponent;
    protected var _customConfigButton :UIComponent;

    protected var _playlistControl :CommandCheckBox;

    protected var _removePuppetControl :Function;
    protected var _puppetControl :CommandCheckBox;

    protected var _decorLabel :Label;

    protected var _backgroundColor :ColorPicker;

    protected var _curTarget :FurniSprite;

    protected var _details :DetailsPanel;
    protected var _action :ActionPanel;
    protected var _middle :VBox;

    protected var _defaultPanel :Box;
    protected var _advancedPanels :Box;
    protected var _genericIcons :Box;

    protected var _room :RoomPanel;
    protected var _namebox :CommandComboBox;
    protected var _controller :RoomEditorController;
}
}
