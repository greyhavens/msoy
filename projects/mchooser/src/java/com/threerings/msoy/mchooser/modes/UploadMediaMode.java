//
// $Id$

package com.threerings.msoy.mchooser.modes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import com.threerings.msoy.mchooser.MediaChooser;

/**
 * Handles the uploading of media to Whirled and displaying progress.
 */
public class UploadMediaMode
    implements MediaChooser.Mode
{
    public UploadMediaMode (String name, byte[] media)
    {
        _source = new ByteArrayPartSource(name, media);
    }

    // from interface MediaChooser.Mode
    public void activate (MediaChooser chooser)
    {
        _chooser = chooser;
        _chooser.setSidebar(new JLabel("Uploading."));
        _chooser.setMain(new JLabel("Uploading..."));

        _uploader = new Uploader();
        _uploader.start();
    }

    // from interface MediaChooser.Mode
    public void deactivated ()
    {
        if (_uploader != null) {
            // _uploader.abortUpload();
            _uploader = null;
        }
    }

    protected void updateStatus (final String status, final String callback)
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run () {
                _chooser.setMain(new JLabel(status));
                if (callback != null) {
                    _chooser.reportUploadComplete(callback);
                }
            }
        });
    }

    protected class Uploader extends Thread
    {
        public void run () {
            try {
                processUpload();
            } catch (Exception e) {
                updateStatus("Upload failed: " + e, null);
            }
        }

        protected void processUpload ()
            throws Exception
        {
            PostMethod method = new PostMethod(_chooser.config.serverURL + "/uploadsvc");
            Part[] parts = {
                // TODO: override sendData(), report progress
                new FilePart(_chooser.config.mediaId, _source)
            };
            method.setRequestEntity(new MultipartRequestEntity(parts, method.getParams()));

            HttpClient client = new HttpClient();
            int status = client.executeMethod(method);
            if (status != 200) {
                throw new Exception("got error " + status);
            }

            String result = method.getResponseBodyAsString();
            Matcher m = RESPONSE_SCRIPT.matcher(result);
            if (!m.find()) {
                throw new Exception("got invalid response text '" + result + "'.");
            }

            updateStatus("Upload complete.", m.group(1));
        }
    }

    protected MediaChooser _chooser;
    protected ByteArrayPartSource _source;
    protected Uploader _uploader;

    protected static Pattern RESPONSE_SCRIPT = Pattern.compile("onLoad=\"parent.(.*)\"");
}
