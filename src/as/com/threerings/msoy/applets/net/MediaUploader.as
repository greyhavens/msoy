//
// $Id$

package com.threerings.msoy.applets.net {

import flash.errors.IllegalOperationError;

import flash.events.Event;
import flash.events.HTTPStatusEvent;
import flash.events.IOErrorEvent;
import flash.events.ProgressEvent;
import flash.events.SecurityErrorEvent;

import flash.net.URLLoader;
import flash.net.URLRequest;
import flash.net.URLRequestMethod;

import flash.utils.ByteArray;

import mx.controls.ProgressBar;

import mx.containers.TitleWindow;

import mx.managers.PopUpManager;

import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.applets.AppletContext;

/**
 * @eventType flash.events.Event.COMPLETE
 */
[Event(name="complete", type="flash.events.Event")]

/**
 * @eventType flash.events.HTTPStatusEvent.HTTP_STATUS
 */
[Event(name="httpStatus", type="flash.events.HTTPStatusEvent")]

/**
 * @eventType flash.events.IOErrorEvent.IO_ERROR
 */
[Event(name="ioError", type="flash.events.IOErrorEvent")]

/**
 * @eventType flash.events.Event.OPEN
 */
[Event(name="open", type="flash.events.Event")]

/**
 * @eventType flash.events.ProgressEvent.PROGRESS
 */
[Event(name="progress", type="flash.events.ProgressEvent")]

/**
 * @eventType flash.events.SecurityErrorEvent.SECURITY_ERROR
 */
[Event(name="securityError", type="flash.events.SecurityErrorEvent")]

/**
 * Uploads media to whirled from flash.
 */
public class MediaUploader extends TitleWindow
{
    /**
     * Create a new media uploader.
     */
    public function MediaUploader (ctx :AppletContext, serverURL :String, authToken :String)
    {
        _ctx = ctx;
        _serverURL = serverURL;
        _authToken = authToken;

        title = ctx.APPLET.get("t.saving");

        PopUpManager.addPopUp(this, ctx.getApplication(), true);
        PopUpManager.centerPopUp(this);
    }

    /**
     * Upload the specified media to the server.
     *
     * @param mediaId the Item media identifier: "main", "furni", "thumb"...
     * @param filename the target filename of the bytes.
     * @param media the raw media bytes. You may safely modify the bytes after starting
     *              the upload.
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
        // we dispatch all the loader's events as our own
        for each (var eventType :String in [
                Event.COMPLETE, HTTPStatusEvent.HTTP_STATUS, IOErrorEvent.IO_ERROR,
                Event.OPEN, ProgressEvent.PROGRESS, SecurityErrorEvent.SECURITY_ERROR ]) {
            _loader.addEventListener(eventType, dispatchEvent);
        }
        _loader.load(request);
    }

    /**
     * Close any current upload operation, if any, and prepare the uploader for re-use.
     */
    public function close () :void
    {
        if (_loader != null) {
            try {
                _loader.close();
            } catch (err :Error) {
                // ignore
            }
            _loader = null;
        }

        PopUpManager.removePopUp(this);
    }

    /**
     * Return the result of the upload, as an Object containing the following properties:
     * {
     *    mediaId: String ("main", "furni", "thumb" ...)
     *    hash: String (MediaDesc hash)
     *    mimeType: int (MediaDesc mimeType)
     *    constraint: int (MediaDesc constraint)
     *    width: int
     *    height: int
     * }
     */
    public function getResult () :Object
    {
        if (_loader == null || _loader.data == null) {
            throw new IllegalOperationError("Uploader has not yet completed the upload.");
        }

        var data :String = _loader.data as String;
        var bits :Array = data.split(" ");

        return {
            mediaId: bits[0],
            hash: bits[1],
            mimeType: parseInt(bits[2]),
            constraint: parseInt(bits[3]),
            width: parseInt(bits[4]),
            height: parseInt(bits[5])
        };
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var bar :ProgressBar = new ProgressBar();
        bar.percentWidth = 100;
        bar.indeterminate = true;
        bar.label = _ctx.APPLET.get("m.wait");
        addChild(bar);
    }

    protected var _ctx :AppletContext;

    protected var _serverURL :String;

    protected var _authToken :String;

    protected var _loader :URLLoader;

    protected static const BOUNDARY :String = "ooo-UmDiddlyHeresSomeData-ooo";
}
}
