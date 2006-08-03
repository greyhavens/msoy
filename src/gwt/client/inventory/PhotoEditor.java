//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.Item;
import com.threerings.msoy.item.data.Photo;

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
        setText(row, 0, "Caption");
        setWidget(row, 1, _caption = new TextBox());
        _caption.addKeyboardListener(new KeyboardListenerAdapter() {
            public void onKeyPress (Widget sender, char keyCode, int mods) {
                if (_photo != null) {
                    _photo.caption = _caption.getText();
                    updateSubmittable();
                }
            }
        });
    }

    // @Override from ItemEditor
    protected boolean itemConsistent ()
    {
        return (_caption.getText().trim().length() > 0);
    }

    // @Override from ItemEditor
    protected Item createBlankItem ()
    {
        return new Photo();
    }

    protected Photo _photo;
    protected TextBox _caption;
}
