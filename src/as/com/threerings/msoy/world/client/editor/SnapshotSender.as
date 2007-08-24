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

import com.threerings.flex.CommandButton;
import com.threerings.msoy.client.WorldClient;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.world.client.RoomView;
import com.threerings.util.StringUtil;

/**
 * Captures RoomView snapshots and sends them over to the server.
 */
public class SnapshotSender
{
    public static const SERVICE_ENTRY_POINT :String = "/snapshotsvc";
    public static const IMAGE_HEIGHT :int = 180;
    public static const IMAGE_WIDTH :int = 320;
    
    public function SnapshotSender (ctx :WorldContext)
    {
        _ctx = ctx;
    }

    public function init () :void
    {
        var client :WorldClient = _ctx.getWorldClient();
        var url :String =
            "http://" + client.getHostname() + ":" + client.getHttpPort() + SERVICE_ENTRY_POINT;

        _request = new URLRequest();
        _request.url = url;
        _request.method = "POST";
        _request.contentType = "multipart/form-data; boundary=" + BOUNDARY;

        _loader = new URLLoader();
        _loader.addEventListener(Event.COMPLETE, handleResult);
        _loader.addEventListener(Event.OPEN, handleProgress);
        _loader.addEventListener(HTTPStatusEvent.HTTP_STATUS, handleProgress);
        _loader.addEventListener(IOErrorEvent.IO_ERROR, handleError);
        _loader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleError);

        _encoder = new JPGEncoder(80);
    }

    public function shutdown () :void
    {
        _loader.removeEventListener(Event.COMPLETE, handleResult);
        _loader.removeEventListener(Event.OPEN, handleProgress);
        _loader.removeEventListener(HTTPStatusEvent.HTTP_STATUS, handleProgress);
        _loader.removeEventListener(IOErrorEvent.IO_ERROR, handleError);
        _loader.removeEventListener(SecurityErrorEvent.SECURITY_ERROR, handleError);
        
        _loader = null;
        _request = null;
        _encoder = null;
    }

    public function send (sceneId :int, view :RoomView) :void
    {
        if (_loader != null) {
            // draw the room, scaling down to the appropriate size
            var newScale :Number = IMAGE_HEIGHT / view.getScrollBounds().height;
            var matrix :Matrix = new Matrix(newScale, 0, 0, newScale);
            var room :BitmapData = new BitmapData(IMAGE_WIDTH, IMAGE_HEIGHT);
            room.draw(view, matrix, null, null, null, true);

            // encode as an image file and send over
            _request.data = makeMimeBody(sceneId, _encoder.encode(room));
            _loader.load(_request);
        }
    }

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

    public function handleError (event :Event) :void
    {
        trace("*** FAULT: " + event);
    }
    
    public function handleProgress (event :Event) :void
    {
        trace("*** INVOKE: " + event);
    }
    
    public function handleResult (event :Event) :void
    {
        trace("*** RESULT: " + event);
    }
    
    protected var _encoder :JPGEncoder;
    protected var _loader :URLLoader;
    protected var _request :URLRequest;
    protected var _ctx :WorldContext;

    protected static const BOUNDARY :String = "why are you reading the raw http stream?";

}
}
