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

import com.threerings.util.StringUtil;

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

        _bar = new ProgressBar();
        _bar.percentWidth = 100;
        _bar.indeterminate = true;
        _bar.label = _ctx.APPLET.get("m.wait");

        PopUpManager.addPopUp(this, ctx.getApplication(), true);
        PopUpManager.centerPopUp(this);
    }

    /**
     * Upload the specified media to the server.
     *
     * @param mediaId the Item media identifier: "main", "furni", "thumb"..., or "main;furni;thumb"
     *        to upload all 3.
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
// Note: goddamnit, the _loader doesn't dispatch PROGRESS events until the *download* phase,
// which we don't care about. We want to see the progress on uploading the giant wadge
// of bytes we're sending TO the server. So we keep the bar in indeterminate mode.
//        _bar.source = _loader;
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
     * Return the result of the upload, as an Object containing, as keys, each of the
     * semicolon-separated mediaIds specified in upload. Each value is another Object, containing:
     * {
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
        var result :Object = {};
        for each (var section :String in data.split("\n")) {
            if (section == "") {
                continue;
            }

            var bits :Array = data.split(" ");
            result[bits[0]] = {
                hash: bits[1],
                mimeType: parseInt(bits[2]),
                constraint: parseInt(bits[3]),
                width: parseInt(bits[4]),
                height: parseInt(bits[5])
            };
        }
        return result;
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        addChild(_bar);
    }

    protected var _ctx :AppletContext;

    protected var _bar :ProgressBar;

    protected var _serverURL :String;

    protected var _authToken :String;

    protected var _loader :URLLoader;

    protected static const BOUNDARY :String = "ooo-UmDiddlyHeresSomeData-ooo";
}
}
