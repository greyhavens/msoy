//
// $Id$

package tutorial {

import flash.display.*;
import flash.events.*;

/**
 * A simple utility class that binds to a MovieClip and then plays scenes of that clip
 * on request, executing a callback method when the scene is finished.
 */
public class ClipHandler
{
    public var scenes :Object;

    public function ClipHandler (clip :MovieClip)
    {
        _clip = clip;

        scenes = new Object();
        for (var ii :int = 0; ii < clip.scenes.length; ii ++) {
            var scene :Scene = _clip.scenes[ii];
            log.debug("Indexing scene: " + scene.name);
            scenes[scene.name] = scene;
        }
    }

    public function unload () :void
    {
        disengage();
    }

    public function gotoScene (sceneName :String, done :Function) :Boolean
    {
        _scene = scenes[sceneName];
        if (_scene) {
            _callback = done;
            _clip.addEventListener(Event.ENTER_FRAME, handleEnterFrame);
            _clip.gotoAndPlay(1, sceneName);
//            log.debug("Playing: " + sceneName);
            return true;
        }
        return false;
    }

    public function disengage () :void
    {
        _callback = null;
        _scene = null;
        _clip.removeEventListener(Event.ENTER_FRAME, handleEnterFrame);
    }

    protected function handleEnterFrame (event :Event) :void
    {
        log.debug("_clip data: [width=" + _clip.width + ", height=" + _clip.height +
                  ", bounds=" + _clip.getBounds(_clip.parent) + "]");

        // if the clip was manipuulated from elsewhere, let's lose interest
        if (_clip.currentScene.name != _scene.name) {
            disengage();
            return;
        }

        // otherwise perhaps we're done?
        if (_clip.currentFrame == _scene.numFrames) {
            log.debug("DONE [scene=" + _scene.name + ", frames=" + _scene.numFrames + "]");
            // if so trigger the callback (if any)
            if (_callback != null) {
                _callback();
            }
            // and stop paying attention
            disengage();
            return;
        }
    }

    protected static const log :Log = Log.getLog(ClipHandler);

    protected var _clip :MovieClip;
    protected var _scene :Scene;
    protected var _callback :Function;
}
}
