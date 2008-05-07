//
// $Id$

package client.item;

import com.google.gwt.user.client.ui.FlexTable;

import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import client.shell.CShell;
import client.util.MsoyCallback;
import client.util.Stars;

public class ItemRating extends FlexTable
    implements Stars.StarClickListener
{
    /**
     * Construct a new display for the given item with member's previous rating of the item and a
     * specified display mode.
     */
    public ItemRating (Item item, byte memberRating, boolean horiz)
    {
        _item = item;
        _averageStars = new Stars(_item.rating, true, false, null);

        // if we're not logged in, force read-only mode
        if (CShell.isGuest() || !item.isRatable()) {
            setWidget(0, 0, _averageStars);
            return;
        }

        _memberRating = memberRating;
        _playerStars = new Stars(_memberRating, false, false, this);
        if (horiz) {
            int col = 0;
            setText(0, col++, CShell.cmsgs.averageRating());
            setWidget(0, col++, _averageStars);
            setWidget(0, col++, WidgetUtil.makeShim(15, 5));
            setText(0, col++, CShell.cmsgs.playerRating());
            setWidget(0, col++, _playerStars);

        } else {
            setText(0, 0, CShell.cmsgs.averageRating());
            setWidget(0, 1, _averageStars);
            setText(1, 0, CShell.cmsgs.playerRating());
            setWidget(1, 1, _playerStars);
        }
    }

    // from interface Stars.StarClickListener
    public void starClicked (byte newRating)
    {
        _playerStars.setRating(_memberRating = newRating);
        ItemIdent ident = new ItemIdent(_item.getType(), _item.getPrototypeId());
        CShell.itemsvc.rateItem(CShell.ident, ident, newRating, new MsoyCallback() {
            public void onSuccess (Object result) {
                _averageStars.setRating(_item.rating = ((Float)result).floatValue());
            }
        });
    }

    protected Item _item;
    protected byte _memberRating;
    protected Stars _averageStars, _playerStars;
}
