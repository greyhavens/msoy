//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.BitmapData;
import flash.events.Event;
import flash.events.HTTPStatusEvent;
import flash.events.IOErrorEvent;
import flash.events.SecurityErrorEvent;
import flash.geom.Matrix;
import flash.geom.Rectangle;
import flash.net.URLLoader;
import flash.net.URLRequest;
import flash.utils.ByteArray;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.flex.CommandButton;
import com.threerings.msoy.client.WorldClient;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.world.client.RoomView;
import com.threerings.util.StringUtil;

/**
 * Captures RoomView snapshots and sends them over to the server.
 */
public class SnapshotController
{
    public static const SERVICE_ENTRY_POINT :String = "/snapshotsvc";
    public static const IMAGE_HEIGHT :int = 180;
    public static const IMAGE_WIDTH :int = 320;

    public function SnapshotController (ctx :WorldContext)
    {
        _ctx = ctx;

        var client :WorldClient = _ctx.getWorldClient();
        var url :String =
            "http://" + client.getHostname() + ":" + client.getHttpPort() + SERVICE_ENTRY_POINT;

        _request = new URLRequest();
        _request.url = url;
        _request.method = "POST";
        _request.contentType = "multipart/form-data; boundary=" + BOUNDARY;

        _loader = new URLLoader();
        _loader.addEventListener(Event.COMPLETE, handleResult);
        _loader.addEventListener(IOErrorEvent.IO_ERROR, handleError);
        _loader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleError);

        _encoder = new JPGEncoder(80);
    }

    /** Pops up a UI that will ask the user to apply or cancel the current room screenshot. */
    public function takeScreenshot (sceneId :int, view :RoomView) :void
    {
        if (_panel == null) {
            var bitmap :BitmapData = capture(view);
            var callback :Function = function (save :Boolean) :void {
                if (save) {
                    // encodes bitmap image as a MIME file upload, and sends it over
                    _request.data = makeMimeBody(sceneId, _encoder.encode(bitmap));
                    _loader.load(_request);
                }
                _panel = null;
            }
            
            _panel = new SnapshotPanel(_ctx, bitmap, callback);
            _panel.open();
        }
    }

    /** Captures the current room view into a bitmap. */
    protected function capture (view :RoomView) :BitmapData
    {
        // draw the room, scaling down to the appropriate size
        var newScale :Number = IMAGE_HEIGHT / view.getScrollBounds().height;
        var matrix :Matrix = new Matrix(newScale, 0, 0, newScale);
        var room :BitmapData = new BitmapData(IMAGE_WIDTH, IMAGE_HEIGHT);
        room.draw(view, matrix, null, null, null, true);
        return room;
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

    protected var _panel :SnapshotPanel;
    protected var _encoder :JPGEncoder;
    protected var _loader :URLLoader;
    protected var _request :URLRequest;
    protected var _ctx :WorldContext;
    
    protected static const BOUNDARY :String = "why are you reading the raw http stream?";
}
}
