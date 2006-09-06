package com.threerings.msoy.world.client {

import mx.binding.utils.BindingUtils;

import mx.core.UIComponent;

import mx.containers.VBox;

import mx.controls.Button;
import mx.controls.TextInput;

import com.threerings.mx.controls.CommandButton;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.ui.Grid;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MsoyUI;

import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;

/**
 * A floating control widget that aids in scene editing.
 */
public class EditRoomPanel extends FloatingPanel
{
    public static const DISCARD_BUTTON :int = 10;
    public static const SAVE_BUTTON :int = 11;

    public function EditRoomPanel (
        ctx :MsoyContext, ctrl :EditRoomController, roomView :RoomView,
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
                _spriteEditor = new FurniEditorPanel(_ctx);
            } else if (sprite is PortalSprite) {
                _spriteEditor = new PortalEditorPanel(_ctx);
            } else {
                throw new Error();
            }
            _spriteEditor.setSprite(sprite);
            _box.addChild(_spriteEditor);
        }

        _deleteBtn.enabled = (sprite != null);
    }

    /**
     * Called by the EditRoomController while sprite properties are
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
        grid.addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.scene_width")),
            _width = new TextInput());
        addChild(grid);

        var btn :CommandButton;

        btn = new CommandButton(EditRoomController.INSERT_PORTAL);
        btn.label = _ctx.xlate("editing", "b.new_portal");
        addChild(btn);

        btn = new CommandButton(EditRoomController.INSERT_FURNI);
        btn.label = _ctx.xlate("editing", "b.new_furni");
        addChild(btn);

        btn = new CommandButton(EditRoomController.DEL_ITEM);
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
        _width.text = String(_sceneModel.width);
    }

    override protected function childrenCreated () :void
    {
        super.childrenCreated();

        updateInputFields();

        BindingUtils.bindSetter(function (o :Object) :void {
            var val :Number = Number(o);
            if (!isNaN(val)) {
                _sceneModel.width = int(val);
                _ctrl.sceneModelUpdated();
            }
        }, _width, "text");
    }

    override protected function createButton (buttonId :int) :Button
    {
        var btn :CommandButton;
        switch (buttonId) {
        case DISCARD_BUTTON:
            btn = new CommandButton(EditRoomController.DISCARD_EDITS);
            btn.label = _ctx.xlate("editing", "b.discard_edits");
            return btn;

        case SAVE_BUTTON:
            btn = new CommandButton(EditRoomController.SAVE_EDITS);
            btn.label = _ctx.xlate("editing", "b.save_edits");
            return btn;

        default:
            return super.createButton(buttonId);
        }
    }

    protected var _ctrl :EditRoomController;

    protected var _scene :MsoyScene;
    protected var _sceneModel :MsoySceneModel;

    protected var _box :VBox;

    protected var _width :TextInput;

    protected var _deleteBtn :CommandButton;

    protected var _spriteEditor :SpriteEditorPanel;
}
}
