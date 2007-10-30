//
// $Id$

package client.item;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.SimplePanel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import client.shell.CShell;
import client.util.Stars;

public class ItemRating extends SimplePanel
{
    /**
     * Construct a new display for the given item with member's previous rating of the item,
     * automatically figuring out read-only or read/write display mode.
     */
    public ItemRating (Item item, int memberId, byte memberRating)
    {
        this(item, memberRating, (item.isRatable() && (memberId != 0)) ?
             Stars.MODE_BOTH : Stars.MODE_READ, false);
    }

    /**
     * Construct a new display for the given item with member's previous rating of the item and a
     * specified display mode.
     */
    public ItemRating (Item item, byte memberRating, int mode, boolean halfSize)
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

        // if we're not logged in, force MODE_READ
        if (CShell.ident == null || mode == Stars.MODE_READ) {
            setWidget(_averageStars = new ItemStars(Stars.MODE_READ, true, halfSize));
        } else {
            Grid ratingsGrid = new Grid(2, 2);
            setWidget(ratingsGrid);
            ratingsGrid.setText(0, 0, CShell.cmsgs.averageRating());
            ratingsGrid.setWidget(0, 1, 
                _averageStars = new ItemStars(Stars.MODE_READ, true, halfSize));
            ratingsGrid.setText(1, 0, CShell.cmsgs.playerRating());
            ratingsGrid.setWidget(1, 1, _playerStars = new ItemStars(mode, false, halfSize));
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
            CShell.itemsvc.rateItem(CShell.ident, _itemId, newRating, new AsyncCallback() {
                public void onSuccess (Object result) {
                    _item.rating = ((Float)result).floatValue();
                    _averageStars.update();
                    if (_playerStars != null) {
                        _playerStars.update();
                    }
                }
                public void onFailure (Throwable caught) {
                    CShell.log("rateItem failed", caught);
                    // TODO: Error image?
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
