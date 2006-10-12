package com.threerings.msoy.world.client.editor {

import flash.events.MouseEvent;

import mx.binding.utils.BindingUtils;

import mx.containers.VBox;
import mx.containers.ViewStack;

import mx.controls.Button;
import mx.controls.ComboBox;
import mx.controls.Label;
import mx.controls.TextInput;

import mx.core.UIComponent;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.SceneBookmarkEntry;

import com.threerings.msoy.ui.Grid;
import com.threerings.msoy.ui.MsoyUI;

import com.threerings.msoy.world.client.MsoySprite;
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

        _locEditor.setSprite(_sprite);

        _xScale.text = String(_sprite.getMediaScaleX());
        _yScale.text = String(_sprite.getMediaScaleY());

        var furni :FurniData = (_sprite as FurniSprite).getFurniData();
        updateActionType(furni);

        switch (furni.actionType) {
        case FurniData.ACTION_PORTAL:
            updatePortal(furni);
            break;

        case FurniData.ACTION_URL:
            _url.text = furni.actionData;
            break;
        }

        // TEMP: update the show-all/edit-all control
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

    protected function updatePortal (furni :FurniData) :void
    {
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

        // location: big controls
        addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.loc")),
            _locEditor = new LocationEditor(_ctx));

        // scale
        addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.xscale")),
            _xScale = new TextInput());
        MsoyUI.enforceNumber(_xScale);
        addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.yscale")),
            _yScale = new TextInput());
        MsoyUI.enforceNumber(_yScale);

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

        _actionPanels = new ViewStack();
        _actionPanels.addChild(new VBox()); // ACTION_NONE
        _actionPanels.addChild(new VBox()); // BACKGROUND (nothing to edit)
        _actionPanels.addChild(new VBox()); // ACTION_GAME (nothing to edit)
        _actionPanels.addChild(createURLEditor()); // ACTION_URL
        _actionPanels.addChild(createPortalEditor()); // ACTION_PORTAL
        addRow(_actionPanels, [2, 1]);

        BindingUtils.bindProperty(_actionPanels, "selectedIndex",
            _actionType, "selectedIndex");

        // BEGIN temporary controls
        var lbl :Label;
        var btn :Button = new Button();
        btn.label = "perspective?";
        btn.addEventListener(MouseEvent.CLICK,
            function (evt :MouseEvent) :void {
                (_sprite as FurniSprite).addPersp();
            });
        addRow(
            lbl = MsoyUI.createLabel("testing:"),
            btn);
        lbl.setStyle("color", 0xFF0000);

        // add an "expert control" for directly editing the action
        addRow(
            lbl = MsoyUI.createLabel(_ctx.xlate("editing", "l.action")),
            _actionData = new TextInput());
        lbl.setStyle("color", 0xFF0000);
        // END: temporary things
    }

    protected function createURLEditor () :UIComponent
    {
        var grid :Grid = new Grid();
        grid.addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.url")),
            _url = new TextInput());
        return grid;
    }

    protected function createPortalEditor () :UIComponent
    {
        var grid :Grid = new Grid();
        grid.addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.dest_scene")),
            _destScene = new ComboBox());
        _destScene.editable = true;
        _destScene.dataProvider = _ctx.getClientObject().recentScenes.toArray();

        grid.addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.dest_portal")),
            _destPortal = new TextInput());

        return grid;
    }

    override protected function bind () :void
    {
        super.bind();

        BindingUtils.bindSetter(function (o :Object) :void {
            var val :Number = Number(o);
            if (!isNaN(val)) {
                _sprite.setMediaScaleX(val);
                spritePropsUpdated();
            }
        }, _xScale, "text");

        BindingUtils.bindSetter(function (o :Object) :void {
            var val :Number = Number(o);
            if (!isNaN(val)) {
                _sprite.setMediaScaleY(val);
                spritePropsUpdated();
            }
        }, _yScale, "text");

        BindingUtils.bindSetter(function (o :Object) :void {
            var furni :FurniData = (_sprite as FurniSprite).getFurniData();
            var item :Object = _actionType.selectedItem;
            furni.actionType = int(item.data);

            // force the sprite to recheck props, so that it re-reads
            // whether it's a background
            (_sprite as FurniSprite).update(_ctx, furni);

            spritePropsUpdated();
        }, _actionType, "text");

        BindingUtils.bindSetter(function (o :Object) :void {
            var furni :FurniData = (_sprite as FurniSprite).getFurniData();
            furni.actionData = String(o);
            spritePropsUpdated();
        }, _actionData, "text");

        BindingUtils.bindSetter(function (url :String) :void {
            var furni :FurniData = (_sprite as FurniSprite).getFurniData();
            if (furni.actionType != FurniData.ACTION_URL) {
                return; // don't update if we shouldn't
            }
            furni.actionData = url;
            spritePropsUpdated();
        }, _url, "text");

        BindingUtils.bindSetter(function (o :Object) :void {
            var furni :FurniData = (_sprite as FurniSprite).getFurniData();
            if (furni.actionType != FurniData.ACTION_PORTAL) {
                return; // don't update if we shouldn't
            }
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
            spritePropsUpdated();
        }, _destScene, "text");

        BindingUtils.bindSetter(function (o :Object) :void {
            var furni :FurniData = (_sprite as FurniSprite).getFurniData();
            if (furni.actionType != FurniData.ACTION_PORTAL) {
                return; // don't update if we shouldn't
            }
            var val :Number = Number(o);
            if (isNaN(val)) {
                return;
            }
            var vals :Array = furni.actionData.split(":");
            furni.actionData = int(vals[0]) + ":" + int(val);
            spritePropsUpdated();
        }, _destPortal, "text");
    }

    protected var _xScale :TextInput;
    protected var _yScale :TextInput;

    protected var _locEditor :LocationEditor;
    
    protected var _actionType :ComboBox;
    protected var _actionData :TextInput;

    protected var _actionPanels :ViewStack;

    protected var _destScene :ComboBox;
    protected var _destPortal :TextInput;

    protected var _url :TextInput;
}
}
