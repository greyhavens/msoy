package com.threerings.msoy.room.client.snapshot {

import com.threerings.util.Log;

import flash.display.BitmapData;
import flash.display.DisplayObject;

import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.EventDispatcher;
import flash.events.IOErrorEvent;
import flash.events.SecurityErrorEvent;

import flash.geom.Matrix;
import flash.geom.Rectangle;

import flash.net.URLLoader;
import flash.net.URLRequest;
import flash.net.URLRequestMethod;

import flash.utils.ByteArray;

import com.threerings.flash.BackgroundJPGEncoder;

import com.threerings.util.Log;
import com.threerings.util.StringUtil;
import com.threerings.util.ValueEvent;

import com.threerings.whirled.data.Scene;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.LayeredContainer;
import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.client.OccupantSprite;
import com.threerings.msoy.room.client.RoomElement;
import com.threerings.msoy.room.client.RoomView;

/**
 * Represents a particular snapshot
 */ 
public class Snapshot extends EventDispatcher
{    
    public static const SCENE_THUMBNAIL_SERVICE :String = "scenethumbsvc";
    public static const SCENE_SNAPSHOT_SERVICE :String = "snapshotsvc";


    public var bitmap :BitmapData;

    public const log :Log = Log.getLog(this);

    /**
     * Create a 'Snapshot' of the provided view.  With a frame of the provided size.
     */
    public function Snapshot (
        ctx :WorldContext, view :RoomView, panel :SnapshotPanel,
        framer :Framer, width :int, height :int)
    {
        _ctx = ctx;
        _view = view;
        _panel = panel;
                
        _frame = new Rectangle(0, 0, width, height);
        _framer = framer;
        bitmap = new BitmapData(width, height);
    }

    public function get ready () :Boolean
    {
        return (_data != null);
    }

    public function startEncode () :void
    {
        if (!ready && _encoder == null) {
            _encoder = new BackgroundJPGEncoder(bitmap, 70);
            _encoder.addEventListener("complete", handleJpegEncoded);
            _encoder.start();
        }
    }

    /**
     * Update the snapshot.
     */
    public function updateSnapshot (
        includeOccupants :Boolean, includeOverlays :Boolean, doEncode :Boolean) :Boolean
    {
        cancelEncoding();

        // first let's fill the bitmap with black or something
        bitmap.fillRect(_frame, 0x000000);

        // draw the room, scaling down to the appropriate size
        var allSuccess :Boolean = true;

        if (!renderChildren(includeOccupants)) {
            allSuccess = false;
        }

        if (includeOverlays) {
            renderOverlays();
        }

        _data = null;

        if (doEncode) {
            startEncode();
        }

        return allSuccess;
    }
    
    /**
     * Render the overlays 
     */ 
    protected function renderOverlays () :void {        
        var d :DisplayObject = _view;
        
        // search up through the containment hierarchy until you find the LayeredContainer
        // or the end of the hierarchy
        while (!(d is LayeredContainer) && d.parent != null) {
            d = d.parent;
        }
        if (d is LayeredContainer) {
            (d as LayeredContainer).snapshotOverlays(bitmap, _framer);
        }
    }

    /**
     * Render the children
     */
    protected function renderChildren (includeOccupants :Boolean) :Boolean 
    {
        var allSuccess:Boolean = true;
        
        for (var ii :int = 0; ii < _view.numChildren; ii++) {
            var child :DisplayObject = _view.getChildAt(ii);
            if (!includeOccupants && (child is OccupantSprite)) {
                continue; // skip it!
            }
            
            var matrix :Matrix = child.transform.matrix; // makes a clone...
            _framer.applyTo(matrix); // apply the framing transformation to the matrix

            if (child is RoomElement) {
                var success :Boolean = RoomElement(child).snapshot(bitmap, matrix);
                allSuccess &&= success;

            } else {
                try {
                    bitmap.draw(child, matrix, null, null, null, true);

                } catch (err :SecurityError) {
                    // not a critical error
                    log.info("Unable to snapshot Room element", err);
                    allSuccess = false;
                }
            }            
        }
        return allSuccess;
    }

    public function cancelAll () :void
    {
        if (_loader != null) {
            try {
                _loader.close();
            } catch (e :Error) {
                //ignore
            }
            clearLoader();
        }
        cancelEncoding();
    }

    /**
     * Cancel encoding if it's underway.  We don't cancel uploads.
     */
    public function cancelEncoding () :void
    {
        if (_encoder) {
            _encoder.cancel();
            _encoder = null;
        }
    }

    protected function handleJpegEncoded (event :ValueEvent) :void
    {
        log.debug("jpeg encoded");
        _data = ByteArray(event.value);
        _encoder = null;

        dispatchEvent(new Event(Event.COMPLETE));

        // call whatever we're supposed to call with the jpeg data now that we have it
    }

    public function upload (service :String, createItem :Boolean, doneFn :Function) :void
    {
        const mimeBody :ByteArray = makeMimeBody(_data, createItem);

        const request :URLRequest = new URLRequest();
        request.url = DeploymentConfig.serverURL + service;
        request.method = URLRequestMethod.POST;
        request.contentType = "multipart/form-data; boundary=" + BOUNDARY;
        request.data = mimeBody;

        _doneFn = doneFn;
        _loader = new URLLoader();
        _loader.addEventListener(Event.COMPLETE, handleSuccess);
        _loader.addEventListener(IOErrorEvent.IO_ERROR, handleError);
        _loader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleError);
        _loader.load(request);
    }

    /** Creates an HTTP POST upload request. */
    protected function makeMimeBody (data :ByteArray, createItem :Boolean) :ByteArray
    {
        var memberId :int = _ctx.getMemberObject().memberName.getMemberId();
        var scene :Scene = _ctx.getSceneDirector().getScene();
        var itemName :String = StringUtil.truncate(
            Msgs.WORLD.get("m.sceneItemName", scene.getName()), MsoyCodes.MAX_NAME_LENGTH, "...");

        const b :String = "--" + BOUNDARY + "\r\n";
        const mediaIds :String = "snapshot" + (createItem ? ";furni;thumb" : "");
        var output :ByteArray = new ByteArray();
        output.writeUTFBytes(
            "\r\n" + b +
//            "Content-Disposition: form-data; name=\"auth\"\r\n" +
//            "\r\n" + Prefs.getSessionToken() + "\r\n" + b +
            "Content-Disposition: form-data; name=\"member\"\r\n" +
            "\r\n" + String(memberId) + "\r\n" + b +
            "Content-Disposition: form-data; name=\"scene\"\r\n" +
            "\r\n" + String(scene.getId()) + "\r\n" + b +
            "Content-Disposition: form-data; name=\"name\"\r\n" +
            "\r\n" + escape(itemName) + "\r\n" + b +
            "Content-Disposition: form-data; name=\"makeItem\"\r\n" +
            "\r\n" + createItem + "\r\n" + b +
            "Content-Disposition: form-data; name=\"" + mediaIds + "\"; " +
            "filename=\"snapshot.jpg\"\r\n" +
            "Content-Type: image/jpeg\r\n" +
            "\r\n");
        output.writeBytes(data);
        output.writeUTFBytes("\r\n--" + BOUNDARY + "--\r\n");
        return output;
    }

    protected function handleError (event :ErrorEvent) :void
    {
        _panel.reportError(Msgs.WORLD.get("e.snap_upload", event.text));
        clearLoader();
    }

    protected function handleSuccess (event :Event) :void
    {
        trace("Done: Loader is " + _loader);
        var fn :Function = _doneFn;
        var data :String = String(_loader.data);
        clearLoader();
        fn(data);
    }

    protected function clearLoader () :void
    {
        _loader = null;
        _doneFn = null;
    }

    protected var _ctx :WorldContext;

    protected var _encoder :BackgroundJPGEncoder;

    protected var _panel :SnapshotPanel;

    protected var _data :ByteArray;

    protected var _uploadOperation :Function;
    protected var _args :Array;
    
    protected var _view :RoomView;
    protected var _frame :Rectangle;
    protected var _framer :Framer;

   /** The currently operating uploader. */
    protected var _loader :URLLoader;
    protected var _doneFn :Function;

    protected static const BOUNDARY :String = "why are you reading the raw http stream?";
}
}
