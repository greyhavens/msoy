//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.events.Event;

import mx.binding.utils.BindingUtils;
import mx.containers.Grid;
import mx.containers.HBox;
import mx.containers.VBox;
import mx.containers.ViewStack;
import mx.controls.CheckBox;
import mx.controls.ComboBox;
import mx.controls.TextInput;
import mx.core.UIComponent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.GridUtil;
import com.threerings.util.Util;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.world.client.RoomController;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MsoyUI;


/**
 * A panel for editing general room settings.
 */
public class ItemPreferencesPanel extends FloatingPanel
{
    public function ItemPreferencesPanel (
        ctx :WorldContext, editCtrl :RoomEditController, roomCtrl :RoomController)
    {
        super(ctx, Msgs.EDITING.get("t.item_prefs"));
        _editCtrl = editCtrl;
        _roomCtrl = roomCtrl;
    }

    /**
     * Refreshes the panel with the furni data. If another furni was being edited, this
     * will simply overwrite the edits, effectively cancelling previous changes.
     */
    public function update (furniData :FurniData) :void
    {
        // abandon previous edits
        _furniData = furniData.clone() as FurniData;

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

        _actionPanels.visible = _actionPanels.includeInLayout = editable;
        _actionTypeSelection.visible = _actionTypeSelection.includeInLayout = editable;
        _readOnlyActionLabel.visible = _readOnlyActionLabel.includeInLayout = ! editable;
    }
        
    
    // from superclass
    override protected function createChildren () :void
    {
        super.createChildren();

        // generate combo box definitions, including only those actions that are
        // mentioned in the EDITABLE array.
        var entries :Array = new Array();
        for each (var type :int in EDITABLE_ACTION_TYPES) {
            var def :Object = getActionDef(type);
            if (def != null) {
                entries.push(def);
            }
        }
        
        // create ui bits
        var grid :Grid = new Grid();
        addChild(grid);
        
        // this combo box will let the user pick a type
        _actionTypeSelection = new ComboBox();
        _actionTypeSelection.dataProvider = entries;
        // and this will be displayed instead of the drop-down box if the user can't edit it
        _readOnlyActionLabel = new TextInput();
        _readOnlyActionLabel.editable = false;
        
        var action :HBox = new HBox();
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
        _debug.editable = false;
        if (_ctx.getMemberObject().tokens.isSupport()) {
            var dgrid :Grid = new Grid();
            dgrid.setStyle("color", 0xff0000);
            addChild(dgrid);
            GridUtil.addRow(dgrid, Msgs.EDITING.get("l.action"), _debug);
        }
        
        addButtons(OK_BUTTON, CANCEL_BUTTON);
    }

    // from superclass
    override protected function childrenCreated () :void
    {
        super.childrenCreated();

        // set data binding functions
        BindingUtils.bindProperty(
            _actionPanels, "selectedIndex", _actionTypeSelection, "selectedIndex");
        BindingUtils.bindSetter(updateTypePanels, _actionTypeSelection, "selectedItem");
    }

    // from superclass
    override protected function buttonClicked (buttonId :int) :void
    {
        // maybe send an update to the server
        if (buttonId == OK_BUTTON) {
            var newData :FurniData = getUserModifications();
            if (newData != null) {
                _editCtrl.updateFurni(_furniData, newData);
            }
        }

        _furniData = null;
        
        // let the parent close this window, or do whatever it wants
        super.buttonClicked(buttonId);
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
        GridUtil.addRow(grid, Msgs.EDITING.get("l.captureMouse"), _captureMouse = new CheckBox());
        return grid;
    }

    protected function updateNonePanel () :void
    {
        if (_furniData != null) {
            // null == capture mouse, "-" means don't.
            // We don't just check for null, because we want to default back to capturing
            // if the user is switching from a different action type.
            _captureMouse.selected = (_furniData.actionData != "-");
        }
    }

    // URL functions
    
