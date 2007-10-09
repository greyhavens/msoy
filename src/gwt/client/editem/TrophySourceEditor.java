//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.TabPanel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.TrophySource;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.util.MsoyUI;
import client.util.RowPanel;

/**
 * A class for creating and editing {@link TrophySource} digital items.
 */
public class TrophySourceEditor extends ItemEditor
{
    // @Override from ItemEditor
    public Item createBlankItem ()
    {
        return new TrophySource();
    }

    // @Override // from ItemEditor
    protected void populateInfoTab (FlexTable info)
    {
        addInfoTip(info, CEditem.emsgs.trophyNameTip());

        super.populateInfoTab(info);
    }

    // @Override from ItemEditor
    protected void createFurniUploader (TabPanel tabs)
    {
        // we have no furni tab
    }

    // @Override from ItemEditor
    protected void createThumbUploader (TabPanel tabs)
    {
        // trophy' thumb media are their primary media
        String title = CEditem.emsgs.trophyMainTitle();
        _thumbUploader = createUploader(Item.THUMB_MEDIA, title, false, new MediaUpdater() {
            public String updateMedia (MediaDesc desc, int width, int height) {
                if (width != TrophySource.TROPHY_WIDTH || height != TrophySource.TROPHY_HEIGHT ||
                    !desc.isImage()) {
                    return CEditem.emsgs.invalidTrophy();
                }
                _item.thumbMedia = desc;
                return null;
            }
        });
        tabs.add(_thumbUploader, CEditem.emsgs.trophyMainTab());
    }
}
