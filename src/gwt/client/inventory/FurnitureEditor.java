//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.Command;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Furniture;
import com.threerings.msoy.item.web.MediaDesc;

import client.MsoyEntryPoint;

/**
 * A class for creating and editing {@link Furniture} digital items.
 */
public class FurnitureEditor extends ItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _furniture = (Furniture)item;
        _descrip.setText((_furniture.description == null) ? "" :
                         _furniture.description);
    }

    // @Override from ItemEditor
    protected void createEditorInterface ()
    {
        super.createEditorInterface();

        int row = getRowCount();
        setText(row, 0, "Description");
        setWidget(row, 1, _descrip = new TextBox());
        bind(_descrip, new Binder() {
            public void textUpdated (String text) {
                _furniture.description = text;
            }
        });
    }

    // @Override from ItemEditor
    protected void setHash (String id, String mediaHash, int mimeType)
    {
        super.setHash(id, mediaHash, mimeType);

        // if the user has not yet uploaded thumb media, use any uploaded
        // furni media for the thumb, too
        if (FURNI_ID.equals(id) && _furniture.thumbMediaHash == null) {
            recheckThumbMedia();
        }

        // if the thumb and the furni are identical, null the thumb
        if (_furniture.thumbMimeType == _furniture.furniMimeType &&
                MediaDesc.arraysEqual(_furniture.thumbMediaHash,
                _furniture.furniMediaHash)) {
            _furniture.thumbMediaHash = null;
            _furniture.thumbMimeType = (byte) 0;
            recheckThumbMedia();
        }
    }

    // @Override from ItemEditor
    protected Item createBlankItem ()
    {
        return new Furniture();
    }

    protected Furniture _furniture;
    protected TextBox _descrip;
}
