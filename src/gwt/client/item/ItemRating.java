//
// $Id$

package client.item;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

public class ItemRating extends FlexTable
{
    /** Display only the item's average rating. Allow no updates. */
    public static final int MODE_READ = 1;

    /** Display only the user's rating of the item. Allow updates. */
    public static final int MODE_WRITE = 2;

    /** Display average rating, or user's on mouse-over, with updates. */
    public static final int MODE_BOTH = 3;

    /**
     * Construct a new display for the given item with member's previous rating of the item,
     * automatically figuring out read-only or read/write display mode.
     */
    public ItemRating (Item item, byte memberRating)
    {
        this(item, memberRating, item.isRatable() ? ItemRating.MODE_BOTH : ItemRating.MODE_READ);
    }

    /**
     * Construct a new display for the given item with member's previous rating of the item and a
     * specified display mode.
     */
    public ItemRating (Item item, byte memberRating, int mode)
    {
        // sanity check
        if (mode != MODE_READ && !item.isRatable()) {
            throw new IllegalArgumentException("Can only rate clones and listed items " + _item);
        }
        setStyleName("itemRating");
        setCellSpacing(0);
        setCellPadding(0);

        _item = item;
        _memberRating = memberRating;
        _itemId = new ItemIdent(_item.getType(), _item.getPrototypeId());
        // if we're not logged in, force MODE_READ
        _mode = (CItem.creds == null) ? MODE_READ : mode;

        // add the 10 images whose src url's we mercilessly mutate throughout this widget
        for (int i = 0; i < 10; i ++) {
            Image halfStar = new Image();
            halfStar.addMouseListener(new RatingMouseListener(i/2+1));
            halfStar.setStyleName("itemRatingStar");
            setWidget(0, i, halfStar);
        }
        // and initialize the stars
        update();
    }

    // each half star gets a listener that knows which rating it translates to
    protected class RatingMouseListener implements MouseListener
    {
        public RatingMouseListener (int starIx)
        {
            _ratingInterval = starIx;

        }
        // from interface MouseListener
        public void onMouseEnter (Widget sender)
        {
            // we act on mouseMove
        }

        // from interface MouseListener
        public void onMouseLeave (Widget sender)
        {
            update();
        }

        // from interface MouseListener
        public void onMouseMove (Widget sender, int x, int y)
        {
            update(_ratingInterval);
        }

        // from interface MouseListener
        public void onMouseDown (Widget sender, int x, int y)
        {
            // we act on mouseUp
        }

        // from interface MouseListener
        public void onMouseUp (Widget sender, int x, int y)
        {
            if (_mode != MODE_READ) {
                rateItem((byte) _ratingInterval);
            }
        }
        
        protected int _ratingInterval;
    }
    
    // called to update image when we're not over the widget
    protected void update ()
    {
        // show average rating unless we're in write-only mode
        if (_mode == MODE_WRITE) {
            updateStarImages(_memberRating, true);
        } else {
            updateStarImages(_item.rating, false);
        }
    }

    // called when we are over the widget
    protected void update (double rating)
    {
        // show the changing user rating as user hovers over widget, unless we're read-only
        if (_mode == MODE_READ) {
            updateStarImages(_memberRating, true);
        } else {
            updateStarImages(rating, true);
        }
    }

    // update the 10 half-star image elements to point to the right actual images
    protected void updateStarImages (double ratingToDisplay, boolean isUserRating)
    {
        int filledStars = (int) (ratingToDisplay * 2);
        String filledUrl = "/images/ui/star_" + (isUserRating ? "user" : "average");
        for (int i = 0; i < filledStars; i ++) {
            ((Image) getWidget(0, i)).setUrl(
                filledUrl + "_" + ((i % 2) == 0 ? "lhalf" : "rhalf") + ".png");
        }
        for (int i = filledStars; i < 10; i ++) {
            ((Image) getWidget(0, i)).setUrl(
                "/images/ui/star_empty_" + ((i % 2) == 0 ? "lhalf" : "rhalf") + ".png");
        }
    }

    /**
     * Performs a server call to give an item a new rating by this member.
     */
    protected void rateItem (byte newRating)
    {
        _memberRating = newRating;
        CItem.itemsvc.rateItem(CItem.creds, _itemId, newRating, new AsyncCallback() {
            public void onSuccess (Object result) {
                _item.rating = ((Float)result).floatValue();
                _mode = MODE_READ;
                update();
            }
            public void onFailure (Throwable caught) {
                CItem.log("rateItem failed", caught);
                // TODO: Error image?
            }
        });
    }

    protected Item _item;
    protected ItemIdent _itemId;
    protected int _mode;
    protected byte _memberRating;
}
