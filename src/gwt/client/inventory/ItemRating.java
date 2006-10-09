//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemDetail;
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

    public ItemRating (WebContext ctx, ItemDetail detail, int mode)
    {
        super();
        _detail = detail;
        _ctx = ctx;
        _itemId = new ItemIdent(
            _detail.item.getType(), _detail.item.getProgenitorId());
        _mode = mode;
        setStyleName("itemRating");
        
        addMouseListener(this);
        update();
    }

    public void onMouseEnter (Widget sender)
    {
        // we act on mouseMove
    }
    public void onMouseLeave (Widget sender)
    {
        update();
    }
    public void onMouseMove (Widget sender, int x, int y)
    {
        update(x, sender.getOffsetWidth());
    }

    public void onMouseDown (Widget sender, int x, int y)
    {
        // we act on mouseUp
    }
    public void onMouseUp (Widget sender, int x, int y)
    {
        if (_mode != MODE_READ &&
            x >= 0 && x < sender.getOffsetWidth() &&
            y >= 0 && y < sender.getOffsetHeight()) {
            rateItem((byte) (1 + ((x * 5) / sender.getOffsetWidth())));
        }
    }

    protected void update ()
    {
        setUrl("/msoy/stars/" + getRatingImage(-1, -1) + ".gif");
    }
    protected void update (int pos, int width)
    {
        // a little sanity check -- this is the web we're dealing with
        if (pos >= 0 && pos < width) {
            setUrl("/msoy/stars/" + getRatingImage(pos, width) + ".gif");
        }
    }
    protected String getRatingImage (int pos, int width)
    {
        // if we're off the widget, or in read-only mode, show fixed # of stars
        if (pos == -1 || _mode == MODE_READ) {
            String imgBase = "stars_";
            float ratingToDisplay;
            if (_mode == MODE_WRITE || pos != -1) {
                ratingToDisplay = _detail.memberRating;
                imgBase += "2";

            } else {
                ratingToDisplay = _detail.item.rating;
                if (ratingToDisplay == 0.0) {
                    return "stars_2_0";
                }
                imgBase += "1";
            }
            // translate [1.0, 5.0] to (10, 15, ..., 50)
            return imgBase + "_" + ((int) (_detail.item.rating * 2)) * 5;
        }
        // if we're mousing over the widget and are configured to update the
        // user's rating, vary the # of stars depending on pointer's position
        return "stars_2_" + (10 + ((pos * 5) / width) * 10);
    }

    protected void rateItem (byte newRating)
    {
        _ctx.itemsvc.rateItem(
            _ctx.creds, _itemId, newRating,
            new AsyncCallback() {
                public void onSuccess (Object result) {
                    _detail = (ItemDetail) result;
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
    protected ItemIdent _itemId;
    protected ItemDetail _detail;
    protected int _mode;

}
