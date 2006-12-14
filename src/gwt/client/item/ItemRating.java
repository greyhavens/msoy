//
// $Id$

package client.item;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.web.client.WebContext;

public class ItemRating extends Image
    implements MouseListener
{
    /** Display only the item's average rating. Allow no updates. */
    public static final int MODE_READ = 1;

    /** Display only the user's rating of the item. Allow updates. */
    public static final int MODE_WRITE = 2;

    /** Display average rating, or user's on mouse-over, with updates. */
    public static final int MODE_BOTH = 3;

    /**
     * Construct a read-only rating that doesn't need a WebContext. 
     */
    public ItemRating (Item item)
    {
        this(null, item, (byte) -1, MODE_READ);
    }

    /**
     * Construct a new display for the given item with member's previous rating of the
     * item, automatically figuring out read-only or read/write display mode.
     */
    public ItemRating (WebContext ctx, Item item, byte memberRating)
    {
        // we can rate this item if it's a clone, or if it's listed, and we have a context
        this(ctx, item, memberRating,
            (ctx != null && (item.parentId != -1 || item.ownerId == -1)) ?
                ItemRating.MODE_BOTH : ItemRating.MODE_READ);
    }

    /**
     * Construct a new display for the given item with member's previous rating of the
     * item and a specified display mode.
     */
    public ItemRating (WebContext ctx, Item item, byte memberRating, int mode)
    {
        setStyleName("itemRating");
        addMouseListener(this);

        _ctx = ctx;
        _item = item;
        _memberRating = memberRating;
        _itemId = new ItemIdent(_item.getType(), _item.getProgenitorId());
        if (mode != MODE_READ) {
            if (ctx == null) {
                throw new IllegalArgumentException("Need non-null webcontext [mode=" + mode + "]");
            }
            if (!_item.isRatable()) {
                throw new IllegalArgumentException(
                    "Can only rate clones and listed items " + _item);
            }
        }
        _mode = mode;

        update();
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
        update(x, sender.getOffsetWidth());
    }

    // from interface MouseListener
    public void onMouseDown (Widget sender, int x, int y)
    {
        // we act on mouseUp
    }

    // from interface MouseListener
    public void onMouseUp (Widget sender, int x, int y)
    {
        // make sure we're still on the widget when we unclick
        if (_mode != MODE_READ &&
            x >= 0 && x < sender.getOffsetWidth() &&
            y >= 0 && y < sender.getOffsetHeight()) {
            rateItem((byte) (1 + ((x * 5) / sender.getOffsetWidth())));
        }
    }

    // called to update image when we're not over the widget
    protected void update ()
    {
        setUrl("/msoy/stars/" + getRatingImage(-1, -1) + ".gif");
    }

    // called when we are over the widget
    protected void update (int pos, int width)
    {
        // a little sanity check -- this is the web we're dealing with
        if (pos >= 0 && pos < width) {
            setUrl("/msoy/stars/" + getRatingImage(pos, width) + ".gif");
        }
    }

    // calculate the right image to display for this situation
    protected String getRatingImage (int pos, int width)
    {
        // if we're off the widget, or in read-only mode, show fixed # of stars
        if (pos == -1 || _mode == MODE_READ) {
            String imgBase = "stars_";
            float ratingToDisplay;
            if (_mode == MODE_WRITE || pos != -1) {
                ratingToDisplay = _memberRating;
                imgBase += "2";
            } else {
                ratingToDisplay = _item.rating;
                if (ratingToDisplay == 0.0) {
                    return "stars_2_0";
                }
                imgBase += "1";
            }
            // translate [1.0, 5.0] to (10, 15, ..., 50)
            return imgBase + "_" + ((int) (_item.rating * 2)) * 5;
        }

        // if we're mousing over the widget and are configured to update the
        // user's rating, vary the # of stars depending on pointer's position
        return "stars_2_" + (10 + ((pos * 5) / width) * 10);
    }

    /**
     * Performs a server call to give an item a new rating by this member.
     */
    protected void rateItem (byte newRating)
    {
        _memberRating = newRating;
        _ctx.itemsvc.rateItem(_ctx.creds, _itemId, newRating, new AsyncCallback() {
            public void onSuccess (Object result) {
                _item.rating = ((Float)result).floatValue();
                _mode = MODE_READ;
                update();
            }
            public void onFailure (Throwable caught) {
                GWT.log("rateItem failed", caught);
                // TODO: Error image?
            }
        });
    }

    protected WebContext _ctx;
    protected Item _item;
    protected ItemIdent _itemId;
    protected int _mode;
    protected byte _memberRating;
}
