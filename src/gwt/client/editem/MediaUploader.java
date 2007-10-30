//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.Event;

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.FormHandler;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormSubmitEvent;
import com.google.gwt.user.client.ui.FormSubmitCompleteEvent;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartFileUpload;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.util.MediaUtil;
import client.util.MsoyUI;

/**
 * Helper class, used in ItemEditor.
 */
public class MediaUploader extends VerticalPanel
{
    /**
     * @param id the id of the uploader to create. This value is later passed to the bridge to
     * identify the hash/mimeType returned by the server.
     * @param title A title to be displayed to the user.
     * @param thumbnail if true the preview will be thumbnail sized, false it will be preview
     * sized.
     * @param updater the updater that knows how to set the media hash on the item.
     */
    public MediaUploader (String id, String title, boolean thumbnail,
                          ItemEditor.MediaUpdater updater)
    {
        setStyleName("mediaUploader");

        _thumbnail = thumbnail;
        _updater = updater;
        _title = title;

        add(_status = MsoyUI.createLabel(_title, "Status"));

        add(_target = new HorizontalPanel());
        _target.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        _target.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        _target.setStyleName(_thumbnail ? "Thumbnail" : "Preview");

        _form = new FormPanel();
        _panel = new HorizontalPanel();
        _form.setWidget(_panel);
        _form.setStyleName("Controls");

        if (GWT.isScript()) {
            _form.setAction("/uploadsvc");
        } else {
            _form.setAction("http://localhost:8080/uploadsvc");
        }
        _form.setEncoding(FormPanel.ENCODING_MULTIPART);
        _form.setMethod(FormPanel.METHOD_POST);

        _upload = new SmartFileUpload();
        _upload.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                uploadMedia();
            }
        });
        _upload.setName(id);
        _panel.add(_upload);

        _form.addFormHandler(new FormHandler() {
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
                    // TODO: This is fugly as all hell, but at least we're now reporting
                    // *something* to the user
                    MsoyUI.error(result);
                } else {
                    _submitted = _upload.getFilename();
                }
            }
        });

        add(_form);
    }

    /**
     * Set the media to be shown in this uploader.
     */
    public void setMedia (MediaDesc desc)
    {
        _target.clear();
        if (desc != null) {
            int size = _thumbnail ? MediaDesc.THUMBNAIL_SIZE : MediaDesc.PREVIEW_SIZE;
            _target.add(MediaUtil.createMediaView(desc, size));
            _status.setText(_title);
        }
    }

    /**
     * Set the media as uploaded by the user.
     */
    public void setUploadedMedia (MediaDesc desc, int width, int height)
    {
        String result = _updater.updateMedia(_upload.getFilename(), desc, width, height);
        if (result == null) {
            setMedia(desc);
        } else {
            MsoyUI.error(result);
        }
    }

    protected void uploadMedia ()
    {
        if (_upload.getFilename().length() == 0) {
            return;
        }
        if (_submitted != null && _submitted.equals(_upload.getFilename())) {
            return;
        }
        _form.submit();
    }

    protected ItemEditor.MediaUpdater _updater;

    protected String _title;
    protected Label _status;
    protected HorizontalPanel _target;
    protected HorizontalPanel _panel;

    protected FormPanel _form;
    protected SmartFileUpload _upload;
    protected String _submitted;

    protected boolean _thumbnail;
}
