//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.TabPanel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.TrophySource;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.shell.CShell;
import client.util.MsoyUI;
import client.util.NumberTextBox;
import client.util.RowPanel;

/**
 * A class for creating and editing {@link TrophySource} digital items.
 */
public class TrophySourceEditor extends SubItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _tsource = (TrophySource)item;
        _order.setText("" + _tsource.sortOrder);
        _secret.setChecked(_tsource.secret);
    }

    // @Override from ItemEditor
    public Item createBlankItem ()
    {
        return new TrophySource();
    }

    // @Override // from ItemEditor
    protected void addInfo ()
    {
        super.addInfo();
        addTip(CShell.emsgs.trophyNameTip());

        addSpacer();
        addRow(CShell.emsgs.trophyOrder(), _order = new NumberTextBox(false, 3, 3));
        addTip(CShell.emsgs.trophyOrderTip());

        addSpacer();
        addRow(CShell.emsgs.trophySecret(), _secret = new CheckBox());
        addTip(CShell.emsgs.trophySecretTip());
    }

    // @Override from ItemEditor
    protected void addFurniUploader ()
    {
        // we have no furni tab
    }

    // @Override from ItemEditor
    protected void addThumbUploader ()
    {
        // trophy' thumb media are their primary media
        addSpacer();
        addRow(CShell.emsgs.trophyMainTab(), createThumbUploader(new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (width != TrophySource.TROPHY_WIDTH || height != TrophySource.TROPHY_HEIGHT ||
                    !desc.isImage()) {
                    return CShell.emsgs.errInvalidTrophy();
                }
                _item.thumbMedia = desc;
                return null;
            }
        }), CShell.emsgs.trophyMainTitle());
    }

    // @Override // from ItemEditor
    protected void prepareItem ()
        throws Exception
    {
        super.prepareItem();
        _tsource.sortOrder = _order.getValue().intValue();
        _tsource.secret = _secret.isChecked();
    }

    protected TrophySource _tsource;
    protected NumberTextBox _order;
    protected CheckBox _secret;
}
