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
import com.threerings.msoy.item.web.Photo;

import client.MsoyEntryPoint;

/**
 * A class for creating and editing {@link Photo} digital items.
 */
public class PhotoEditor extends MediaItemEditor
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
        super.createEditorInterface();

        int row = getRowCount();
        setText(row, 0, "Preview");
        getFlexCellFormatter().setColSpan(row, 0, 2);
        setWidget(row+1, 0, _preview = new Image());
        getFlexCellFormatter().setColSpan(row+1, 0, 2);

        setText(row+2, 0, "Caption");
        setWidget(row+2, 1, _caption = new TextBox());
        bind(_caption, new Binder() {
            public void textUpdated (String text) {
                _photo.caption = text;
            }
        });
    }

    // @Override from MediaItemEditor
    protected void setHash (String mediaHash, int mimeType)
    {
        super.setHash(mediaHash, mimeType);
        if (mediaHash.length() > 0) {
            _preview.setUrl(
                MsoyEntryPoint.toMediaPath(_photo.getThumbnailPath()));
        }
    }

    // @Override from ItemEditor
    protected Item createBlankItem ()
    {
        return new Photo();
    }

    protected Photo _photo;
    protected Image _preview;
    protected TextBox _caption;
}
