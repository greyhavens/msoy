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
                _photo.photoMedia = new MediaDesc(hash, mimeType);
                recenter();
            }
        });

        super.createEditorInterface();

        addRow("Caption", _caption = new TextBox());
        bind(_caption, new Binder() {
            public void textUpdated (String text) {
                _photo.caption = text;
            }
        });
    }

    // @Override from ItemEditor
    protected Item createBlankItem ()
    {
        return new Photo();
    }

    protected Photo _photo;
    protected TextBox _caption;
}
