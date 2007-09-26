//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.Event;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormHandler;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormSubmitEvent;
import com.google.gwt.user.client.ui.FormSubmitCompleteEvent;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

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

        final FormPanel form = new FormPanel();
        _panel = new HorizontalPanel();
        form.setWidget(_panel);
        form.setStyleName("Controls");

        if (GWT.isScript()) {
            form.setAction("/uploadsvc");
        } else {
            form.setAction("http://localhost:8080/uploadsvc");
        }
        form.setEncoding(FormPanel.ENCODING_MULTIPART);
        form.setMethod(FormPanel.METHOD_POST);

        final FileUpload upload = new FileUpload() {
            public void onBrowserEvent (Event event) {
                _status.setText(event.toString());
            }
        };
        upload.setName(id);
        _panel.add(upload);

        form.addFormHandler(new FormHandler() {
            public void onSubmit (FormSubmitEvent event) {
                // don't let them submit until they plug in a file...
                if (upload.getFilename().length() == 0) {
                    event.setCancelled(true);
                }
            }

            public void onSubmitComplete (FormSubmitCompleteEvent event) {
                String result = event.getResults();
                if (result != null && result.length() > 0) {
                    // TODO: This is fugly as all hell, but at least we're
                    // now reporting *something* to the user
                    _status.setText(result);
                }
            }
        });

        Button submit = new Button(CEditem.emsgs.upload());
        submit.setStyleName("mediaUploader");
        submit.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                form.submit();
            }
        });
        _panel.add(submit);

        add(form);
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
        String result = _updater.updateMedia(desc, width, height);
        if (result == null) {
            setMedia(desc);
        } else {
            _status.setText(result);
        }
    }

    protected ItemEditor.MediaUpdater _updater;

    protected String _title;
    protected Label _status;
    protected HorizontalPanel _target;
    protected HorizontalPanel _panel;

    protected boolean _thumbnail;
}
