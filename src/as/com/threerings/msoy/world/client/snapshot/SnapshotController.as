//
// $Id$

package com.threerings.msoy.world.client.snapshot {

import flash.display.BitmapData;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.SecurityErrorEvent;
import flash.net.URLLoader;
import flash.net.URLRequest;
import flash.net.URLRequestMethod;
import flash.utils.ByteArray;

import com.adobe.images.JPGEncoder;

import com.threerings.util.Controller;
import com.threerings.util.StringUtil;
import com.threerings.util.Log;

import com.threerings.whirled.data.Scene;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.client.WorldClient;
import com.threerings.msoy.world.client.WorldContext;

/**
 * Controls the creation of scene snapshots.  Opens a control panel from which the user can choose
 * options for taking a snapshot as an image item and/or a canonical thumbnail for the room.  When
 * the panel is closed, the controller uploads any shots taken to the server.
 */
public class SnapshotController extends Controller
{
    public static const SCENE_THUMBNAIL_SERVICE :String = "/scenethumbsvc";    
    public static const SCENE_SNAPSHOT_SERVICE :String = "/snapshotsvc";
    
    public function SnapshotController (ctx :WorldContext, view :RoomView)
    {
        _view = view;
        _ctx = ctx;
    }

    /**
     * Idempotent method to open a new snapshot panel if one isn't already open.
     */
    public function showPanel () :void 
    {
        if (! _panel) {
            _panel = new SnapshotPanel(this, _ctx, _view);
        }
    }

    /**
     * Idempotent method to discard the current panel.
     */
    public function destroyPanel () :void
    {
        _panel.close();
    }

    /**
     * Return true if this controller is currently showing a panel
     */
    public function isShowing () :Boolean 
    {
        return null != _panel;
    }

    /** 
     * Called by the panel when the user clicks OK. The snapshots taken by snapshotPanel will be
     * uploaded to the server. The canonical one will be used as the new snapshot for the scene. 
     */
    public function doUpload (panel :SnapshotPanel) :void
    {
        if (panel.shouldSaveSceneThumbnail) {
            upload(panel.sceneThumbnail.bitmap, SCENE_THUMBNAIL_SERVICE);
        }
        
        if (panel.shouldSaveGalleryImage) {
            upload(panel.galleryImage.bitmap, SCENE_SNAPSHOT_SERVICE);                
            Log.testing(
                "saving gallery image size: " +
                panel.galleryImage.bitmap.width + "x" +
                panel.galleryImage.bitmap.height);                
            upload(panel.galleryImage.bitmap, SCENE_SNAPSHOT_SERVICE);                
        }
        
        //todo: save the ordinary file here... depends on 
        _ctx.getGameDirector().tutorialEvent("snapshotTaken");
    }
    
    /**
     * Handle the case that the panel is closed but no action is nseeded.
     */
    public function panelClosed () :void 
    {
        _panel = null;
    }
    
    protected function upload (bitmap :BitmapData, service :String) :void    
    {
        const encoder :JPGEncoder = new JPGEncoder(80);
        const mimeBody :ByteArray = makeMimeBody(encoder.encode(bitmap));

        // TODO: display a progress dialog during uploading
        // These should be local, or the dialog is a new thing. Fuck this controller, actually.
        const request :URLRequest = new URLRequest();
        request.url = DeploymentConfig.serverURL + service;
        request.method = URLRequestMethod.POST;
        request.contentType = "multipart/form-data; boundary=" + BOUNDARY;
        request.data = mimeBody;

        const loader :URLLoader = new URLLoader();
        loader.addEventListener(Event.COMPLETE, handleResult);
        loader.addEventListener(IOErrorEvent.IO_ERROR, handleError);
        loader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleError);
        loader.load(request);
    }

    /** Creates an HTTP POST upload request. */
    protected function makeMimeBody (data :ByteArray) :ByteArray
    {
        var memberId :int = _ctx.getMemberObject().memberName.getMemberId();
        var scene :Scene = _ctx.getSceneDirector().getScene();

        const b :String = "--" + BOUNDARY + "\r\n";
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
            "\r\n" + escape(Msgs.WORLD.get("m.sceneItemName", scene.getName())) + "\r\n" + b +
            "Content-Disposition: form-data; name=\"snapshot_plus_thumb\"; " +
            "filename=\"snapshot.jpg\"\r\n" +
            "Content-Type: image/jpeg\r\n" +
            "\r\n");
        output.writeBytes(data);
        output.writeUTFBytes("\r\n--" + BOUNDARY + "--\r\n");
        return output;
    }

    protected function handleError (event :Event) :void
    {
        _ctx.displayFeedback(MsoyCodes.WORLD_MSGS, "e.snap");
    }

    protected function handleResult (event :Event) :void
    {
        _ctx.displayFeedback(MsoyCodes.WORLD_MSGS, "m.snap_success");
    }

    protected var _panel :SnapshotPanel;
    protected var _view :RoomView;
    protected var _ctx :WorldContext;

    protected static const BOUNDARY :String = "why are you reading the raw http stream?";
}
}
