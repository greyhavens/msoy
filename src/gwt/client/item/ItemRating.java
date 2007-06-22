//
// $Id$

package client.item;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

public class ItemRating extends FlowPanel
{
    /** Display only the item's average rating. Allow no updates. */
    public static final int MODE_READ = 1;

    /** Display only the user's rating of the item. Allow updates. */
    public static final int MODE_WRITE = 2;

    /** Display average rating, or user's on mouse-over, with updates. */
    public static final int MODE_BOTH = 3;

    /** Supply this for memberRating when using MODE_READ. */
    public static final byte NO_RATING = 0;

    /**
     * Construct a new display for the given item with member's previous rating of the item,
     * automatically figuring out read-only or read/write display mode.
     */
    public ItemRating (Item item, byte memberRating)
    {
        this(item, memberRating, item.isRatable() ?
             ItemRating.MODE_BOTH : ItemRating.MODE_READ, false);
    }

    /**
     * Construct a new display for the given item with member's previous rating of the item and a
     * specified display mode.
     */
    public ItemRating (Item item, byte memberRating, int mode, boolean halfSize)
    {
        // sanity check
        if (mode != MODE_READ && !item.isRatable()) {
            throw new IllegalArgumentException("Can only rate clones and listed items " + _item);
        }
        setStyleName("itemRating");

        _item = item;
        _memberRating = memberRating;
        _itemId = new ItemIdent(_item.getType(), _item.getPrototypeId());
        // if we're not logged in, force MODE_READ
        _mode = (CItem.ident == null) ? MODE_READ : mode;
        _halfSize = halfSize;

        // add the 10 images whose src url's we mercilessly mutate throughout this widget
        for (int ii = 0; ii < 10; ii ++) {
            Image halfStar = new Image();
            if (_mode != MODE_READ) {
                halfStar.addMouseListener(new RatingMouseListener(ii/2+1));
            }
            add(halfStar);
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
            rateItem((byte) _ratingInterval);
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
            updateStarImages(_item.rating, true);
        } else {
            updateStarImages(rating, true);
        }
    }

    // update the 10 half-star image elements to point to the right actual images
    protected void updateStarImages (double ratingToDisplay, boolean isUserRating)
    {
        int filledStars = (int) (ratingToDisplay * 2);
        String prefix = "/images/ui/stars/" + (_halfSize ? "half" : "full") + "/";
        String filledUrl = prefix + (isUserRating ? "user" : "average");
        for (int ii = 0; ii < filledStars; ii++) {
            String path = filledUrl + "_" + ((ii % 2) == 0 ? "lhalf" : "rhalf") + ".png";
            ((Image) getWidget(ii)).setUrl(path);
        }
        for (int ii = filledStars; ii < 10; ii++) {
            String path = prefix + "empty_" + ((ii % 2) == 0 ? "lhalf" : "rhalf") + ".png";
            ((Image) getWidget(ii)).setUrl(path);
        }
    }

    /**
     * Performs a server call to give an item a new rating by this member.
     */
    protected void rateItem (byte newRating)
    {
        _memberRating = newRating;
        CItem.itemsvc.rateItem(CItem.ident, _itemId, newRating, new AsyncCallback() {
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
    protected boolean _halfSize;
}
