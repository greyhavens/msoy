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
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.Snapshottable;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.client.OccupantSprite;
import com.threerings.msoy.room.client.RoomView;

/**
 * Represents a particular snapshot
 */ 
public class Snapshot extends EventDispatcher
{    
    public static const THUMBNAIL_WIDTH :int = 350;
    public static const THUMBNAIL_HEIGHT :int = 200;

    public var bitmap :BitmapData;

    public const log :Log = Log.getLog(this);

    /**
     * Convenience method to create the thumbnail Snapshot.
     */
    public static function createThumbnail (
        ctx :WorldContext, view :RoomView, handleComplete :Function, handleError :Function)
        :Snapshot
    {
        const frame :Rectangle = new Rectangle(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
        const framer :Framer = new CanonicalFramer(view.getScrollBounds(), frame,
            view.getScrollOffset());
        return new Snapshot(ctx, true, view, framer, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT,
            handleComplete, handleError);
    }

    /**
     * Create a 'Snapshot' of the provided view.  With a frame of the provided size.
     *
     * @param handleCompleteFn informed when *encoding* is complete.
     * @param handleErrorFn informed when *uploading* errors.
     */
    public function Snapshot (
        ctx :WorldContext, thumbnail :Boolean, view :RoomView, framer :Framer,
        width :int, height :int, handleCompleteFn :Function, handleErrorFn :Function)
    {
        _ctx = ctx;
        _view = view;
        _thumbnail = thumbnail;
                
        _frame = new Rectangle(0, 0, width, height);
        _framer = framer;
        bitmap = new BitmapData(width, height);

        addEventListener(Event.COMPLETE, handleCompleteFn);
        addEventListener(IOErrorEvent.IO_ERROR, handleErrorFn);
        addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleErrorFn);
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
        includeOccupants :Boolean, includeChat :Boolean, doEncode :Boolean) :Boolean
    {
        cancelEncoding();

        // first let's fill the bitmap with black or something
        bitmap.fillRect(_frame, 0x000000);

        var occPredicate :Function = null;
        if (!includeOccupants) {
            occPredicate = function (child :DisplayObject) :Boolean {
                return !(child is OccupantSprite);
            };
        }

        var matrix :Matrix = _framer.getMatrix();

        // first snapshot the room
        var allSuccess :Boolean = _view.snapshot(bitmap, matrix, occPredicate);

        // then, add the overlays
        // find the layered container...
        var d :DisplayObject = _view;
        while (!(d is LayeredContainer) && d.parent != null) {
            d = d.parent;
        }
        if (d is LayeredContainer) {
            var lc :LayeredContainer = LayeredContainer(d);
            var layerPredicate :Function = function (child :DisplayObject) :Boolean {
                // if it's not even a layer, we must be further down: include
                if (!lc.containsOverlay(child)) {
                    return true;
                }
                // blacklist certain layers
                switch (lc.getLayer(child)) {
                default:
                    return true;

                case PlaceBox.LAYER_ROOM_SPINNER:
                case PlaceBox.LAYER_CHAT_LIST:
                case PlaceBox.LAYER_TRANSIENT:
                case PlaceBox.LAYER_FEATURED_PLACE:
                    return false;

                case PlaceBox.LAYER_CHAT_SCROLL:
                case PlaceBox.LAYER_CHAT_STATIC:
                case PlaceBox.LAYER_CHAT_HISTORY:
                    return includeChat;
                }
            };

            if (!lc.snapshot(bitmap, matrix, layerPredicate)) {
                allSuccess = false;
            }
        }

        _data = null; // clear old encoded data
        if (doEncode) {
            startEncode();
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

    public function upload (createItem :Boolean = false, doneFn :Function = null) :void
    {
        const mimeBody :ByteArray = makeMimeBody(_data, createItem);

        const request :URLRequest = new URLRequest();
        request.url = DeploymentConfig.serverURL +
            (_thumbnail ? THUMBNAIL_SERVICE : SNAPSHOT_SERVICE);
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
    
    protected function handleJpegEncoded (event :ValueEvent) :void
    {
        log.debug("jpeg encoded");
        _data = ByteArray(event.value);
        _encoder = null;

        dispatchEvent(new Event(Event.COMPLETE));

        // call whatever we're supposed to call with the jpeg data now that we have it
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
        // re-dispatch it
        dispatchEvent(event);
        clearLoader();
    }

    protected function handleSuccess (event :Event) :void
    {
        var fn :Function = _doneFn;
        var data :String = String(_loader.data);
        clearLoader();
        if (fn != null) {
            fn(data);
        }
    }

    protected function clearLoader () :void
    {
        _loader = null;
        _doneFn = null;
    }

    protected var _ctx :WorldContext;
    protected var _thumbnail :Boolean;

    protected var _encoder :BackgroundJPGEncoder;

    protected var _data :ByteArray;

    protected var _uploadOperation :Function;
    protected var _args :Array;
    
    protected var _view :RoomView;
    protected var _frame :Rectangle;
    protected var _framer :Framer;

   /** The currently operating uploader. */
    protected var _loader :URLLoader;
    protected var _doneFn :Function;

    protected static const THUMBNAIL_SERVICE :String = "scenethumbsvc";
    protected static const SNAPSHOT_SERVICE :String = "snapshotsvc";

    protected static const BOUNDARY :String = "why are you reading the raw http stream?";
}
}
