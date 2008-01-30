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
{
    /**
     * Construct a new display for the given item with member's previous rating of the item,
     * automatically figuring out read-only or read/write display mode.
     */
    public ItemRating (Item item, int memberId, byte memberRating, boolean horiz)
    {
        this(item, memberRating, (item.isRatable() && (memberId != 0)) ?
             Stars.MODE_BOTH : Stars.MODE_READ, false, horiz);
    }

    /**
     * Construct a new display for the given item with member's previous rating of the item and a
     * specified display mode.
     */
    public ItemRating (Item item, byte memberRating, int mode, boolean halfSize, boolean horiz)
    {
        super();
        // sanity check
        if (mode != Stars.MODE_READ && !item.isRatable()) {
            throw new IllegalArgumentException("Can only rate clones and listed items " + _item);
        }
        setStyleName("itemRating");

        _item = item;
        _memberRating = memberRating;
        _itemId = new ItemIdent(_item.getType(), _item.getPrototypeId());
        _averageStars = new ItemStars(Stars.MODE_READ, true, halfSize);
        _playerStars = new ItemStars(mode, false, halfSize);

        // if we're not logged in, force MODE_READ
        if (CShell.ident == null || mode == Stars.MODE_READ) {
            setWidget(0, 0, _averageStars);

        } else if (horiz) {
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

    /** The subclass of {#link Stars} that implements most of the item rating. */
    protected class ItemStars extends Stars
    {
        protected ItemStars (int mode, boolean averageRating, boolean halfSize)
        {
            super(mode, averageRating, halfSize);
        }

        // @Override
        protected void update ()
        {
            if (_averageRating) {
                updateStarImage(_item.rating);
            } else {
                updateStarImage(_memberRating);
            }
        }

        // called when we are over the widget
        // @Override
        protected void update (double rating)
        {
            // we only allow mouse updates on player stars
            updateStarImage(rating);
        }

        // @Override
        protected void starsClicked (byte newRating)
        {
            rateItem(newRating);
        }
    
        /**
         * Performs a server call to give an item a new rating by this member.
         */
        protected void rateItem (byte newRating)
        {
            _memberRating = newRating;
            CShell.itemsvc.rateItem(CShell.ident, _itemId, newRating, new MsoyCallback() {
                public void onSuccess (Object result) {
                    _item.rating = ((Float)result).floatValue();
                    _averageStars.update();
                    if (_playerStars != null) {
                        _playerStars.update();
                    }
                }
            });
        }
    }

    protected Item _item;
    protected ItemIdent _itemId;
    protected byte _memberRating;
    protected ItemStars _averageStars;
    protected ItemStars _playerStars;
}
