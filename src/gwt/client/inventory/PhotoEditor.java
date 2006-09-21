//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.Photo;

import client.MsoyEntryPoint;

/**
 * A class for creating and editing {@link Photo} digital items.
 */
public class PhotoEditor extends ItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _photo = (Photo)item;
        _caption.setText((_photo.caption == null) ? "" : _photo.caption);
    }

    // @Override from ItemEditor
    protected void createEditorInterface ()
    {
        configureMainUploader("Upload your photo.", new MediaUpdater() {
            public void updateMedia (byte[] hash, byte mimeType) {
                _photo.photoMediaHash = hash;
                _photo.photoMimeType = mimeType;
            }
        });

        super.createEditorInterface();

        int row = getRowCount();
        setText(row, 0, "Caption");
        setWidget(row, 1, _caption = new TextBox());
        bind(_caption, new Binder() {
            public void textUpdated (String text) {
                _photo.caption = text;
            }
        });
    }

    // @Override from ItemEditor
    protected void setHash (String id, String mediaHash, int mimeType)
    {
        super.setHash(id, mediaHash, mimeType);

        // once the main media is uploaded, we may use the same media
        // in the other two places
        if (MAIN_ID.equals(id)) {
            if (_photo.thumbMediaHash == null) {
                recheckThumbMedia();
            }
            if (_photo.furniMediaHash == null) {
                recheckFurniMedia();
            }
        }

        // if the thumb and photo are the same, null the thumb
        if (_photo.thumbMimeType == _photo.photoMimeType &&
                MediaDesc.arraysEqual(_photo.thumbMediaHash,
                _photo.photoMediaHash)) {
            _photo.thumbMediaHash = null;
            _photo.thumbMimeType = (byte) 0;
            recheckThumbMedia();
        }

        // if the furni and photo are the same, null the furni
        if (_photo.furniMimeType == _photo.photoMimeType &&
                MediaDesc.arraysEqual(_photo.furniMediaHash,
                _photo.photoMediaHash)) {
            _photo.furniMediaHash = null;
            _photo.furniMimeType = (byte) 0;
            recheckFurniMedia();
        }
    }

    // @Override from ItemEditor
    protected Item createBlankItem ()
    {
        return new Photo();
    }

    protected Photo _photo;
    protected TextBox _caption;
}
