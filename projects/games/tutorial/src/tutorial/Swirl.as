//
// $Id$

package tutorial {

import flash.display.*;
import flash.text.*;
import flash.geom.*;
import flash.events.*;
import flash.filters.*;
import flash.net.*;
import flash.ui.*;
import flash.utils.*;

import com.threerings.util.EmbeddedSwfLoader;
import com.threerings.flash.SimpleTextButton;

import com.threerings.flash.path.*;

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

    protected static const SMALL_SWIRL_LOC :Point = new Point(75, 75);
    protected static const BIG_SWIRL_LOC :Point = new Point(200, 200);

    public function gotoState (state :int) :void
    {
        this.visible = true;

        var transition :String = null;

        var x :Number, y :Number;

        x = SMALL_SWIRL_LOC.x;
        y = SMALL_SWIRL_LOC.y;

        switch(state) {
        case View.SWIRL_INTRO:
            if (_swirlState != View.SWIRL_NONE) {
                log.warning("Unexpected transtion [from=" + _swirlState + ", to=" +
                            state + "]");
            }
            x = BIG_SWIRL_LOC.x;
            y = BIG_SWIRL_LOC.y;

            transition = SCN_BIG_APPEAR;
            break;
        case View.SWIRL_DEMURE:
            if (_swirlState == View.SWIRL_NONE) {
                transition = SCN_SMALL_APPEAR;

            } else if (_swirlState == View.SWIRL_INTRO) {
                transition = SCN_MINIMIZE;

            } else {
                transition = SCN_IDLE;
            }
            break;
        case View.SWIRL_BOUNCY:
            if (_swirlState == View.SWIRL_INTRO) {
                log.warning("Unexpected transtion [from=" + _swirlState + ", to=" +
                            state + "]");
            }
            transition = SCN_LOOKATME;
            break;
        default:
            log.warning("Can't goto unknown swirl state [state=" + state + "]");
            return;
        }

        _swirlHandler.gotoScene(transition);

        if (transition == SCN_MINIMIZE) {
            var path :Path = new LinePath(
                this, new HermiteFunc(BIG_SWIRL_LOC.x, SMALL_SWIRL_LOC.x),
                new HermiteFunc(BIG_SWIRL_LOC.y, SMALL_SWIRL_LOC.y), 400);
            path.setOnComplete(function (path :Path) :void {
                if (_swirlState == View.SWIRL_DEMURE) {
                    _swirlHandler.gotoScene(SCN_IDLE);
                }
            });
            path.start();
        } else {
            this.x = x;
            this.y = y;
        }

        _swirlState = state;
    }

    protected function handleSwirlLoaded (evt :Event) :void
    {
        var swirlClip :MovieClip = MovieClip(EmbeddedSwfLoader(evt.target).getContent());
        _swirl = new Buttonizer(swirlClip);
        _swirl.addEventListener(MouseEvent.CLICK, function (evt :Event) :void {
            log.debug("Swirly clicked!");
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

    protected static const SCN_BIG_APPEAR :String = "appear_text";
    protected static const SCN_SMALL_APPEAR :String = "appear_no_text";
    protected static const SCN_MINIMIZE :String = "minimize";
    protected static const SCN_IDLE :String = "idle";
    protected static const SCN_LOOKATME :String = "lookatme";
    protected static const SCN_GOODJOB :String = "goodjob";
}
}

