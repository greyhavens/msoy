package com.threerings.msoy.world.client {

import mx.binding.utils.BindingUtils;

import mx.controls.ComboBox;
import mx.controls.TextInput;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.SceneBookmarkEntry;

import com.threerings.msoy.world.data.MsoyPortal;

import com.threerings.msoy.ui.MsoyUI;

public class PortalEditorPanel extends SpriteEditorPanel
{
    public function PortalEditorPanel (ctx :MsoyContext)
    {
        super(ctx);
    }

    override public function updateInputFields () :void
    {
        super.updateInputFields();

        var portal :MsoyPortal = (_sprite as PortalSprite).getPortal();

        updateDestScene(portal);
        _destPortal.text = String(portal.targetPortalId);
    }

    protected function updateDestScene (portal :MsoyPortal) :void
    {
        var data :Object = _destScene.dataProvider;
        for (var ii :int = 0; ii < data.length; ii++) {
            var sbe :SceneBookmarkEntry = (data[ii] as SceneBookmarkEntry);
            trace("sbe: " + sbe);
            if (sbe.sceneId === portal.targetSceneId) {
                trace("found dest in recent");
                _destScene.selectedIndex = ii;
                return;
            }
        }
        // never found
        trace("never found dest in recent");
        _destScene.text = String(portal.targetSceneId);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.dest_scene")),
            _destScene = new ComboBox());
        _destScene.editable = true;
        _destScene.dataProvider = _ctx.getClientObject().recentScenes.toArray();

        addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.dest_portal")),
            _destPortal = new TextInput());
    }

    override protected function bind () :void
    {
        super.bind();

        BindingUtils.bindSetter(function (o :Object) :void {
            var p :MsoyPortal = (_sprite as PortalSprite).getPortal();
            var item :Object = _destScene.selectedItem;
            if (item != null) {
                p.targetSceneId = (item as SceneBookmarkEntry).sceneId;
                spriteWasTextuallyEdited();
            } else {
                var val :Number = Number(o);
                if (!isNaN(val)) {
                    p.targetSceneId = int(val);
                    spriteWasTextuallyEdited();
                }
            }
        }, _destScene, "text");
        BindingUtils.bindSetter(function (o :Object) :void {
            var val :Number = Number(o);
            if (!isNaN(val)) {
                var p :MsoyPortal = (_sprite as PortalSprite).getPortal();
                p.targetPortalId = int(val);
                spriteWasTextuallyEdited();
            }
        }, _destPortal, "text");
    }

    protected var _destScene :ComboBox;
    protected var _destPortal :TextInput;
}
}
