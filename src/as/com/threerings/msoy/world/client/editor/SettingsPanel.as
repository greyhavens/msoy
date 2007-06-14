//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.events.Event;

import mx.binding.utils.BindingUtils;
import mx.containers.Grid;
import mx.controls.HSlider;
import mx.controls.Spacer;
import mx.controls.TextInput;

import com.threerings.flex.GridUtil;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.item.client.InventoryLoader;
import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MsoyUI;
import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.client.editor.SingleItemSelector;
import com.threerings.msoy.world.data.AudioData;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;


/**
 * A panel for editing general room settings.
 */
public class SettingsPanel extends FloatingPanel
{
    public function SettingsPanel (
        ctx :WorldContext, sceneModel :MsoySceneModel, ctrl :SettingsController)
    {
        super(ctx, Msgs.EDITING.get("t.room_settings"));
        _ctrl = ctrl;
        _sceneModel = sceneModel;
    }

    // from superclass
    override protected function createChildren () :void
    {
        super.createChildren();
        
        var general :Grid = new Grid();
        GridUtil.addRow(general,
                        MsoyUI.createLabel(Msgs.EDITING.get("l.scene_name")),
                        _name = new TextInput());

        GridUtil.addRow(
            general, MsoyUI.createLabel(Msgs.EDITING.get("l.background_image")),
            _decorSelector = new SingleItemSelector(_ctx, Item.DECOR));

        _decorSelector.selectionChanged = newBackgroundImageSelected;
        
        _decorItemLoader = new InventoryLoader(_ctx, Item.DECOR);
        _decorItemLoader.addEventListener(InventoryLoader.SUCCESS, updateInitialDecorSelection);
        _decorItemLoader.start();

        GridUtil.addRow(
            general, MsoyUI.createLabel(Msgs.EDITING.get("l.background_audio")),
            _audioSelector = new SingleItemSelector(_ctx, Item.AUDIO));

        _audioSelector.selectionChanged = newBackgroundAudioSelected;

        _audioItemLoader = new InventoryLoader(_ctx, Item.AUDIO);
        _audioItemLoader.addEventListener(InventoryLoader.SUCCESS, updateInitialAudioSelection);
        _audioItemLoader.start();

        GridUtil.addRow(general, MsoyUI.createLabel(Msgs.EDITING.get("l.volume")),
                        _volume = new HSlider());
        _volume.minimum = 0;
        _volume.maximum = 1;
        _volume.percentWidth = 100;
        _volume.liveDragging = true;

        var spacer :Spacer = new Spacer();
        spacer.height = 20;
        GridUtil.addRow(general, spacer, [2, 1]);

        addChild(general);

        addButtons(OK_BUTTON, CANCEL_BUTTON);
    }

    // from superclass
    override protected function childrenCreated () :void
    {
        super.childrenCreated();

        updateInputFields();

        BindingUtils.bindSetter(function (name :String) :void {
                _ctrl.setName(name);
            }, _name, "text");

        BindingUtils.bindSetter(function (o :Object) :void {
                _ctrl.setVolume(Number(o));
            }, _volume, "value");
    }

    // from superclass
    override protected function buttonClicked (buttonId :int) :void
    {
        _ctrl.finish(buttonId == OK_BUTTON);
        
        // note: we don't let the parent process this action. the controller will take care
        // of closing the panel. 
    }

    
    /**
     * Once decor finished loading, set the current displayed decor
     * to whatever was stored in the model.
     */
    protected function updateInitialDecorSelection (event :Event) :void
    {
        pickItemInSelector (
            Item.DECOR, _sceneModel.decor != null ? _sceneModel.decor.itemId : 0,
            updateInitialDecorSelection, _decorSelector);
    }

    /**
     * Once audio finished loading, display its info.
     */
    protected function updateInitialAudioSelection (event :Event) :void
    {
        pickItemInSelector (
            Item.AUDIO, _sceneModel.audioData != null ? _sceneModel.audioData.itemId : 0,
            updateInitialAudioSelection, _audioSelector);
    }

    /** Common functionality for selecting an item out based on its Id. */
    protected function pickItemInSelector (
        itemType :int, itemId :int, handlerFn :Function, selector :SingleItemSelector) :void
    {
        var memObj :MemberObject = _ctx.getMemberObject();
        if (memObj.isInventoryLoaded(itemType)) {
            _decorItemLoader.removeEventListener(InventoryLoader.SUCCESS, handlerFn);
            if (itemId != 0) {
                var items :Array = memObj.getItems(itemType);  
                for each (var item :Item in items) {
                    if (item.itemId == itemId) {
                        selector.setSelectedItem(item);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Set the current displayed values to those in the model.
     */
    public function updateInputFields () :void
    {
        _name.text = _sceneModel.name;
        _volume.value = _sceneModel.audioData.volume; 
    }
    
    protected function newBackgroundImageSelected () :void
    {
        var decor :Decor = (_decorSelector.getSelectedItem()) as Decor;

        if (decor == null) {
            decor = MsoySceneModel.defaultMsoySceneModelDecor();
        }

        _ctrl.setBackground(decor);
    }

    protected function newBackgroundAudioSelected () :void
    {
        var audio :Audio = _audioSelector.getSelectedItem() as Audio;
        var ad :AudioData = _sceneModel.audioData;
        if (audio != null) {
            ad.itemId = audio.itemId;
            ad.media = audio.audioMedia;
            
        } else {
            // user cleared the background audio - clear the media id
            ad.itemId = 0;
        }
        
        _ctrl.setBackgroundMusic(ad);
    }

    protected var _ctrl :SettingsController;
    protected var _sceneModel :MsoySceneModel;

    protected var _name :TextInput;
    protected var _decorSelector :SingleItemSelector;
    protected var _audioSelector :SingleItemSelector;
    protected var _volume :HSlider;

    protected var _decorItemLoader :InventoryLoader;
    protected var _audioItemLoader :InventoryLoader;

}
}
