//
// $Id$

package client.util;

import java.util.HashMap;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FormHandler;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormSubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormSubmitEvent;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartFileUpload;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.editem.EditemMessages;
import client.shell.CShell;

/**
 * Provides a simple "Upload" button that uploads a piece of media to the server and provides the
 * MediaDesc to a listener.
 */
public class MediaUploader extends FormPanel
{
    public static interface Listener
    {
        /**
         * Called to inform the listener that new media has been uploaded.
         *
         * @param name the name of the uploaded file (from the user's local filesystem).
         * @param desc a media descriptor referencing the uploaded media.
         * @param width if the media is a non-thumbnail image this will contain the width of the
         * image, otherwise zero.
         * @param height if the media is a non-thumbnail image this will contain the height of the
         * image, otherwise zero.
         */
        void mediaUploaded (String name, MediaDesc desc, int width, int height);
    }

    public MediaUploader (String mediaId, Listener listener)
    {
        addStyleName("mediaUploader");
        _mediaId = mediaId;
        _listener = listener;

        HorizontalPanel controls = new HorizontalPanel();
        setWidget(controls);
        controls.setStyleName("Controls");

        if (GWT.isScript()) {
            setAction("/uploadsvc");
        } else {
            setAction("http://localhost:8080/uploadsvc");
        }
        setEncoding(FormPanel.ENCODING_MULTIPART);
        setMethod(FormPanel.METHOD_POST);

        _upload = new SmartFileUpload();
        _upload.setName(mediaId);
        _upload.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                String toUpload = _upload.getFilename();
                if (toUpload.length() > 0 && !toUpload.equals(_submitted)) {
                    submit();
                }
            }
        });
        controls.add(_upload);

        addFormHandler(new FormHandler() {
            public void onSubmit (FormSubmitEvent event) {
                // don't let them submit until they plug in a file...
                if (_upload.getFilename().length() == 0) {
                    event.setCancelled(true);
                }
            }
            public void onSubmitComplete (FormSubmitCompleteEvent event) {
                String result = event.getResults();
                result = (result == null) ? "" : result.trim();
                if (result.length() > 0) {
                    // TODO: this is fugly as hell, but at least report *something* to the user
                    MsoyUI.error(result);
                } else {
                    _submitted = _upload.getFilename();
                }
            }
        });
    }

    @Override // from Widget
    public void onLoad ()
    {
        super.onLoad();
        if (_uploaders.size() == 0) {
            configureBridge();
        }
        _uploaders.put(_mediaId, this);
    }

    @Override // from Widget
    public void onUnload ()
    {
        super.onUnload();
        _uploaders.remove(_mediaId);
    }

    protected void mediaUploaded (MediaDesc desc, int width, int height)
    {
        _listener.mediaUploaded(_submitted, desc, width, height);
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code received from the
     * server as a response to our file upload POST request.
     */
    protected static void mediaUploaded (String id, String mediaHash, int mimeType, int constraint,
                                         int width, int height)
    {
        // for some reason the strings that come in from JavaScript are not "real" and if we just
        // pass them straight on through to GWT, freakoutery occurs (of the non-hand-waving
        // variety); so we convert them hackily to GWT strings here
        MediaUploader uploader = _uploaders.get(""+id);
        if (uploader == null) {
            CShell.log("No uploader registered for uploaded media [id=" + id +
                       ", hash=" + mediaHash + ", type=" + mimeType + "].");
        } else {
            uploader.mediaUploaded(
                new MediaDesc(""+mediaHash, (byte)mimeType, (byte)constraint), width, height);
        }
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code received from the
     * server to display an internal error message to the user.
     */
    protected static void uploadError ()
    {
        MsoyUI.error(_emsgs.errUploadError());
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code received from the
     * server to display a friendly message to the user that the upload was too large.
     */
    protected static void uploadTooLarge ()
    {
        MsoyUI.error(_emsgs.errUploadTooLarge());
    }

    /**
     * This wires up a sensibly named function that our POST response JavaScript code can call.
     */
    protected static native void configureBridge () /*-{
        $wnd.setHash = function (id, hash, type, constraint, width, height) {
           @client.util.MediaUploader::mediaUploaded(Ljava/lang/String;Ljava/lang/String;IIII)(id, hash, type, constraint, width, height);
        };
        $wnd.uploadError = function () {
           @client.util.MediaUploader::uploadError()();
        };
        $wnd.uploadTooLarge = function () {
           @client.util.MediaUploader::uploadTooLarge()();
        };
    }-*/;

    protected String _mediaId;
    protected Listener _listener;

    protected SmartFileUpload _upload;
    protected String _submitted;

    protected static HashMap<String, MediaUploader> _uploaders =
        new HashMap<String, MediaUploader>();

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
