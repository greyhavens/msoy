//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.item.data.all.Document;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.shell.CShell;

/**
 * A class for creating and editing {@link Document} digital items.
 */
public class DocumentEditor extends ItemEditor
{
    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _doc = (Document)item;
    }

    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        return new Document();
    }

    @Override // from ItemEditor
    protected void addExtras ()
    {
        String label = _emsgs.documentLabel();
        addRow(label, createMainUploader(TYPE_ANY, false, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                // TODO: validate media type
                _doc.docMedia = desc;
                return null;
            }
        }));
        addTip(_emsgs.documentTip());

        super.addExtras();
    }

    protected Document _doc;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
