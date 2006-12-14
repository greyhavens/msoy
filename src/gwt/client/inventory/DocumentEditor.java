//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.VerticalPanel;

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
    }

    // @Override from ItemEditor
    protected void createMainInterface (VerticalPanel main)
    {
        super.createMainInterface(main);

        main.add(createMainUploader("Upload your document.", new MediaUpdater() {
            public String updateMedia (MediaDesc desc) {
                // TODO: validate media type
                _doc.docMedia = desc;
                return null;
            }
        }));
    }

    // @Override from ItemEditor
    protected Item createBlankItem ()
    {
        return new Document();
    }

    protected Document _doc;
}
