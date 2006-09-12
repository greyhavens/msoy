//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.Item;
import com.threerings.msoy.item.data.Furniture;

import client.MsoyEntryPoint;

/**
 * A class for creating and editing {@link Furniture} digital items.
 */
public class FurnitureEditor extends MediaItemEditor
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
        setText(row, 0, "Preview");
        getFlexCellFormatter().setColSpan(row, 0, 2);
        setWidget(row+1, 0, _preview = new Image());
        getFlexCellFormatter().setColSpan(row+1, 0, 2);

        setText(row+2, 0, "Description");
        setWidget(row+2, 1, _descrip = new TextBox());
        _descrip.addKeyboardListener(new KeyboardListenerAdapter() {
            public void onKeyPress (Widget sender, char keyCode, int mods) {
                if (_furniture != null) {
                    DeferredCommand.add(new Command() {
                        public void execute () {
                            _furniture.description = _descrip.getText();
                            updateSubmittable();
                        }
                    });
                }
            }
        });
    }

    // @Override from MediaItemEditor
    protected void setHash (String mediaHash, int mimeType)
    {
        super.setHash(mediaHash, mimeType);
        if (mediaHash.length() > 0) {
            _preview.setUrl(
                MsoyEntryPoint.toMediaPath(_furniture.getThumbnailPath()));
        }
    }

    // @Override from ItemEditor
    protected boolean itemConsistent ()
    {
        return super.itemConsistent() &&
            (_descrip.getText().trim().length() > 0);
    }

    // @Override from ItemEditor
    protected Item createBlankItem ()
    {
        return new Furniture();
    }

    protected Furniture _furniture;
    protected Image _preview;
    protected TextBox _descrip;
}
