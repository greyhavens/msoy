package com.threerings.msoy.world.client.editor {

import flash.events.MouseEvent;

import mx.binding.utils.BindingUtils;

import mx.collections.ListCollectionView;

import mx.containers.Grid;
import mx.containers.VBox;
import mx.containers.ViewStack;

import mx.controls.CheckBox;
import mx.controls.ComboBox;
import mx.controls.HSlider;
import mx.controls.Label;
import mx.controls.TextInput;

import mx.core.UIComponent;

import com.threerings.util.ArrayUtil;

import com.threerings.flex.GridUtil;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.SceneBookmarkEntry;

import com.threerings.msoy.ui.MsoyUI;

import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.data.FurniData;


public class FurniPanel extends SpritePanel
{
    override public function updateInputFields () :void
    {
        super.updateInputFields();

        _scaleEditor.setSprite(_sprite);

        var furniSprite :FurniSprite = (_sprite as FurniSprite);

        _perspective.selected = furniSprite.isPerspectivized();
        _perspective.enabled = furniSprite.isPerspectable();

        var furni :FurniData = furniSprite.getFurniData();
        updateActionType(furni);

        switch (furni.actionType) {
        case FurniData.BACKGROUND:
            updateBackground(furni);
            break;

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
                if (furni.actionType == FurniData.BACKGROUND) {
                    _centering.selected = false;
                }
                return;
            }
        }
    }

    protected function updatePortal (furni :FurniData) :void
    {
        var vals :Array = furni.splitActionData();
        var targetSceneId :int = int(vals[0]);

//        _destPortal.text = String(targetPortalId);

        var data :Object = _destScene.dataProvider;
        for (var ii :int = 0; ii < data.length; ii++) {
            var o :Object = data[ii];
            if (o is SceneBookmarkEntry) {
                var sbe :SceneBookmarkEntry = (o as SceneBookmarkEntry);
                if (sbe.sceneId === targetSceneId) {
                    _destScene.selectedIndex = ii;
                    return;
                }
            } else if (targetSceneId === o) {
                _destScene.selectedIndex = ii;
                return;
            }
        }

        // never found. setting _destScene.text should work, but it
        // doesn't, so add the scene to the dataprovider..
        ListCollectionView(_destScene.dataProvider).addItem(targetSceneId);
        _destScene.selectedIndex = data.length;
    }

    protected function updateBackground (furni :FurniData) :void
    {
        // TODO: I guess you can't use an audio item's furniture visualization
        // as the background for a scene...

        var isAudio :Boolean = furni.media.isAudio();
        _volume.visible = isAudio;

        if (isAudio) {
            var v :Number = Number(furni.actionData);
            _volume.value = isNaN(v) ? 1 : Math.min(1, Math.max(0, v));
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        // set up scale editing
        GridUtil.addRow(this,
            _scaleEditor = new ScaleEditor(_ctx), [2, 1]);

        // set up perspective editing
        _perspective = new CheckBox();
        _perspective.addEventListener(MouseEvent.CLICK,
            function (evt :MouseEvent) :void {
                var furn :FurniSprite = FurniSprite(_sprite);
                furn.togglePerspective();
                spritePropsUpdated();
            });
        GridUtil.addRow(this,
            MsoyUI.createLabel(Msgs.EDITING.get("l.perspective")),
            _perspective);

        // set up action editing
        GridUtil.addRow(this,
            MsoyUI.createLabel(Msgs.EDITING.get("l.action")),
            _actionType = new ComboBox());
        _actionType.dataProvider = [
            { label: Msgs.EDITING.get("l.action_none"),
              data: FurniData.ACTION_NONE },
            { label: Msgs.EDITING.get("l.background"),
              data: FurniData.BACKGROUND },
            { label: Msgs.EDITING.get("l.action_lobby_game"),
              data: FurniData.ACTION_LOBBY_GAME },
            { label: Msgs.EDITING.get("l.action_world_game"),
              data: FurniData.ACTION_WORLD_GAME },
            { label: Msgs.EDITING.get("l.action_url"),
              data: FurniData.ACTION_URL },
            { label: Msgs.EDITING.get("l.action_portal"),
              data: FurniData.ACTION_PORTAL }
        ];

        _actionPanels = new ViewStack();
        _actionPanels.addChild(new VBox()); // ACTION_NONE
        _actionPanels.addChild(createBackgroundEditor()); // BACKGROUND
        _actionPanels.addChild(new VBox()); // ACTION_LOBBY_GAME (nothing to edit)
        _actionPanels.addChild(new VBox()); // ACTION_WORLD_GAME
        _actionPanels.addChild(createURLEditor()); // ACTION_URL
        _actionPanels.addChild(createPortalEditor()); // ACTION_PORTAL
        GridUtil.addRow(this, _actionPanels, [2, 1]);

        BindingUtils.bindProperty(_actionPanels, "selectedIndex",
            _actionType, "selectedIndex");

        // BEGIN temporary controls
        // add an "expert control" for directly editing the action
        var lbl :Label;
        GridUtil.addRow(this,
            lbl = MsoyUI.createLabel(Msgs.EDITING.get("l.action")),
            _actionData = new TextInput());
        lbl.setStyle("color", 0xFF0000);
        // END: temporary things
    }

    protected function createBackgroundEditor () :UIComponent
    {
        var grid :Grid = new Grid();
        GridUtil.addRow(grid,
            MsoyUI.createLabel(Msgs.EDITING.get("l.volume")),
            _volume = new HSlider());
        _volume.liveDragging = true;
        _volume.minimum = 0;
        _volume.maximum = 1;
        return grid;
    }

    protected function createURLEditor () :UIComponent
    {
        var grid :Grid = new Grid();
        GridUtil.addRow(grid,
            MsoyUI.createLabel(Msgs.EDITING.get("l.url")),
            _url = new TextInput());
        return grid;
    }

    protected function createPortalEditor () :UIComponent
    {
        var grid :Grid = new Grid();
        GridUtil.addRow(grid,
            MsoyUI.createLabel(Msgs.EDITING.get("l.dest_scene")),
            _destScene = new ComboBox());
        _destScene.editable = true;

        // combine recent and owned scenes into one array
        var recent :Array = _ctx.getClientObject().recentScenes.toArray();
        var owned :Array = _ctx.getClientObject().ownedScenes.toArray();
        var scenes :Array = recent.concat();
        for each (var sbe :SceneBookmarkEntry in owned) {
            if (!ArrayUtil.contains(recent, sbe)) {
                scenes.push(sbe);
            }
        }
        _destScene.dataProvider = scenes;

//        GridUtil.addRow(grid,
//            MsoyUI.createLabel(Msgs.EDITING.get("l.dest_portal")),
//            _destPortal = new TextInput());

        return grid;
    }

    override protected function bind () :void
    {
        super.bind();

        BindingUtils.bindSetter(function (o :Object) :void {
            var furni :FurniData = (_sprite as FurniSprite).getFurniData();
            var item :Object = _actionType.selectedItem;
            furni.actionType = int(item.data);

            // force the sprite to recheck props, so that it re-reads
            // whether it's a background
            (_sprite as FurniSprite).update(furni);

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

        BindingUtils.bindSetter(function (val :Number) :void {
            var furni :FurniData = (_sprite as FurniSprite).getFurniData();
            if (furni.actionType != FurniData.BACKGROUND) {
                return; // don't update if we shouldn't
            }
            furni.actionData = String(val);
            spritePropsUpdated();
        }, _volume, "value");

        BindingUtils.bindSetter(function (o :Object) :void {
            var furni :FurniData = (_sprite as FurniSprite).getFurniData();
            if (furni.actionType != FurniData.ACTION_PORTAL) {
                return; // don't update if we shouldn't
            }
            var item :Object = _destScene.selectedItem;
            var targetSceneId :int;
            if (item is SceneBookmarkEntry) {
                targetSceneId = (item as SceneBookmarkEntry).sceneId;
                
            } else if (item is int) {
                targetSceneId = int(item);

            } else {
                // parse the 'text' value
                var val :Number = Number(o);
                if (isNaN(val)) {
                    return;
                }
                targetSceneId = int(val);
            }
//            var vals :Array = furni.splitActionData();
//            vals.shift(); // remove the previous first entry
//            vals.unshift(targetSceneId);
//            furni.actionData = vals.join(":");
            furni.actionData = String(targetSceneId);
            spritePropsUpdated();
        }, _destScene, "text");

//        BindingUtils.bindSetter(function (o :Object) :void {
//            var furni :FurniData = (_sprite as FurniSprite).getFurniData();
//            if (furni.actionType != FurniData.ACTION_PORTAL) {
//                return; // don't update if we shouldn't
//            }
//            var val :Number = Number(o);
//            if (isNaN(val)) {
//                return;
//            }
//            var vals :Array = furni.splitActionData();
//            vals[1] = int(val); // replace the target portal id
//            furni.actionData = vals.join(":");
//            spritePropsUpdated();
//        }, _destPortal, "text");
    }

    protected var _scaleEditor :ScaleEditor;

    protected var _actionType :ComboBox;
    protected var _actionData :TextInput;

    protected var _actionPanels :ViewStack;

    protected var _destScene :ComboBox;

    protected var _perspective :CheckBox;
//    protected var _destPortal :TextInput;

    protected var _volume :HSlider;

    protected var _url :TextInput;
}
}
