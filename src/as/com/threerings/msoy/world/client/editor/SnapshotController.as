//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.BitmapData;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.SecurityErrorEvent;
import flash.net.URLLoader;
import flash.net.URLRequest;
import flash.net.URLRequestMethod;
import flash.utils.ByteArray;

import com.adobe.images.JPGEncoder;

import com.threerings.util.StringUtil;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.client.WorldClient;
import com.threerings.msoy.world.client.WorldContext;

/**
 * Captures RoomView snapshots and sends them over to the server.
 */
public class SnapshotController
{
    public static const SERVICE_ENTRY_POINT :String = "/snapshotsvc";

    public function SnapshotController (ctx :WorldContext, sceneId :int)
    {
        _ctx = ctx;
        _sceneId = sceneId;

        _request = new URLRequest();
        _request.url = DeploymentConfig.serverURL + SERVICE_ENTRY_POINT;
        _request.method = URLRequestMethod.POST;
        _request.contentType = "multipart/form-data; boundary=" + BOUNDARY;

        _loader = new URLLoader();
        _loader.addEventListener(Event.COMPLETE, handleResult);
        _loader.addEventListener(IOErrorEvent.IO_ERROR, handleError);
        _loader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleError);

        _encoder = new JPGEncoder(80);
    }

    public function get sceneId () :int
    {
        return _sceneId;
    }

    public function get mediaUrl () :String
    {
        return DeploymentConfig.mediaURL + "/snapshot/" + _sceneId + ".jpg";
    }


    /** Pops up a UI that will ask the user to apply or cancel the current room screenshot. */
    public function takeScreenshot (view :RoomView) :void
    {
        if (_panel == null) {
            _panel = new SnapshotPanel(_ctx, this, view);
            _panel.open();
        }
    }

    /** Called when after the screenshot panel was closed. If bitmap is not null,
     *  it will be uploaded to the server as the new screenshot for the scene. */
    public function close (bitmap :BitmapData) :void
    {
        if (bitmap != null) {
            // encodes bitmap image as a MIME file upload, and sends it over
            _request.data = makeMimeBody(sceneId, _encoder.encode(bitmap));
            _loader.load(_request);

            _ctx.getGameDirector().tutorialEvent("snapshotTaken");
        }
        _panel = null;
    }

    /** Creates an HTTP POST upload request. */
    protected function makeMimeBody (sceneId :int, data :ByteArray) :ByteArray
    {
        var output :ByteArray = new ByteArray();
        var memberId :int = _ctx.getMemberObject().memberName.getMemberId();

        var b :String = "--" + BOUNDARY + "\r\n";
        output.writeBytes(
            StringUtil.toBytes(
                "\r\n" + b +
                "Content-Disposition: form-data; name=\"member\"\r\n" +
                "\r\n" + String(memberId) + "\r\n" + b +
                "Content-Disposition: form-data; name=\"scene\"\r\n" +
                "\r\n" + String(sceneId) + "\r\n" + b +
                "Content-Disposition: form-data; name=\"snapshot\"; " +
                "filename=\"snapshot.jpg\"\r\n" +
                "Content-Type: image/jpg\r\n" +
                "\r\n"));
        output.writeBytes(data);
        output.writeBytes(StringUtil.toBytes("\r\n--" + BOUNDARY + "--\r\n"));
        return output;
    }

    protected function handleError (event :Event) :void
    {
        _ctx.displayFeedback(MsoyCodes.EDITING_MSGS, "e.snapshot_error");
    }

    protected function handleResult (event :Event) :void
    {
        _ctx.displayFeedback(MsoyCodes.EDITING_MSGS, "e.snapshot_success");
    }

    protected var _sceneId :int;
    protected var _panel :SnapshotPanel;
    protected var _encoder :JPGEncoder;
    protected var _loader :URLLoader;
    protected var _request :URLRequest;
    protected var _ctx :WorldContext;

    protected static const BOUNDARY :String = "why are you reading the raw http stream?";
}
}
