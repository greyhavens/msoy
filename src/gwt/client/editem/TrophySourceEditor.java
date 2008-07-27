//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.CheckBox;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.TrophySource;

import client.ui.NumberTextBox;

/**
 * A class for creating and editing {@link TrophySource} digital items.
 */
public class TrophySourceEditor extends SubItemEditor
{
    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _tsource = (TrophySource)item;
        _order.setText("" + _tsource.sortOrder);
        _secret.setChecked(_tsource.secret);
    }

    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        return new TrophySource();
    }

    @Override // from ItemEditor
    protected void addInfo ()
    {
        super.addInfo();
        addTip(_emsgs.trophyNameTip());

        addSpacer();
        addRow(_emsgs.trophyOrder(), _order = new NumberTextBox(false, 3, 3));
        addTip(_emsgs.trophyOrderTip());

        addSpacer();
        addRow(_emsgs.trophySecret(), _secret = new CheckBox());
        addTip(_emsgs.trophySecretTip());
    }

    @Override // from ItemEditor
    protected void addFurniUploader ()
    {
        // we have no furni tab
    }

    @Override // from ItemEditor
    protected void addThumbUploader ()
    {
        // trophy' thumb media are their primary media
        addSpacer();
        addRow(_emsgs.trophyLabel(), createThumbUploader(new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (width != TrophySource.TROPHY_WIDTH || height != TrophySource.TROPHY_HEIGHT ||
                    !desc.isImage()) {
                    return _emsgs.errInvalidTrophy();
                }
                _item.thumbMedia = desc;
                return null;
            }
        }));
    }

    @Override // from ItemEditor
    protected String getThumbnailHint ()
    {
        return _emsgs.trophyTip();
    }

    @Override // from ItemEditor
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

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
