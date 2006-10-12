package com.threerings.msoy.world.client.editor {

import flash.events.MouseEvent;

import mx.binding.utils.BindingUtils;

import mx.containers.GridRow;

import mx.controls.Button;
import mx.controls.ComboBox;
import mx.controls.TextInput;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.SceneBookmarkEntry;

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
        updatePortal(furni);
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

    protected function updatePortal (furni :FurniData) :void
    {
        var isPortal :Boolean = (furni.actionType == FurniData.ACTION_PORTAL);
        _portalDestSceneRow.visible = isPortal;
        _portalDestPortalRow.visible = isPortal;

        if (!isPortal) {
            return;
        }

        var vals :Array = furni.actionData.split(":");
        var targetSceneId :int = int(vals[0]);
        var targetPortalId :int = int(vals[1]);

        _destPortal.text = String(targetPortalId);

        var data :Object = _destScene.dataProvider;
        for (var ii :int = 0; ii < data.length; ii++) {
            var sbe :SceneBookmarkEntry = (data[ii] as SceneBookmarkEntry);
            if (sbe.sceneId === targetSceneId) {
                _destScene.selectedIndex = ii;
                return;
            }
        }
        // never found
        _destScene.text = String(targetSceneId);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var btn :Button = new Button();
        btn.label = "perspective?";
        btn.addEventListener(MouseEvent.CLICK,
            function (evt :MouseEvent) :void {
                (_sprite as FurniSprite).addPersp();
            });

        addRow(
            MsoyUI.createLabel("testing:"),
            btn);

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
              data: FurniData.ACTION_URL },
            { label: _ctx.xlate("editing", "l.action_portal"),
              data: FurniData.ACTION_PORTAL }
        ];

        addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.action")),
            _actionData = new TextInput());

        _portalDestSceneRow = addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.dest_scene")),
            _destScene = new ComboBox());
        _destScene.editable = true;
        _destScene.dataProvider = _ctx.getClientObject().recentScenes.toArray();

        _portalDestPortalRow = addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.dest_portal")),
            _destPortal = new TextInput());
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

        BindingUtils.bindSetter(function (o :Object) :void {
            var furni :FurniData = (_sprite as FurniSprite).getFurniData();
            var item :Object = _destScene.selectedItem;
            var targetSceneId :int;
            if (item != null) {
                targetSceneId = (item as SceneBookmarkEntry).sceneId;

            } else {
                var val :Number = Number(o);
                if (isNaN(val)) {
                    return;
                }
                targetSceneId = int(val);
            }
            var vals :Array = furni.actionData.split(":");
            furni.actionData = targetSceneId + ":" + int(vals[1]);
            spriteWasTextuallyEdited();

        }, _destScene, "text");
        BindingUtils.bindSetter(function (o :Object) :void {
            var val :Number = Number(o);
            if (isNaN(val)) {
                return;
            }
            var furni :FurniData = (_sprite as FurniSprite).getFurniData();
            var vals :Array = furni.actionData.split(":");
            furni.actionData = int(vals[0]) + ":" + int(val);
            spriteWasTextuallyEdited();
        }, _destPortal, "text");
    }
    
    protected var _actionType :ComboBox;
    protected var _actionData :TextInput;

    protected var _destScene :ComboBox;
    protected var _destPortal :TextInput;

    protected var _portalDestSceneRow :GridRow;
    protected var _portalDestPortalRow :GridRow;
}
}

