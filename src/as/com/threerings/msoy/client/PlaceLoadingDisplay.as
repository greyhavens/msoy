//
// $Id$

package com.threerings.msoy.client {

import flash.display.LoaderInfo;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.ErrorEvent;
import flash.events.IEventDispatcher;
import flash.events.IOErrorEvent;
import flash.events.ProgressEvent;
import flash.events.SecurityErrorEvent;

import flash.utils.Dictionary;

import caurina.transitions.Tweener;

import mx.events.ResizeEvent;

import com.threerings.msoy.client.PlaceBox;

import com.threerings.msoy.ui.LoadingSpinner;

public class PlaceLoadingDisplay extends Sprite
    implements LoadingWatcher
{
    public function PlaceLoadingDisplay (box :PlaceBox)
    {
        _box = box;
        _box.addEventListener(ResizeEvent.RESIZE, handleBoxResized);
        addChild(_spinner = new LoadingSpinner());
    }

    /**
     * Has everything finished loading?
     */
    public function isFinished () :Boolean
    {
        return (_primary == null) && (_secondaryCount == 0);
    }

    // from interface LoadingWatcher
    public function watchLoader (
        info :LoaderInfo, unloadie :IEventDispatcher, isPrimary :Boolean = false) :void
    {
        _unloadies[info] = unloadie;
        unloadie.addEventListener(Event.UNLOAD, handleUnload);

        info.addEventListener(Event.COMPLETE, handleComplete);
        info.addEventListener(IOErrorEvent.IO_ERROR, handleError);
        info.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleError);

        if (isPrimary) {
            _primary = info;
            info.addEventListener(ProgressEvent.PROGRESS, handleProgress);

            doStaticLayout();
            _spinner.setProgress(0, 1);

        } else {
            if (_secondaryCount == 0) {
                _spinner.setProgress(); // put in "indeterminite" mode
                doTransitionOut();
            }

            _secondaryCount++;
        }

        // make sure we're showing
        if (parent == null) {
            _box.addOverlay(this, PlaceBox.LAYER_ROOM_SPINNER);
        }
    }

    protected function unwatchLoader (info :LoaderInfo) :void
    {
        var ed :IEventDispatcher = IEventDispatcher(_unloadies[info]);
        if (ed == null) {
            // due to shite in MediaContainer, we may get both an ERROR and UNLOAD,
            // so just avoid processing things twice.
            return;
        }

        ed.removeEventListener(Event.UNLOAD, handleUnload);
        delete _unloadies[info];

        info.removeEventListener(Event.COMPLETE, handleComplete);
        info.removeEventListener(IOErrorEvent.IO_ERROR, handleError);
        info.removeEventListener(SecurityErrorEvent.SECURITY_ERROR, handleError);

        if (info == _primary) {
            info.removeEventListener(ProgressEvent.PROGRESS, handleProgress);
            _primary = null;

        } else {
            _secondaryCount--;
        }

        if (isFinished() && (parent != null)) {
            _box.removeOverlay(this);
        }
    }

    protected function handleBoxResized (event :ResizeEvent) :void
    {
        if (_primary != null) {
            doStaticLayout();
        }
    }

    /** Called to lay out the static loader, while it's counting up the loaded percentage. */ 
    protected function doStaticLayout () :void
    {
        this.x = 0;
        this.y = 0;
        
        _spinner.scaleX = 1;
        _spinner.scaleY = 1;
        _spinner.x = (_box.width - LoadingSpinner.WIDTH) / 2;
        _spinner.y = (_box.height - LoadingSpinner.HEIGHT) / 2;
    }
    
    /** 
     * Called to perform a transition away from static loading, 
     * eg. by sliding the loader out of view. 
     */
    protected function doTransitionOut () :void
    {
        const DURATION :Number = 1;
        const TRANS :String = "easeoutcubic";
        Tweener.addTween(_spinner, { x: 20, y: 20, scaleX: .4, scaleY: .4, 
            time: DURATION, transition: TRANS });
   
    }

    protected function handleComplete (event :Event) :void
    {
        unwatchLoader(event.target as LoaderInfo);
    }

    protected function handleError (event :ErrorEvent) :void
    {
        unwatchLoader(event.target as LoaderInfo);
    }

    protected function handleProgress (event :ProgressEvent) :void
    {
        _spinner.setProgress(event.bytesLoaded, event.bytesTotal);
    }

    protected function handleUnload (event :Event) :void
    {
        // since this is the uncommon case, let's just iterate and find the right LoaderInfo
        for (var key :* in _unloadies) {
            if (_unloadies[key] == event.target) {
                unwatchLoader(LoaderInfo(key));
                return;
            }
        }
    }

    protected var _box :PlaceBox;

    protected var _primary :LoaderInfo

    protected var _secondaryCount :int;

    protected var _spinner :LoadingSpinner;

    protected var _unloadies :Dictionary = new Dictionary(true);
}
}
