//
// $Id$

package com.threerings.msoy.applets.net {

import flash.errors.IllegalOperationError;

import flash.events.Event;
import flash.events.EventDispatcher;

import flash.net.URLLoader;
import flash.net.URLRequest;
import flash.net.URLRequestMethod;

import flash.utils.ByteArray;

import com.threerings.util.StringUtil;
import com.threerings.util.ValueEvent;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 */
public class MediaUploader extends EventDispatcher
{
    public function MediaUploader (serverURL :String, authToken :String)
    {
        _serverURL = serverURL;
        _authToken = authToken;
    }

    /**
     * Upload the specified media to the server.
     *
     * @throws IllegalOperationError if this uploader has already been used to upload.
     */
    public function upload (mediaId :String, filename :String, media :ByteArray) :void
    {
        if (_loader != null) {
            throw new IllegalOperationError("Uploader has already been used to upload.");
        }

        var mimeType :String = MediaDesc.mimeTypeToString(MediaDesc.suffixToMimeType(filename));

        var body :ByteArray = new ByteArray();
        body.writeUTFBytes("\r\n--" + BOUNDARY + "\r\n" +
            "Content-Disposition: form-data; name=\"client\"\r\n\r\n" +
            "mchooser\r\n--" + BOUNDARY + "\r\n" +
            "Content-Disposition: form-data; name=\"auth\"\r\n\r\n" +
            _authToken + "\r\n--" + BOUNDARY + "\r\n" +
            "Content-Disposition: form-data; name=\"" + mediaId + "\"; " +
            "filename=\"" + filename + "\"\r\n" +
            "Content-Type: " + mimeType + "\r\n\r\n");
        body.writeBytes(media);
        body.writeUTFBytes("\r\n--" + BOUNDARY + "--\r\n");

        var request :URLRequest = new URLRequest(_serverURL + "uploadsvc");
        request.contentType = "multipart/form-data; boundary=" + BOUNDARY;
        request.method = URLRequestMethod.POST;
        request.data = body;

        _loader = new URLLoader();
        _loader.addEventListener(Event.COMPLETE, handleComplete);
        // TODO: error handling
        _loader.load(request);
    }

    protected function handleComplete (event :Event) :void
    {
        var data :String = _loader.data as String;
        var bits :Array = data.split(" ");

        var desc :MediaDesc = new MediaDesc();
        desc.hash = MediaDesc.stringToHash(bits[1]);
        desc.mimeType = parseInt(bits[2]);
        desc.constraint = parseInt(bits[3]);

        var result :Object = {
            mediaId: bits[0],
            hash: bits[1],
            mimeType: parseInt(bits[2]),
            constraint: parseInt(bits[3]),
            width: parseInt(bits[4]),
            height: parseInt(bits[5])
        };

        dispatchEvent(new ValueEvent(Event.COMPLETE, result));
    }

    protected var _serverURL :String;

    protected var _authToken :String;

    protected var _loader :URLLoader;

    protected static const BOUNDARY :String = "ooo-UmDiddlyHeresSomeData-ooo";
}
}
