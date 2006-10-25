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
import com.threerings.msoy.item.web.MediaDesc;

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
            public String updateMedia (MediaDesc desc) {
                // TODO: validate media type
                _doc.docMedia = desc;
                return null;
            }
        });

        super.createEditorInterface();

        addRow("Title", _title = new TextBox());
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
