//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Document;
import com.threerings.msoy.item.web.Item;

/**
 * A class for creating and editing {@link Document} digital items.
 */
public class DocumentEditor extends ItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _doc = (Document)item;
        _title.setText((_doc.title == null) ? "" : _doc.title);
    }

    // @Override from ItemEditor
    protected void createEditorInterface ()
    {
        configureMainUploader("Upload your document.", new MediaUpdater() {
            public void updateMedia (byte[] hash, byte mimeType) {
                _doc.docMediaHash = hash;
                _doc.docMimeType = mimeType;
            }
        });

        super.createEditorInterface();

        int row = getRowCount();
        setText(0, 0, "Title");
        setWidget(0, 1, _title = new TextBox());
        bind(_title, new Binder() {
            public void textUpdated (String text) {
                _doc.title = text;
            }
        });
    }

    // @Override from ItemEditor
    protected Item createBlankItem ()
    {
        return new Document();
    }

    protected Document _doc;
    protected TextBox _title;
}
