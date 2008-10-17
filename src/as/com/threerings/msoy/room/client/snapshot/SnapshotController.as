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

    public function cancelUpload () :void
    {
        if (_loader != null) {
            try {
                _loader.close();
            } catch (e :Error) {
                // ignore
            }
            clearLoader();
        }
    }

    public function upload (
        data :ByteArray, service :String, createItem :Boolean, doneFn :Function) :void    
    {
        const mimeBody :ByteArray = makeMimeBody(data, createItem);

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

    protected function handleError (event :Event) :void
    {
        _panel.uploadError(Msgs.WORLD.get("e.snap"));
        clearLoader();
    }

    protected function handleSuccess (event :Event) :void
    {
        var fn :Function = _doneFn;
        // TODO: do the download, if applicable
//        trace("Data: " + _loader.data);
        clearLoader();
        fn();
    }

    protected function clearLoader () :void
    {
        _loader = null;
        _doneFn = null;
    }

    protected var _ctx :WorldContext;
    protected var _panel :SnapshotPanel;
    protected var _view :RoomView;

    /** A function to call when we're done successfully uploading. */
    protected var _doneFn :Function;

    /** The currently operating uploader. */
    protected var _loader :URLLoader;

    protected static const BOUNDARY :String = "why are you reading the raw http stream?";
}
}
