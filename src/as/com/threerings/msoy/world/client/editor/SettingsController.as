//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.events.Event;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.world.data.AudioData;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.util.Controller;
import com.threerings.util.Util;


/**
 * Handles actions performed on the room settings panel.
 */
public class SettingsController extends Controller
{
    public function SettingsController (ctx :WorldContext, ctrl :RoomEditController)
    {
        _ctx = ctx;
        _ctrl = ctrl;
    }

    /**
     * Opens and initializes the settings panel.
     */
    public function start () :void
    {
        if (_panel == null) {

            _origScene = _ctx.getSceneDirector().getScene() as MsoyScene;

            // make a copy of the scene, so we can edit it
            _editScene = _origScene.clone() as MsoyScene;
            _editModel = _editScene.getSceneModel() as MsoySceneModel;

            _panel = new SettingsPanel(_ctx, _editModel, this);
            _panel.open();
        }
    }

    /**
     * Close the settings panel and, optionally, save the new settings.
     */
    public function finish (saveSettings :Boolean) :void
    {
        if (_panel != null) {

            var origModel :MsoySceneModel = _origScene.getSceneModel() as MsoySceneModel;

            var samename :Boolean = Util.equals(_editModel.name, origModel.name);
            var sameaudio :Boolean = Util.equals(_editModel.audioData, origModel.audioData);
            var samedecor :Boolean = Util.equals(_editModel.decor.itemId, origModel.decor.itemId);
            
            // configure an update, if needed
            if (saveSettings) {
                
                if (! (samename && sameaudio && samedecor)) {
                    _ctrl.updateScene(_origScene, _editScene);
                }
                
            } else {
                if (! samedecor) {
                    setBackground(origModel.decor);
                }
                if (! sameaudio) {
                    setBackgroundMusic(origModel.audioData);
                }

                _ctrl.roomView.setScene(_origScene);
                _ctrl.roomView.rereadScene();
            }
                
            _panel.close();
            _panel = null;
            _editModel = null;
            _editScene = null;
            _origScene = null;
        }
    }

    /**
     * Called by the panel to update the room name.
     */
    public function setName (name :String) :void
    {
        _editModel.name = name;
        _panel.updateInputFields();
    }

    /**
     * Called by the panel to update the room background music volume.
     */
    public function setVolume (volume :Number) :void
    {
        _editModel.audioData.volume = volume;
        setBackgroundMusic(_editModel.audioData);
    }

    /**
     * Called by the panel to specify a new background sprite for preview.
     */
    public function setBackground (decor :Decor) :void
    {
        _ctrl.roomView.setScene(_editScene);
        _ctrl.roomView.setBackground(decor);
    }

    /**
     * Called by the panel to preview a new background audio.
     */
    public function setBackgroundMusic (audiodata :AudioData) :void
    {
        _ctrl.roomView.setScene(_editScene);
        _ctrl.roomCtrl.setBackgroundMusic(audiodata);
    }
   
    protected var _panel :SettingsPanel;

    protected var _ctx :WorldContext;
    protected var _ctrl :RoomEditController;

    protected var _origScene :MsoyScene;
    protected var _editScene :MsoyScene;
    protected var _editModel :MsoySceneModel;
}
}

