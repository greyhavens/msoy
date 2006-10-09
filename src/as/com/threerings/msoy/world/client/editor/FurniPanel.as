package com.threerings.msoy.world.client.editor {

import mx.binding.utils.BindingUtils;

import mx.controls.ComboBox;
import mx.controls.TextInput;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.ui.MsoyUI;

import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.data.FurniData;


public class FurniPanel extends SpritePanel
{
    public function FurniPanel (ctx :MsoyContext)
    {
        super(ctx);
    }

    override public function updateInputFields () :void
    {
        super.updateInputFields();

        var furni :FurniData = (_sprite as FurniSprite).getFurniData();
        updateActionType(furni);
        _actionData.text = furni.actionData;
    }

    protected function updateActionType (furni :FurniData) :void
    {
        var data :Object = _actionType.dataProvider;
        for (var ii :int = 0; ii < data.length; ii++) {
            if (data[ii].data == furni.actionType) {
                _actionType.selectedIndex = ii;
                return;
            }
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.action")),
            _actionType = new ComboBox());
        _actionType.dataProvider = [
            { label: _ctx.xlate("editing", "l.action_none"),
              data: FurniData.ACTION_NONE },
            { label: _ctx.xlate("editing", "l.background"),
              data: FurniData.BACKGROUND },
            { label: _ctx.xlate("editing", "l.action_game"),
              data: FurniData.ACTION_GAME },
            { label: _ctx.xlate("editing", "l.action_url"),
              data: FurniData.ACTION_URL }
        ];

        addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.action")),
            _actionData = new TextInput());
    }

    override protected function bind () :void
    {
        super.bind();

        BindingUtils.bindSetter(function (o :Object) :void {
            var furni :FurniData = (_sprite as FurniSprite).getFurniData();
            var item :Object = _actionType.selectedItem;
            furni.actionType = int(item.data);

            // TODO: maybe remove?
            // since currently background is an action type, we recheck
            (_sprite as FurniSprite).update(_ctx, furni);

            spriteWasTextuallyEdited();
        }, _actionType, "text");

        BindingUtils.bindSetter(function (o :Object) :void {
            var furni :FurniData = (_sprite as FurniSprite).getFurniData();
            furni.actionData = String(o);
            spriteWasTextuallyEdited();
        }, _actionData, "text");
    }
    
    protected var _actionType :ComboBox;
    protected var _actionData :TextInput;
}
}

