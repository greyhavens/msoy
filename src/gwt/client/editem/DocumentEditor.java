//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.TabPanel;
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
    public Item createBlankItem ()
    {
        return new Document();
    }

    // @Override from ItemEditor
    protected void createInterface (VerticalPanel contents, TabPanel tabs)
    {
        tabs.add(createMainUploader(CEditem.emsgs.documentMainTitle(), new MediaUpdater() {
            public String updateMedia (MediaDesc desc) {
                // TODO: validate media type
                _doc.docMedia = desc;
                return null;
            }
        }), CEditem.emsgs.documentMainTab());

        super.createInterface(contents, tabs);
    }

    protected Document _doc;
}
