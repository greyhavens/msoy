//
// $Id$

package com.threerings.msoy.applets.net {

import flash.net.URLRequest;
import flash.net.URLRequestMethod;
import flash.net.URLVariables;

import flash.utils.ByteArray;

import com.threerings.msoy.client.DeploymentConfig;

import com.threerings.msoy.data.all.MediaDesc;

public class MediaUploadUtil
{
    /**
     * Create the URLRequest object for doing an upload to one of our media servlets, for
     * use with either a FileReference or URLLoader.
     *
     * @param servlet the servlet to talk to, for example "uploadsvc"
     * @param authToken the user's auth token
     * @param mediaIds if uploading using a URLLoader, the form field name of the file data.
     * @param filename if uploading using a URLLoader, the filename.
     * @param media if uploading using a URLLoader, the media to upload.
     */
    public static function createRequest (
        servlet :String, authToken :String,
        mediaIds :String = null, filename :String = null, media :ByteArray = null) :URLRequest
    {
        const BOUNDARY :String = "ooo-UmDoodleGotSomeData-ooo";
        var request :URLRequest = new URLRequest(DeploymentConfig.serverURL + servlet);
        request.contentType = "multipart/form-data; boundary=" + BOUNDARY;
        request.method = URLRequestMethod.POST;

        if (mediaIds != null) {
            const mimeType :String = MediaDesc.mimeTypeToString(
                MediaDesc.suffixToMimeType(filename));
            var body :ByteArray = new ByteArray();
            body.writeUTFBytes("\r\n--" + BOUNDARY + "\r\n" +
                "Content-Disposition: form-data; name=\"client\"\r\n\r\n" +
                "mchooser\r\n--" + BOUNDARY + "\r\n" +
                "Content-Disposition: form-data; name=\"auth\"\r\n\r\n" +
                authToken + "\r\n--" + BOUNDARY + "\r\n" +
                "Content-Disposition: form-data; name=\"" + mediaIds + "\"; " +
                "filename=\"" + filename + "\"\r\n" +
                "Content-Type: " + mimeType + "\r\n\r\n");
            body.writeBytes(media);
            body.writeUTFBytes("\r\n--" + BOUNDARY + "--\r\n");

            request.data = body;
            
        } else {
            var vars :URLVariables = new URLVariables();
            vars.client = "mchooser";
            vars.auth = authToken;

            request.data = vars;
        }

        return request;
    }

    /**
     * Parse the result of an upload.
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
    public static function parseResult (response :String) :Object
    {
        var result :Object = {};
        for each (var section :String in response.split("\n")) {
            if (section == "") {
                continue;
            }
            var bits :Array = section.split(" ");
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
}
}
