//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;

import client.shell.CShell;
import client.ui.BorderedPopup;
import client.ui.MsoyUI;
import client.util.FlashClients;
import client.util.MediaUtil;

/**
 * Helper class, used in ItemEditor.
 */
public class ItemMediaUploader extends FlexTable
{
    public static final int MODE_NORMAL = 0;
    public static final int MODE_THUMB = 1;
    public static final int MODE_GAME_SHOT = 2;
    public static final int MODE_GAME_SPLASH = 3;

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

        fmt.setStyleName(0, 0, "ItemPreview");
        fmt.setHorizontalAlignment(0, 0, HorizontalPanel.ALIGN_CENTER);
        fmt.setVerticalAlignment(0, 0, HorizontalPanel.ALIGN_MIDDLE);
        setText(0, 0, "");

        fmt.setWidth(0, 1, "5px");

        setWidget(0, 2, _hint = MsoyUI.createLabel("", "Tip"));
        _hint.setWidth((2 * MediaDesc.THUMBNAIL_WIDTH) + "px");
        fmt.setVerticalAlignment(0, 1, HorizontalPanel.ALIGN_TOP);

        setText(1, 0, "");
        fmt.setVerticalAlignment(1, 0, HorizontalPanel.ALIGN_BOTTOM);
        setText(2, 0, "");
        fmt.setVerticalAlignment(2, 0, HorizontalPanel.ALIGN_BOTTOM);
        setWidget(3, 0, FlashClients.createUploader(mediaIds, type));
        fmt.setVerticalAlignment(3, 0, HorizontalPanel.ALIGN_BOTTOM);

        setWidget(4, 0, _clearMedia = new Button(_emsgs.clear(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                _updater.clearMedia();
                setMedia(null);
            }
        }));
        fmt.setVerticalAlignment(4, 0, HorizontalPanel.ALIGN_BOTTOM);

        // sweet sweet debugging
        //setText(5, 0, type + " : " + mediaIds + " : " + mode);

        fmt.setRowSpan(0, 0, getRowCount());
        fmt.setRowSpan(0, 1, getRowCount());
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
            CShell.log("*** setMedia", "desc", desc, "width", width, "height", height); // TODO RZ

            setWidget(0, 0, MediaUtil.createMediaView(desc, width, height, null));

        } else {
            setMediaBlank();
        }

        if (-1 != _type.indexOf(ItemEditor.TYPE_IMAGE)) {
            addImageEditing(desc);
        }
    }

    protected void addImageEditing (final MediaDesc desc)
    {
        final Button createBtn = new Button(_emsgs.createImage());

        ClickHandler listener = new ClickHandler() {
            public void onClick (ClickEvent event) {
                openImageEditor((event.getSource() == createBtn) ? null : desc, false);
            }
        };
        createBtn.addClickHandler(listener);

        HorizontalPanel hpan = new HorizontalPanel();
        hpan.add(createBtn);
        hpan.add(FlashClients.createCameraButton(_mediaIds));
        hpan.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        setWidget(2, 0, hpan);

        if (desc != null && desc.isImage()) {
            Button editBtn = new Button(_emsgs.editImage());
            editBtn.addClickHandler(listener);
            setWidget(1, 0, editBtn);

        } else {
            setText(1, 0, "");
        }
    }

    /**
     * Set the media as uploaded by the user.
     */
    public void setUploadedMedia (String filename, MediaDesc desc, int width, int height)
    {
        String result = _updater.updateMedia(filename, desc, width, height);
        if (result == null) {
            setMedia(desc);
        } else if (result != ItemEditor.MediaUpdater.SUPPRESS_ERROR) {
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
     * Force open the image editor.
     */
    public void openImageEditor (MediaDesc desc, boolean takeSnapshot)
    {
        int popWidth = _itemEditor.getOffsetWidth() - 8;
        int popHeight = Math.max(FlashClients.CLIENT_HEIGHT,
            Math.min(_itemEditor.getOffsetHeight() - 8, Window.getClientHeight() - 8));
        String url = (desc == null) ? null : desc.getMediaPath();
        int maxWidth = -1;
        int maxHeight = -1;
        boolean maxRequired = false;

        switch (_mode) {
        case MODE_THUMB:
            maxWidth = MediaDesc.getWidth(MediaDesc.THUMBNAIL_SIZE);
            maxHeight = MediaDesc.getHeight(MediaDesc.THUMBNAIL_SIZE);
            break;
        case MODE_GAME_SHOT:
            maxWidth = MediaDesc.getWidth(MediaDesc.GAME_SHOT_SIZE);
            maxHeight = MediaDesc.getHeight(MediaDesc.GAME_SHOT_SIZE);
            maxRequired = true;
            break;
        case MODE_GAME_SPLASH:
            maxWidth = MediaDesc.getWidth(MediaDesc.GAME_SPLASH_SIZE);
            maxHeight = MediaDesc.getHeight(MediaDesc.GAME_SPLASH_SIZE);
            maxRequired = true;
            break;
        }

        _editorPopup = new BorderedPopup(false, true);
        _editorPopup.setWidget(FlashClients.createImageEditor(
            popWidth, popHeight, _mediaIds, takeSnapshot, url, maxWidth, maxHeight, maxRequired));
        _editorPopup.show();
    }

    /**
     * Create the widget we show when the media descriptor is null.
     */
    protected void setMediaBlank ()
    {
        setText(0, 0, "");
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
    protected Button _clearMedia;

    protected ItemEditor _itemEditor;
    protected String _mediaIds;
    protected String _type;
    protected int _mode;

    protected static BorderedPopup _editorPopup;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
