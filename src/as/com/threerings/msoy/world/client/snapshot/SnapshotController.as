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

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.client.WorldClient;
import com.threerings.msoy.world.client.WorldContext;

/**
 * Captures RoomView snapshots and sends them over to the server.
 */
public class SnapshotController extends Controller
{
    public static const SCENE_THUMBNAIL_SERVICE :String = "/scenethumbsvc";
    
    // todo: set the actual servlet name for scene snapshots
    public static const SCENE_SNAPSHOT_SERVICE :String = "/snapshotsvc";

    public function SnapshotController (ctx :WorldContext, panel :SnapshotPanel)
    {
        _ctx = ctx;
        setControlledPanel(panel);
    }

//    public function get mediaUrl () :String
//    {
//        return DeploymentConfig.mediaURL + "/snapshot/" + _sceneId + ".jpg";
//    }

    /** Called when after the screenshot panel was closed. If doUpload is true, the snapshots
     *  taken by snapshotPanel will be uploaded to the server.  The canonical one will be
     *  used as the new snapshot for the scene. */
    public function close (doUpload :Boolean, panel :SnapshotPanel, sceneId :int = 0) :void
    {
        if (doUpload) {
            if (panel.shouldSaveSceneThumbnail) {
                upload (panel.sceneThumbnail.bitmap, sceneId, SCENE_THUMBNAIL_SERVICE);
            }
            
            if (panel.shouldSaveGalleryImage) {
                upload(panel.galleryImage.bitmap, sceneId, SCENE_SNAPSHOT_SERVICE);                
            }
            
            //todo: save the ordinary file here... depends on 
            _ctx.getGameDirector().tutorialEvent("snapshotTaken");
        }
        _panel = null;
    }
    
    protected function upload (bitmap :BitmapData, sceneId :int, service :String) :void    
    {
        const encoder :JPGEncoder = new JPGEncoder(80);
        const mimeBody :ByteArray = makeMimeBody(sceneId, encoder.encode(bitmap));

        // TODO: display a progress dialog during uploading
        // These should be local, or the dialog is a new thing. Fuck this controller, actually.
        _request = new URLRequest();
        _request.url = DeploymentConfig.serverURL + service;
        _request.method = URLRequestMethod.POST;
        _request.contentType = "multipart/form-data; boundary=" + BOUNDARY;
        _request.data = mimeBody;

        _loader = new URLLoader();
        _loader.addEventListener(Event.COMPLETE, handleResult);
        _loader.addEventListener(IOErrorEvent.IO_ERROR, handleError);
        _loader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleError);
        _loader.load(_request);
    }

    /** Creates an HTTP POST upload request. */
    protected function makeMimeBody (sceneId :int, data :ByteArray) :ByteArray
    {
        var memberId :int = _ctx.getMemberObject().memberName.getMemberId();

        const b :String = "--" + BOUNDARY + "\r\n";
        var output :ByteArray = new ByteArray();
        output.writeUTFBytes(
            "\r\n" + b +
//            "Content-Disposition: form-data; name=\"auth\"\r\n" +
//            "\r\n" + Prefs.getSessionToken() + "\r\n" + b +
            "Content-Disposition: form-data; name=\"member\"\r\n" +
            "\r\n" + String(memberId) + "\r\n" + b +
            "Content-Disposition: form-data; name=\"scene\"\r\n" +
            "\r\n" + String(sceneId) + "\r\n" + b +
            "Content-Disposition: form-data; name=\"snapshot\"; " +
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
    protected var _encoder :JPGEncoder;
    protected var _loader :URLLoader;
    protected var _request :URLRequest;
    protected var _ctx :WorldContext;

    protected static const BOUNDARY :String = "why are you reading the raw http stream?";
}
}
