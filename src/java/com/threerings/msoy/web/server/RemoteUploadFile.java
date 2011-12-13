//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.samskivert.io.StreamUtil;

import com.threerings.msoy.data.all.MediaMimeTypes;

import static com.threerings.msoy.Log.log;

/**
 * Downloads a file from a remote web server to use as an UploadFile source.
 */
public class RemoteUploadFile extends UploadFile
{
    public RemoteUploadFile (String url)
    {
        _url = url;
    }

    @Override
    public InputStream getInputStream ()
        throws IOException
    {
        if (_bytes == null) {
            // Only load the bytes once...
            _bytes = StreamUtil.toByteArray(requireConnection().getInputStream());
        }
        // But create a new stream every time...
        return new ByteArrayInputStream(_bytes);
    }

    @Override
    public String getOriginalName ()
    {
        return _url;
    }

    @Override
    public byte getMimeType ()
    {
        // Use the HTTP Content-Type mime type, otherwise derive one from the url
        try {
            byte mimeType = MediaMimeTypes.stringToMimeType(
                requireConnection().getHeaderField("Content-Type"));
            log.info("getMimeType", "mimeType", mimeType, "header", requireConnection().getHeaderField("Content-Type"));
            if (mimeType != MediaMimeTypes.INVALID_MIME_TYPE) {
                return mimeType;
            }
        } catch (IOException e) {
            log.warning("Couldn't read mime type", e);
        }
        return _detectedMimeType;
    }

    protected HttpURLConnection requireConnection ()
        throws IOException
    {
        if (_conn == null) {
            _conn = (HttpURLConnection) new URL(_url).openConnection();
            if (_conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Bad HTTP response: " + _conn.getResponseMessage());
            }
        }
        return _conn;
    }

    protected String _url;
    protected HttpURLConnection _conn;
    protected byte[] _bytes;
}
