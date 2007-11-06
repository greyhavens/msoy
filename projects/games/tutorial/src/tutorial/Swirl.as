//
// $Id$

package tutorial {

import flash.display.MovieClip;
import flash.display.Sprite;
import flash.events.Event;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.utils.ByteArray;

import com.threerings.flash.path.HermiteFunc;
import com.threerings.flash.path.LinePath;
import com.threerings.flash.path.Path;

import com.threerings.util.EmbeddedSwfLoader;

public class Swirl extends Sprite
{
    public function Swirl (view :View, swirlBytes :ByteArray, done :Function)
    {
        var loader :EmbeddedSwfLoader = new EmbeddedSwfLoader();
        loader.addEventListener(Event.COMPLETE, handleSwirlLoaded);
        loader.load(swirlBytes);

        _view = view;
        _done = done;

        _swirlState = View.SWIRL_NONE;
    }

    public function isReady () :Boolean
    {
        return _swirl != null;
    }

    public function unload () :void
    {
        if (_swirlHandler) {
            _swirlHandler.unload();
        }
    }

    public function gotoState (state :int) :void
    {
        this.visible = true;

        var moveFrom :Point = new Point(this.x, this.y);
        var moveTo :Point = null;
        var transScene :String = null;
        var endScene :String = null;

        switch (state) {
        case View.SWIRL_INTRO:
            if (_swirlState == View.SWIRL_NONE) {
                moveFrom = BIG_SWIRL_LOC;
            } else {
                log.warning("Unexpected transtion [from=" + _swirlState + ", to=" + state + "].");
            }
            moveTo = BIG_SWIRL_LOC;
            transScene = SCN_BIG_APPEAR;
            break;

        case View.SWIRL_DEMURE:
            if (_swirlState == View.SWIRL_NONE) {
                moveFrom = SMALL_SWIRL_LOC;
                transScene = SCN_SMALL_APPEAR;
            } else if (_swirlState == View.SWIRL_INTRO) {
                transScene = SCN_MINIMIZE;
            }
            moveTo = SMALL_SWIRL_LOC;
            endScene = SCN_IDLE;
            break;

        case View.SWIRL_BOUNCY:
            if (_swirlState == View.SWIRL_INTRO) {
                log.warning("Unexpected transtion [from=" + _swirlState + ", to=" + state + "].");
            }
            endScene = SCN_LOOKATME;
            break;

        case View.SWIRL_NONE:
            this.visible = false;
            break;

        default:
            log.warning("Can't goto unknown swirl state [state=" + state + "].");
            return;
        }

        if (moveTo && transScene) {
            var path :Path = new LinePath(this, new HermiteFunc(moveFrom.x, moveTo.x),
                                          new HermiteFunc(moveFrom.y, moveTo.y), 400);
            if (endScene) {
                path.setOnComplete(function (path :Path) :void {
                    // make sure we are still relevant after the delay
                    if (_swirlState == state) {
                        _swirlHandler.gotoScene(endScene);
                    }
                });
            }
            path.start();
            _swirlHandler.gotoScene(transScene);

        } else {
            if (moveTo) {
                this.x = moveTo.x;
                this.y = moveTo.y;
            }
            if (endScene) {
                _swirlHandler.gotoScene(endScene);
            }
        }

        _swirlState = state;
    }

    protected function handleSwirlLoaded (evt :Event) :void
    {
        var swirlClip :MovieClip = MovieClip(EmbeddedSwfLoader(evt.target).getContent());
        _swirl = new Buttonizer(swirlClip);
        _swirl.addEventListener(MouseEvent.CLICK, function (evt :Event) :void {
            _view.swirlClicked(_swirlState);
        });

        _swirl.x =  -Content.SWIRL_OFFSET.x;
        _swirl.y =  -Content.SWIRL_OFFSET.y;
        addChild(_swirl);

        _swirlHandler = new ClipHandler(swirlClip);
        this.visible = false;
        _done();
    }

    protected var _view :View;
    protected var _done :Function;

    protected var _swirl :Sprite;
    protected var _swirlHandler :ClipHandler;
    protected var _swirlState :int;

    protected static const log :Log = Log.getLog(Swirl);

    protected static const SMALL_SWIRL_LOC :Point = new Point(75, 75);
    protected static const BIG_SWIRL_LOC :Point = new Point(200, 200);

    protected static const SCN_BIG_APPEAR :String = "appear_text";
    protected static const SCN_SMALL_APPEAR :String = "appear_no_text";
    protected static const SCN_MINIMIZE :String = "minimize";
    protected static const SCN_IDLE :String = "idle";
    protected static const SCN_LOOKATME :String = "lookatme";
    protected static const SCN_GOODJOB :String = "goodjob";
}
}

