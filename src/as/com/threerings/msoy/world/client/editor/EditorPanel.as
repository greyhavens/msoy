package com.threerings.msoy.world.client.editor {

import flash.events.Event;

import mx.binding.utils.BindingUtils;

import mx.core.UIComponent;

import mx.containers.TabNavigator;
import mx.containers.VBox;

import mx.controls.Button;
import mx.controls.ButtonBar;
import mx.controls.ComboBox;
import mx.controls.HSlider;
import mx.controls.TextInput;

import com.threerings.mx.controls.CommandButton;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.ui.Grid;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MsoyUI;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.client.InventoryDisplay;
import com.threerings.msoy.item.client.ItemList;

import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.world.client.RoomView;

import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;

/**
 * A floating control widget that aids in scene editing.
 */
public class EditorPanel extends VBox
{
    /** An item list containing the items and props in the scene. */
    public var itemList :ItemList;

    /** The list of items in our inventory. */
    public var inventory :InventoryDisplay;

    /** The button for adding an item from inventory. */
    public var addButton :CommandButton;

    public function EditorPanel (
        ctx :MsoyContext, ctrl :EditorController, roomView :RoomView,
        editableScene :MsoyScene, items :Array)
    {
        _ctx = ctx;
        _ctrl = ctrl;
        _scene = editableScene;
        _sceneModel = (editableScene.getSceneModel() as MsoySceneModel);

        itemList = new ItemList(_ctx, FurniItemRenderer);
        itemList.addItems(items);

        // add all props to the list of items in the room
        for each (var furni :FurniData in _sceneModel.furnis) {
            if (furni.itemType == Item.NOT_A_TYPE) {
                itemList.addItem(furni);
            }
        }

        itemList.addEventListener(Event.CHANGE, function (event :Event) :void {
            // for the love of beavis, for some reason using
            // they keyboard to hightlight items is broken, broken, broken
            _ctrl.itemSelectedFromList(itemList.selectedItem);
        });
    }

    /**
     * Given a MsoySprite, find the appropriate Item or FurniData (prop) from
     * the itemList.
     */
    public function listedItemFromSprite (sprite :MsoySprite) :Object
    {
        // TEMP
        if (!(sprite is FurniSprite)) {
            return null;
        }
        // END: TEMP

        var sprData :FurniData = (sprite as FurniSprite).getFurniData();
        for each (var element :Object in itemList.dataProvider) {
            if (element is Item) {
                var item :Item = (element as Item);
                if (sprData.itemType == item.getType() &&
                        sprData.itemId == item.itemId) {
                    return element;
                }

            } else if (element is FurniData) {
                var furni :FurniData = (element as FurniData);
                if (furni.id == sprData.id) {
                    return element;
                }
            }
        }

        _ctrl.log.warning("Ack! Sprite not found: " + sprData);
        return null;
    }

    public function setEditSprite (sprite :MsoySprite) :void
    {
        if (_spriteEditor != null) {
            _spriteBox.removeChild(_spriteEditor);
            _spriteEditor = null;
        }

        var hasSprite :Boolean = (sprite != null);
        if (hasSprite) {
            if (sprite is FurniSprite) {
                _spriteEditor = new FurniPanel(_ctx);
            } else {
                throw new Error();
            }
            _spriteEditor.setSprite(sprite);
            _spriteBox.addChild(_spriteEditor);

            itemList.selectedItem = listedItemFromSprite(sprite);

        } else {
            itemList.selectedItem = null;
        }

        _deleteBtn.enabled = hasSprite;
    }

    /**
     * Called by the EditorController while sprite properties are
     * being updated interactively.
     */
    public function spritePropertiesUpdated () :void
    {
        if (_spriteEditor != null) {
            _spriteEditor.updateInputFields();
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _tabBox = new TabNavigator();
        _tabBox.resizeToContent = true;
        _tabBox.addChild(createRoomPanel());

        _spriteBox = new VBox();
        _spriteBox.label = _ctx.xlate("editing", "t.sprite_props");

        // add a list that will display items in the room
        _spriteBox.addChild(itemList);

        // add a delete button (temp?)
        _deleteBtn = new CommandButton(EditorController.DEL_ITEM);
        _deleteBtn.label = _ctx.xlate("editing", "b.delete_item");
        _deleteBtn.enabled = false;
        _spriteBox.addChild(_deleteBtn);

        _tabBox.addChild(_spriteBox);

        _inventoryBox = new VBox();
        _inventoryBox.label = _ctx.xlate("editing", "t.inventory");
        inventory = new InventoryDisplay(_ctx)
        _inventoryBox.addChild(inventory);

        addButton = new CommandButton(EditorController.ADD_ITEM);
        addButton.label = _ctx.xlate("editing", "b.add_item");
        addButton.enabled = false;
        _inventoryBox.addChild(addButton);

        _tabBox.addChild(_inventoryBox);

        addChild(_tabBox);

        var butBox :ButtonBar = new ButtonBar();
        var btn :CommandButton;

        btn = new CommandButton(EditorController.DISCARD_EDITS);
        btn.label = _ctx.xlate("editing", "b.discard_edits");
        butBox.addChild(btn);

        btn = new CommandButton(EditorController.SAVE_EDITS);
        btn.label = _ctx.xlate("editing", "b.save_edits");
        butBox.addChild(btn);

        addChild(butBox);
    }

    protected function createRoomPanel () :VBox
    {
        var box :VBox = new VBox();
        box.label = _ctx.xlate("editing", "t.room_props");

        // add a grid of controls for the room
        var grid :Grid = new Grid();

        // edit scene type
        grid.addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.scene_type")),
            _type = new ComboBox());
        var types :Array = [];
        for (var ii :int = 0; ii < MsoySceneModel.TYPE_COUNT; ii++) {
            types.push({ label: _ctx.xlate("editing", "m.scene_type_" + ii),
                         data: ii });
        }
        _type.dataProvider = types;

//        // background furni
//        grid.addRow(
//            MsoyUI.createLabel(_ctx.xlate("editing", "l.scene_background")),
//            _background = new ItemReceptor(_ctx));

        grid.addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.scene_name")),
            _name = new TextInput());
        grid.addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.scene_width")),
            _width = new TextInput());
        grid.addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.scene_depth")),
            _depth = new TextInput());
        grid.addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.horizon")),
            _horizon = new HSlider());
        _horizon.minimum = 0;
        _horizon.maximum = 1;
        _horizon.liveDragging = true;

        box.addChild(grid);

        return box;
    }

    /**
     * Set the current displayed values to those in the model.
     */
    public function updateInputFields () :void
    {
        _type.selectedIndex = _sceneModel.type;
        //_background = 
        _name.text = _sceneModel.name;
        _width.text = String(_sceneModel.width);
        _depth.text = String(_sceneModel.depth);
        _horizon.value = _sceneModel.horizon;
    }

    override protected function childrenCreated () :void
    {
        super.childrenCreated();

        updateInputFields();

        BindingUtils.bindSetter(function (o :Object) :void {
            _sceneModel.type = int(o.data);
            _ctrl.sceneModelUpdated();
        }, _type, "selectedItem");

        BindingUtils.bindSetter(function (o :String) :void {
            _sceneModel.name = o;
        }, _name, "text");

        BindingUtils.bindSetter(function (o :Object) :void {
            var val :Number = Number(o);
            if (!isNaN(val)) {
                _sceneModel.width = int(val);
                _ctrl.sceneModelUpdated();
            }
        }, _width, "text");

        BindingUtils.bindSetter(function (o :Object) :void {
            var val :Number = Number(o);
            if (!isNaN(val)) {
                _sceneModel.depth = int(val);
                _ctrl.sceneModelUpdated();
            }
        }, _depth, "text");

        BindingUtils.bindSetter(function (o :Object) :void {
            var val :Number = Number(o);
            _sceneModel.horizon = val;
            _ctrl.sceneModelUpdated();
        }, _horizon, "value");
    }

    protected var _ctx :MsoyContext;

    protected var _ctrl :EditorController;

    protected var _scene :MsoyScene;
    protected var _sceneModel :MsoySceneModel;

    protected var _tabBox :TabNavigator;

    /** The place where we add the sprite editor. */
    protected var _spriteBox :VBox;

    protected var _type :ComboBox;
    protected var _background :ItemReceptor;

    protected var _inventoryBox :VBox;

    protected var _name :TextInput;
    protected var _width :TextInput;
    protected var _depth :TextInput;
    protected var _horizon :HSlider;

    protected var _deleteBtn :CommandButton;

    protected var _spriteEditor :SpritePanel;
}
}
