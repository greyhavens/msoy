//
// $Id$

package com.threerings.msoy.room.client.editor {

import flash.events.Event;
import flash.events.MouseEvent;

import mx.binding.utils.BindingUtils;
import mx.containers.Grid;
import mx.containers.VBox;
import mx.containers.ViewStack;
import mx.controls.CheckBox;
import mx.controls.ComboBox;
import mx.controls.TextInput;
import mx.core.UIComponent;
import mx.events.FlexEvent;
import mx.events.ListEvent;

import com.threerings.util.StringUtil;

import com.threerings.flex.CommandButton;
import com.threerings.flex.GridUtil;
import com.threerings.flex.FlexUtil;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.room.data.FurniData;

/**
 * Displays furni action details as a sub-panel.
 */
public class ActionPanel extends BasePanel
{
    public function ActionPanel (controller :RoomEditorController)
    {
        super(controller);
    }

    // @Override from BasePanel
    override public function updateDisplay (data :FurniData) :void
    {
        super.updateDisplay(data);

        if (data == null || data is EntranceFurniData) {
            _readOnlyActionLabel.text = "";
            _actionTypeSelection.selectedIndex = 0;
            // also, make sure we disable the panel (the parent already does this for null data,
            // but this subclass also cares about EntranceFurniData instances!)
            this.enabled = false;            
        } else {
            // abandon previous edits

            var def :Object = getActionDef(_furniData.actionType);

            // can this action type be edited by the player?
            var editable :Boolean = isActionTypeEditable(_furniData.actionType);
            if (editable) {
                // select the right drop down entry based on the action type
                _actionTypeSelection.selectedIndex = getActionIndex(_furniData.actionType);
                updateTypePanels(def);
            } else {
                _readOnlyActionLabel.text = def.label;
                _actionTypeSelection.selectedIndex = 0;
            }

            FlexUtil.setVisible(_actionPanels, editable);
            FlexUtil.setVisible(_actionTypeSelection, editable);
            FlexUtil.setVisible(_readOnlyActionLabel, !editable);
        }
    }

    // @Override from superclass
    override protected function createChildren () :void
    {
        super.createChildren();

        var playerIsSupportPlus :Boolean = _controller.ctx.getTokens().isSupport();

        // generate combo box definitions, including only those actions whose editable flag is set,
        // and which are available for the player's account level
        var entries :Array = new Array();
        for each (var def :Object in ACTIONS) {
            // is it editable in the first place?
            if (isActionTypeEditable(def.data)) {
                // make sure the action is either available to everyone, or if it's support+ only,
                // that the player has the credentials.
                if (isActionTypeForAllPlayers(def.data) || playerIsSupportPlus) {
                    entries.push(def);
                }
            }
        }

        // create ui bits
        var grid :Grid = new Grid();
        addChild(grid);

        // this combo box will let the user pick a type
        _actionTypeSelection = new ComboBox();
        _actionTypeSelection.dataProvider = entries;
        _actionTypeSelection.addEventListener(ListEvent.CHANGE, applyHandler);

        // and this will be displayed instead of the drop-down box if the user can't edit it
        _readOnlyActionLabel = new TextInput();
        _readOnlyActionLabel.editable = false;
        _readOnlyActionLabel.enabled = false;
        _readOnlyActionLabel.width = 120; // not too big
        // hide this one initially
        FlexUtil.setVisible(_readOnlyActionLabel, false);

        var action :VBox = new VBox();
        action.addChild(_readOnlyActionLabel);
        action.addChild(_actionTypeSelection);

        // make editing panels for each action type
        GridUtil.addRow(grid, Msgs.EDITING.get("l.action"), action);
        _actionPanels = new ViewStack();
        _actionPanels.resizeToContent = true;
        for each (var entry :Object in entries) {
            if (entry.panelCreateFn != null) {
                _actionPanels.addChild((entry.panelCreateFn as Function)());
            } else {
                _actionPanels.addChild(new VBox());
            }
        }
        addChild(_actionPanels);

        // this label is for support+
        _debug = new TextInput();
        _debug.percentWidth = 100;
        _debug.maxWidth = 250;
        _debug.editable = false;
        _debug.enabled = false;
        if (playerIsSupportPlus) {
            var dgrid :Grid = new Grid();
            dgrid.setStyle("color", 0xff0000);
            addChild(dgrid);
            GridUtil.addRow(dgrid, Msgs.EDITING.get("l.action_debug"));
            GridUtil.addRow(dgrid, _debug);
        }

        addChild(makePanelButtons());
    }

    // @Override from superclass
    override protected function childrenCreated () :void
    {
        super.childrenCreated();

        // set data binding functions
        BindingUtils.bindProperty(
            _actionPanels, "selectedIndex", _actionTypeSelection, "selectedIndex");
        BindingUtils.bindSetter(updateTypePanels, _actionTypeSelection, "selectedItem");

    }

    /**
     * Called when a new type is selected from the list, it will find call the appropriate panel's
     * update function. The panel itself is popped to the top independently of this function.
     */
    protected function updateTypePanels (def :Object) :void
    {
        var actionData :String = _furniData != null ? _furniData.actionData : null;
        if (def != null) {
            if (def.panelUpdateFn != null) {
                (def.panelUpdateFn as Function)();
            }
            _debug.text = actionData;
        } else {
            _debug.text = "";
        }
    }

    // FurniData.ACTION_NONE functions

    protected function createNonePanel () :UIComponent
    {
        var grid :Grid = new Grid();
        GridUtil.addRow(grid, _ignoreMouse = new CheckBox());
        _ignoreMouse.label = Msgs.EDITING.get("l.ignore_mouse");
        _ignoreMouse.toolTip = Msgs.EDITING.get("l.ignore_mouse_tooltip");

        _ignoreMouse.addEventListener(MouseEvent.CLICK, changedHandler);
        _ignoreMouse.addEventListener(MouseEvent.CLICK, applyHandler);

        return grid;
    }

    protected function updateNonePanel () :void
    {
        if (_furniData != null) {
            // null == capture mouse, "-" means ignore mouse.
            // We don't just check for null, because we want to default back to capturing
            // if the user is switching from a different action type.
            _ignoreMouse.selected = (_furniData.actionData == "-");
        }
    }

    // URL functions

    protected function createURLPanel () :UIComponent
    {
        _url = new TextInput();
        _url.percentWidth = 100;
        _url.maxWidth = 250;
        _url.addEventListener(Event.CHANGE, changedHandler);
        _url.addEventListener(FlexEvent.ENTER, applyHandler);

        _urlTip = new TextInput();
        _urlTip.percentWidth = 100;
        _urlTip.maxWidth = 250;
        _urlTip.addEventListener(Event.CHANGE, changedHandler);
        _urlTip.addEventListener(FlexEvent.ENTER, applyHandler);

        var grid :Grid = new Grid();
        GridUtil.addRow(grid, Msgs.EDITING.get("l.url"));
        GridUtil.addRow(grid, _url);
        GridUtil.addRow(grid, Msgs.EDITING.get("l.urlTip"));
        GridUtil.addRow(grid, _urlTip);
        return grid;
    }

    protected function updateURLPanel () :void
    {
        if (_furniData != null) {
            var data :Array = _furniData.splitActionData();
            // maybe validation here?
            _url.text = data[0] as String;
            _urlTip.text = (data.length > 1) ? data[1] as String : "";
        }
    }

    // HELP_PAGE functions

    protected function createHelpTabPanel () :UIComponent
    {
        var grid :Grid = new Grid();
        GridUtil.addRow(grid, Msgs.EDITING.get("l.help_tab"));
        GridUtil.addRow(grid, _helpTabAction = new TextInput());

        _helpTabAction.addEventListener(Event.CHANGE, changedHandler);
        _helpTabAction.addEventListener(FlexEvent.ENTER, applyHandler);

        return grid;
    }

    protected function updateHelpTabPanel () :void
    {
        if (_furniData != null) {
            var url :String = _furniData.actionData;
            // maybe validation here?
            _helpTabAction.text = url;
        }
    }

    // door functions

    protected function createPortalPanel () :UIComponent
    {
        var grid :Grid = new Grid();

        _door = new TextInput();
        _door.editable = false;
        _door.percentWidth = 100;
        _door.maxWidth = 250;

        GridUtil.addRow(grid, Msgs.EDITING.get("l.dest_scene"));
        GridUtil.addRow(grid, _door);
        GridUtil.addRow(grid, Msgs.EDITING.get("l.set_portal"));
        GridUtil.addRow(grid,
            new CommandButton(Msgs.EDITING.get("b.set_portal"), editPortalTarget));

        return grid;
    }

    protected function updatePortalPanel () :void
    {
        if (_furniData != null) {
            var data :Array = _furniData.splitActionData();
            _door.text = data[data.length-1] as String; // last argument is room name
        }
    }

    /**
     * Called when the player clicks on the "set portal" button, this function closes
     * this properties editor, and tells the room controller to start a new door editor.
     */
    protected function editPortalTarget () :void
    {
        applyChanges();                  // save changes so far
        _controller.actionTargetDoor();  // close editing window, open door editor
    }

    // @Override from BasePanel
    override protected function getUserModifications () :FurniData
    {
        // do not call super - this is a replacement

        if (_furniData == null || _actionTypeSelection.selectedIndex == -1) {
            return null; // nothing to do!
        }

        var newData :FurniData;
        var type :int = _actionTypeSelection.selectedItem.data;

        if (! isActionTypeEditable(type)) {
            // these aren't handled by this editor, so let's not touch the data
            return null;
        }

        newData = _furniData.clone() as FurniData;
        newData.actionType = type;

        switch (type) {
        case FurniData.ACTION_NONE:
            newData.actionData = _ignoreMouse.selected ? "-" : null;
            break;
        case FurniData.ACTION_URL:
            newData.actionData = _url.text;
            var tip :String = StringUtil.trim(_urlTip.text);
            if (!StringUtil.isBlank(tip)) {
                newData.actionData += "||" + tip;
            }
            break;
        case FurniData.ACTION_PORTAL:
            if (_furniData.actionType == FurniData.ACTION_PORTAL) {
                // preserve the existing portal target if we're currently a portal
                newData.actionData = _furniData.actionData;
            } else {
                newData.actionData = DEFAULT_PORTAL_DEST;
            }
            break;
        case FurniData.ACTION_HELP_PAGE:
            newData.actionData = _helpTabAction.text;
            break;
        }

        if (! _furniData.equivalent(newData)) {
            return newData;
        } else {
            return null;
        }
    }

    /** Returns the index in ACTIONS of the action definiton with specified type. */
    protected function getActionIndex (actionType :int) :int
    {
        for (var i :int = 0; i < ACTIONS.length; i++) {
            if (ACTIONS[i].data == actionType) {
                return i;
            }
        }

        return -1;
    }

    /** Returns definition object for the specified action type. */
    protected function getActionDef (actionType :int) :Object
    {
        var index :int = getActionIndex(actionType);
        return (index != -1) ? ACTIONS[index] : null;
    }

    /** Returns true if the specified action type is available in the combo box. */
    protected function isActionTypeEditable (actionType :int) :Boolean
    {
        var def :Object = getActionDef(actionType);
        return (def != null) && Boolean(def.editable);
    }

    /** Returns true if the specifies action type should be displayed to all players,
     *  or false if it should be displayed to support+ staff only. */
    protected function isActionTypeForAllPlayers (actionType :int) :Boolean
    {
        var def :Object = getActionDef(actionType);
        return (def != null) && (! Boolean(def.supportOnly));
    }

    /** Definitions of different action types and how they affect the preferences panel. */
    protected const ACTIONS :Array = [
        { data: FurniData.ACTION_NONE,
          label: Msgs.EDITING.get("l.action_none"),
          editable: true,
          panelCreateFn: createNonePanel,
          panelUpdateFn: updateNonePanel },

        { data: FurniData.ACTION_PORTAL,
          label: Msgs.EDITING.get("l.action_portal"),
          editable: true,
          panelCreateFn: createPortalPanel,
          panelUpdateFn: updatePortalPanel },

        { data: FurniData.ACTION_URL,
          label: Msgs.EDITING.get("l.action_url"),
          editable: true,
          panelCreateFn: createURLPanel,
          panelUpdateFn: updateURLPanel },

        { data: FurniData.ACTION_LOBBY_GAME,
          label: Msgs.EDITING.get("l.action_lobby_game"),
          supportOnly: true },

        { data: FurniData.ACTION_WORLD_GAME,
          label: Msgs.EDITING.get("l.action_world_game"),
          supportOnly: true },

        // Help page is not currently available.
        { data: FurniData.ACTION_HELP_PAGE,
          label: "Obsolete", // Msgs.EDITING.get("l.action_help_page"),
          editable: true,
          supportOnly: true,
          panelCreateFn: createHelpTabPanel,
          panelUpdateFn: updateHelpTabPanel },
    ];

    /** Default location for doors, in case they get interrupted mid-editing. */
    protected static const DEFAULT_PORTAL_DEST :String = "1:";

    protected var _comboEntries :Array = new Array();
    protected var _readOnlyActionLabel :TextInput;
    protected var _actionTypeSelection :ComboBox;
    protected var _actionPanels :ViewStack;
    protected var _ignoreMouse :CheckBox;
    protected var _url :TextInput;
    protected var _urlTip :TextInput;
    protected var _helpTabAction :TextInput;
    protected var _door :TextInput;
    protected var _debug :TextInput;
}

}