    protected function createURLPanel () :UIComponent
    {
        var grid :Grid = new Grid();
        GridUtil.addRow(grid, Msgs.EDITING.get("l.url"), _url = new TextInput());
        return grid;
    }

    protected function updateURLPanel () :void
    {
        if (_furniData != null) {
            var url :String = _furniData.actionData;
            // maybe validation here?
            _url.text = url; 
        }
    }

    // door functions
    
    protected function createPortalPanel () :UIComponent
    {
        var grid :Grid = new Grid();

        _door = new TextInput();
        _door.editable = false;
        
        var setportal :CommandButton = new CommandButton();
        setportal.label = Msgs.EDITING.get("b.set_portal");
        setportal.setCallback(this.editPortalTarget);

        GridUtil.addRow(grid, Msgs.EDITING.get("l.dest_scene"), _door);
        GridUtil.addRow(grid, Msgs.EDITING.get("l.set_portal"), setportal);
            
        return grid;
    }

    protected function updatePortalPanel () :void
    {
        if (_furniData != null) {
            var data :Array = _furniData.splitActionData();
            var door :String = data[1] as String;
            _door.text = door; 
        }
    }

    /**
     * Called when the player clicks on the "set portal" button, this function closes
     * this properties editor, and tells the room controller to start a new door editor.
     */
    protected function editPortalTarget () :void
    {
        var data :FurniData = _furniData;  // keep a reference to the furni data
        buttonClicked(OK_BUTTON);          // close this window, saving changes
        _roomCtrl.handleEditDoor(data);    // start the door editor
    }

    /**
     * Copies action data from the UI based on action type, and if the data is different,
     * creates a new FurniData with changes applied.
     */
    protected function getUserModifications () :FurniData
    {
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
            newData.actionData = _captureMouse.selected ? null : "-";
            break;
        case FurniData.ACTION_URL:
            newData.actionData = _url.text;
            break;
        case FurniData.ACTION_PORTAL:
            newData.actionData = DEFAULT_PORTAL_DEST;
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
        return EDITABLE_ACTION_TYPES.indexOf(actionType) != -1;
    }

    /** List of the action types available in the combo box. */
    protected const EDITABLE_ACTION_TYPES :Array =
        [ FurniData.ACTION_NONE, FurniData.ACTION_PORTAL, FurniData.ACTION_URL ];
    
    /** Definitions of different action types and how they affect the preferences panel. */
    protected const ACTIONS :Array = [
        { data: FurniData.ACTION_NONE,
          label: Msgs.EDITING.get("l.action_none"),
          panelCreateFn: createNonePanel,
          panelUpdateFn: updateNonePanel },

        { data: FurniData.ACTION_PORTAL,
          label: Msgs.EDITING.get("l.action_portal"),
          panelCreateFn: createPortalPanel,
          panelUpdateFn: updatePortalPanel },

        { data: FurniData.ACTION_URL,
          label: Msgs.EDITING.get("l.action_url"),
          panelCreateFn: createURLPanel,
          panelUpdateFn: updateURLPanel },

        { data: FurniData.ACTION_LOBBY_GAME,
          label: Msgs.EDITING.get("l.action_lobby_game") },

        { data: FurniData.ACTION_WORLD_GAME,
          label: Msgs.EDITING.get("l.action_world_game") }

        ];

    /** Default location for doors, in case they get interrupted mid-editing. */
    protected static const DEFAULT_PORTAL_DEST :String = "1:";
    
    protected var _roomCtrl :RoomController;
    protected var _editCtrl :RoomEditController;
    protected var _furniData :FurniData;

    protected var _comboEntries :Array = new Array();
    protected var _readOnlyActionLabel :TextInput;
    protected var _actionTypeSelection :ComboBox;
    protected var _actionPanels :ViewStack;
    protected var _captureMouse :CheckBox;
    protected var _url :TextInput;
    protected var _door :TextInput;
    protected var _debug :TextInput;
}
}
        
