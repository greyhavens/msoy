//
// $Id$

package com.threerings.msoy.room.client.snapshot {

import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.SecurityErrorEvent;
import flash.net.URLLoader;
import flash.net.URLRequest;
import flash.net.URLRequestMethod;
import flash.utils.ByteArray;

import com.threerings.util.Controller;
import com.threerings.util.StringUtil;

import com.threerings.whirled.data.Scene;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.client.RoomView;

/**
 * Controls the creation of scene snapshots.  Opens a control panel from which the user can choose
 * options for taking a snapshot as an image item and/or a canonical thumbnail for the room.  When
 * the panel is closed, the controller uploads any shots taken to the server.
 */
public class SnapshotController extends Controller
{
    public static const SCENE_THUMBNAIL_SERVICE :String = "scenethumbsvc";    
    public static const SCENE_SNAPSHOT_SERVICE :String = "snapshotsvc";
    
    public function SnapshotController (ctx :WorldContext, view :RoomView, panel :SnapshotPanel)
    {
        _ctx = ctx;
        _view = view;
        setControlledPanel(panel);
    }

    /** 
     * Called by the panel when the user clicks OK. The snapshots taken by snapshotPanel will be
     * uploaded to the server. The canonical one will be used as the new snapshot for the scene. 
     */
    public function doUpload (panel :SnapshotPanel) :void
    {
        //todo: save the ordinary file here... depends on 
    }
    
    public function uploadThumbnail (data:ByteArray) :void
    {
        upload(data, SCENE_THUMBNAIL_SERVICE);
    }
    
    public function uploadGalleryImage (data:ByteArray) :void
    {
        upload(data, SCENE_SNAPSHOT_SERVICE);        
    }
    
    protected function upload (data :ByteArray, service :String) :void    
    {
        const mimeBody :ByteArray = makeMimeBody(data);

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
        var itemName :String = StringUtil.truncate(
            Msgs.WORLD.get("m.sceneItemName", scene.getName()), MsoyCodes.MAX_NAME_LENGTH, "...");

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
            "\r\n" + escape(itemName) + "\r\n" + b +
            "Content-Disposition: form-data; name=\"snapshot;furni;thumb\"; " +
            "filename=\"snapshot.jpg\"\r\n" +
            "Content-Type: image/jpeg\r\n" +
            "\r\n");
        output.writeBytes(data);
        output.writeUTFBytes("\r\n--" + BOUNDARY + "--\r\n");
        return output;
    }

    protected function handleError (event :Event) :void
    {
        _panel.uploadError(Msgs.WORLD.get("e.snap"));
    }

    protected function handleResult (event :Event) :void
    {
        // no need to overdo it by providing even more confirmation here
    }

    protected var _ctx :WorldContext;
    protected var _panel :SnapshotPanel;
    protected var _view :RoomView;

    protected static const BOUNDARY :String = "why are you reading the raw http stream?";
}
}
