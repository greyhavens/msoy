package com.threerings.msoy.world.client.editor {

import mx.binding.utils.BindingUtils;

import mx.core.UIComponent;

import mx.containers.VBox;

import mx.controls.Button;
import mx.controls.ComboBox;
import mx.controls.HSlider;
import mx.controls.TextInput;

import com.threerings.mx.controls.CommandButton;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.ui.Grid;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MsoyUI;

import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.world.client.PortalSprite;
import com.threerings.msoy.world.client.RoomView;

import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;

/**
 * A floating control widget that aids in scene editing.
 */
public class EditorPanel extends FloatingPanel
{
    public static const DISCARD_BUTTON :int = 10;
    public static const SAVE_BUTTON :int = 11;

    public function EditorPanel (
        ctx :MsoyContext, ctrl :EditorController, roomView :RoomView,
        editableScene :MsoyScene)
    {
        super(ctx, ctx.xlate("editing", "t.editing"));
        _ctrl = ctrl;
        _scene = editableScene;
        _sceneModel = (editableScene.getSceneModel() as MsoySceneModel);

        // open non-modal with the room as the parent
        open(false, roomView);
    }

    public function setEditSprite (sprite :MsoySprite) :void
    {
        if (_spriteEditor != null) {
            _box.removeChild(_spriteEditor);
            _spriteEditor = null;
        }

        if (sprite != null) {
            if (sprite is FurniSprite) {
                _spriteEditor = new FurniPanel(_ctx);
            } else if (sprite is PortalSprite) {
                _spriteEditor = new PortalPanel(_ctx);
            } else {
                throw new Error();
            }
            _spriteEditor.setSprite(sprite);
            _box.addChild(_spriteEditor);
        }

        _deleteBtn.enabled = (sprite != null);
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

        // background furni
        grid.addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.scene_background")),
            _background = new ItemReceptor(_ctx));

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

        addChild(grid);

        var btn :CommandButton;

        btn = new CommandButton(EditorController.INSERT_PORTAL);
        btn.label = _ctx.xlate("editing", "b.new_portal");
        addChild(btn);

        btn = new CommandButton(EditorController.INSERT_FURNI);
        btn.label = _ctx.xlate("editing", "b.new_furni");
        addChild(btn);

        btn = new CommandButton(EditorController.DEL_ITEM);
        btn.label = _ctx.xlate("editing", "b.delete_item");
        btn.enabled = false;
        _deleteBtn = btn;
        addChild(btn);

        addChild(_box = new VBox());

        addButtons(DISCARD_BUTTON, SAVE_BUTTON);
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

    override protected function createButton (buttonId :int) :Button
    {
        var btn :CommandButton;
        switch (buttonId) {
        case DISCARD_BUTTON:
            btn = new CommandButton(EditorController.DISCARD_EDITS);
            btn.label = _ctx.xlate("editing", "b.discard_edits");
            return btn;

        case SAVE_BUTTON:
            btn = new CommandButton(EditorController.SAVE_EDITS);
            btn.label = _ctx.xlate("editing", "b.save_edits");
            return btn;

        default:
            return super.createButton(buttonId);
        }
    }

    protected var _ctrl :EditorController;

    protected var _scene :MsoyScene;
    protected var _sceneModel :MsoySceneModel;

    protected var _box :VBox;

    protected var _type :ComboBox;
    protected var _background :ItemReceptor;

    protected var _name :TextInput;
    protected var _width :TextInput;
    protected var _depth :TextInput;
    protected var _horizon :HSlider;

    protected var _deleteBtn :CommandButton;

    protected var _spriteEditor :SpritePanel;
}
}
