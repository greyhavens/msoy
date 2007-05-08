//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.events.Event;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.world.data.AudioData;
import com.threerings.msoy.world.data.DecorData;
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
            // make a copy of the scene, so we can edit it
            _editScene = (_ctx.getSceneDirector().getScene() as MsoyScene).clone() as MsoyScene;
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
        trace("FINISH: " + saveSettings);
        if (_panel != null) {

            var origScene :MsoyScene = _ctx.getSceneDirector().getScene() as MsoyScene;
            var origModel :MsoySceneModel = origScene.getSceneModel() as MsoySceneModel;

            var samename :Boolean = Util.equals(_editModel.name, origModel.name);
            var sameaudio :Boolean = Util.equals(_editModel.audioData, origModel.audioData);
            var samedecor :Boolean = Util.equals(_editModel.decorData, origModel.decorData);
            
            // configure an update, if needed
            if (saveSettings) {
                
                if (! (samename && sameaudio && samedecor)) {
                    _ctrl.updateScene(origScene, _editScene);
                }
                
            } else {
                if (! samedecor) {
                    setBackground(origModel.decorData);
                }
                if (! sameaudio) {
                    setBackgroundMusic(origModel.audioData);
                }
            }

            _panel.close();
            _panel = null;
            _editModel = null;
            _editScene = null;
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
    public function setBackground (decordata :DecorData) :void
    {
        _ctrl.roomView.setBackground(decordata);
        sceneModelUpdated();
    }

    /**
     * Called by the panel to preview a new background audio.
     */
    public function setBackgroundMusic (audiodata :AudioData) :void
    {
        _ctrl.roomCtrl.setBackgroundMusic(audiodata);
        sceneModelUpdated();
    }
   
    /**
     * Called by the panel to notify us that the scene model has changed.
     */
    public function sceneModelUpdated () :void
    {
        _ctrl.roomView.setScene(_editScene);
    }


    
    protected var _panel :SettingsPanel;

    protected var _ctx :WorldContext;
    protected var _ctrl :RoomEditController;
    
    protected var _editScene :MsoyScene;
    protected var _editModel :MsoySceneModel;
}
}

