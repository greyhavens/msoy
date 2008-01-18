//
// $Id$

package com.threerings.msoy.mchooser.modes;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.threerings.msoy.mchooser.MediaChooser;

import static com.threerings.msoy.mchooser.MediaChooser.log;

/**
 * Handles the uploading of media to Whirled and displaying progress.
 */
public class UploadMediaMode
    implements MediaChooser.Mode
{
    public UploadMediaMode (String name, byte[] media)
    {
        _name = name;
        _media = media;
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

    protected class Uploader extends Thread
    {
        public void run () {
            try {
                processUpload();
            } catch (Exception e) {
                updateStatus("Upload failed: " + e);
                e.printStackTrace(System.err);
            }
        }

        protected void processUpload ()
            throws Exception
        {
            URL upurl = new URL(_chooser.config.serverURL + "uploadsvc");
            HttpURLConnection conn = (HttpURLConnection)upurl.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);

            String bodyStart = "\r\n--" + BOUNDARY + "\r\n" +
                "Content-Disposition: form-data; name=\"client\"\r\n\r\n" +
                "mchooser\r\n--" + BOUNDARY + "\r\n" +
                "Content-Disposition: form-data; name=\"auth\"\r\n\r\n" +
                _chooser.config.authToken + "\r\n--" + BOUNDARY + "\r\n" +
                "Content-Disposition: form-data; name=\"" + _chooser.config.mediaId + "\"; " +
                "filename=\"" + _name + "\"\r\n" +
                "Content-Type: " + URLConnection.guessContentTypeFromName(_name) + "\r\n\r\n";
            String bodyEnd = "\r\n--" + BOUNDARY + "--\r\n";

            OutputStream out = conn.getOutputStream();
            out.write(bodyStart.getBytes("UTF-8"));
            for (int written = 0; written < _media.length; ) {
                int towrite = Math.min(_media.length-written, BLOCK_SIZE);
                out.write(_media, written, towrite);
                written += towrite;
                updateStatus("Wrote " + written + " of " + _media.length + "...");
            }
            out.write(bodyEnd.getBytes("UTF-8"));
            out.close();

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder buf = new StringBuilder();
            for (String line; (line = reader.readLine()) != null; ) {
                postResult(line);
            }
            reader.close();

            updateStatus("Upload complete.");
        }

        protected void updateStatus (final String status)
        {
            SwingUtilities.invokeLater(new Runnable() {
                public void run () {
                    _chooser.setMain(new JLabel(status));
                }
            });
        }

        protected void postResult (final String line)
        {
            SwingUtilities.invokeLater(new Runnable() {
                public void run () {
                    String[] bits = line.split(" ");
                    try {
                        _chooser.reportUpload(
                            bits[0], bits[1], Integer.parseInt(bits[2]), Integer.parseInt(bits[3]),
                            Integer.parseInt(bits[4]), Integer.parseInt(bits[5]));
                    } catch (Exception e) {
                        log.warning("Malformed upload result '" + line + "': " + e);
                    }
                }
            });
        }
    }

    protected String _name;
    protected byte[] _media;

    protected MediaChooser _chooser;
    protected Uploader _uploader;

    protected static final String BOUNDARY = "OHAIHEREISSOMEDATALOLZ!!!11!!11";
    protected static final int BLOCK_SIZE = 4096;
    protected static final Pattern RESPONSE_SCRIPT = Pattern.compile("onLoad=\"parent.(.*)\"");
}
