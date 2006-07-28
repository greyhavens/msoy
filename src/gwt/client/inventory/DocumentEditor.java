//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.Document;
import com.threerings.msoy.item.data.Item;

/**
 * A class for creating and editing {@link Document} digital items.
 */
public class DocumentEditor extends ItemEditor
{
    public DocumentEditor ()
    {
        add(new Label("Title"), WEST);
        add(_title = new TextBox(), CENTER);
        _title.addKeyboardListener(new KeyboardListenerAdapter() {
            public void onKeyPress (Widget sender, char keyCode, int mods) {
                if (_doc != null) {
                    _doc.title = _title.getText();
                    updateSubmittable();
                }
            }
        });
    }

    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _doc = (Document)item;
        _title.setText((_doc.title == null) ? "" : _doc.title);
    }

    // @Override from ItemEditor
    protected boolean itemConsistent ()
    {
        return (_title.getText().trim().length() > 0);
    }

    protected Document _doc;
    protected TextBox _title;
}
