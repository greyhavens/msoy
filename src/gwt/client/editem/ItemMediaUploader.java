//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FormHandler;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormSubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormSubmitEvent;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;

import com.threerings.gwt.ui.SmartFileUpload;

import client.ui.BorderedPopup;
import client.ui.MsoyUI;

import client.util.FlashClients;
import client.util.MediaUtil;

/**
 * Helper class, used in ItemEditor. TODO: this should be removed and we should switch to the new
 * general purpose MediaUploader in util.
 */
public class ItemMediaUploader extends FlexTable
{
    public static final int MODE_NORMAL = 0;
    public static final int MODE_THUMB = 1;

    /**
     * @param mediaIds a semicolon-delimited list of types for the uploader to create, e.g.
     * {@link Item#MAIN_MEDIA} or {@link Item#MAIN_MEDIA};{@link Item#THUMB_MEDIA}. This value is
     * later passed to the bridge to identify the hash/mimeType returned by the server.
     * @param type the type of media being chosen: {@link ItemEditor#TYPE_IMAGE}, etc.
     * @param mode whether we're uploading normal media, thumbnail media or normal media that
     * should also generate a thumbnail image when changed.
     * @param updater the updater that knows how to set the media hash on the item.
     */
    public ItemMediaUploader (
        ItemEditor itemEditor, String mediaIds, String type, int mode,
        ItemEditor.MediaUpdater updater)
    {
        _itemEditor = itemEditor;
        _mediaIds = mediaIds;
        _type = type;
        _mode = mode;
        _updater = updater;

        setStyleName("mediaUploader");
        setCellPadding(0);
        setCellSpacing(0);

        FlexCellFormatter fmt = getFlexCellFormatter();

        fmt.setRowSpan(0, 0, 4);
        fmt.setStyleName(0, 0, "ItemPreview");
        fmt.setHorizontalAlignment(0, 0, HorizontalPanel.ALIGN_CENTER);
        fmt.setVerticalAlignment(0, 0, HorizontalPanel.ALIGN_MIDDLE);
        setText(0, 0, "");

        fmt.setWidth(0, 1, "5px");
        fmt.setRowSpan(0, 1, 4);

        setWidget(0, 2, _hint = MsoyUI.createLabel("", "Tip"));
        _hint.setWidth((2 * MediaDesc.THUMBNAIL_WIDTH) + "px");
        fmt.setVerticalAlignment(0, 1, HorizontalPanel.ALIGN_TOP);

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

        _upload.setName(mediaIds);
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

        setText(1, 0, "");
        fmt.setVerticalAlignment(1, 0, HorizontalPanel.ALIGN_BOTTOM);
        setText(2, 0, "");
        fmt.setVerticalAlignment(1, 0, HorizontalPanel.ALIGN_BOTTOM);
        setWidget(3, 0, _form);
        fmt.setVerticalAlignment(2, 0, HorizontalPanel.ALIGN_BOTTOM);

        // sweet sweet debugging
        //setText(3, 0, type + " : " + mediaIds + " : " + mode);
    }

    /**
     * Set the media to be shown in this uploader.
     */
    public void setMedia (MediaDesc desc)
    {
        if (desc != null) {
            int width = MediaDesc.THUMBNAIL_WIDTH, height = MediaDesc.THUMBNAIL_HEIGHT;
            if (_mode != MODE_THUMB) {
                width *= 2;
                height *= 2;
            }
            setWidget(0, 0, MediaUtil.createMediaView(desc, width, height, null));

        } else {
            setMediaBlank();
        }

        if (ItemEditor.TYPE_FLASH.equals(_type) || ItemEditor.TYPE_IMAGE.equals(_type)) {
            addImageEditing(desc);
        }
    }

    protected void addImageEditing (final MediaDesc desc)
    {
        final Button createBtn = new Button(_emsgs.createImage());

        ClickListener listener = new ClickListener() {
            public void onClick (Widget sender) {
                String url = (sender == createBtn) ? null : desc.getMediaPath();
                int maxWidth = (_mode == MODE_THUMB) ? MediaDesc.THUMBNAIL_WIDTH : -1;
                int maxHeight = (_mode == MODE_THUMB) ? MediaDesc.THUMBNAIL_HEIGHT : -1;
                _editorPopup = new BorderedPopup();
                _editorPopup.setWidget(FlashClients.createImageEditor(
                    _itemEditor.getOffsetWidth(), _itemEditor.getOffsetHeight(),
                    _mediaIds, url, maxWidth, maxHeight));
                _editorPopup.show();
            }
        };
        createBtn.addClickListener(listener);
        setWidget(2, 0, createBtn);

        if (desc != null && desc.isImage()) {
            Button editBtn = new Button(_emsgs.editImage());
            editBtn.addClickListener(listener);
            setWidget(1, 0, editBtn);

        } else {
            setText(1, 0, "");
        }
    }

    /**
     * Set the media as uploaded by the user.
     */
    public void setUploadedMedia (MediaDesc desc, int width, int height)
    {
        // TODO: have the media chooser tell us the original file name
        // String result = _updater.updateMedia(_upload.getFilename(), desc, width, height);
        String result = _updater.updateMedia("", desc, width, height);
        if (result == null) {
            setMedia(desc);
        } else {
            MsoyUI.error(result);
        }
    }

    /**
     * Set a hint to be displayed next to the media area.
     */
    public void setHint (String hint)
    {
        _hint.setText(hint);
    }

    /**
     * Create the widget we show when the media descriptor is null.
     */
    protected void setMediaBlank ()
    {
        setText(0, 0, "");
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

    @Override // from Widget
    protected void onLoad ()
    {
        super.onLoad();
        configureBridge();
    }

    @Override // from Widget
    protected void onUnload ()
    {
        super.onUnload();
        closeImageEditor();
    }

    protected static native void configureBridge () /*-{
        $wnd.closeImageEditor = function () {
            @client.editem.ItemMediaUploader::closeImageEditor()();
        };
    }-*/;

    protected static void closeImageEditor ()
    {
        if (_editorPopup != null) {
            _editorPopup.removeFromParent();
            _editorPopup = null;
        }
    }

    protected ItemEditor.MediaUpdater _updater;

    protected Label _hint;
    protected HorizontalPanel _panel;

    protected FormPanel _form;
    protected SmartFileUpload _upload;
    protected String _submitted;

    protected ItemEditor _itemEditor;
    protected String _mediaIds;
    protected String _type;
    protected int _mode;

    protected static BorderedPopup _editorPopup;

    protected static final int CHOOSER_WIDTH = 60;
    protected static final int CHOOSER_HEIGHT = 28;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
