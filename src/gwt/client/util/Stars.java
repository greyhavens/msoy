//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.Widget;

/**
 * A widget for displaying and optionally registering input on a row of 5 stars, i.e. for ratings.
 */
public abstract class Stars extends FlowPanel
{
    /** Display only the item's average rating. Allow no updates. */
    public static final int MODE_READ = 1;
    /** Display only the user's rating of the item. Allow updates. */
    public static final int MODE_WRITE = 2;
    /** Display average rating, or user's on mouse-over, with updates. */
    public static final int MODE_BOTH = 3;
    /** Supply this for memberRating when using MODE_READ. */
    public static final byte NO_RATING = 0;

    public Stars (int mode, boolean halfSize)
    {
        super();
        
        _mode = mode;
        _halfSize = halfSize;

        // add the 10 images whose src url's we mercilessly mutate throughout this widget
        for (int ii = 0; ii < 10; ii ++) {
            Image halfStar = new Image();
            if (_mode != MODE_READ) {
                halfStar.addMouseListener(new RatingMouseListener(ii/2+1));
            }
            add(halfStar);
        }
        update();
    }

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
            starsClicked((byte) _ratingInterval);
        }
        
        protected int _ratingInterval;
    }

    /**
     * This method is called when the mouse pointer is not over the widget, i.e. when it's
     * first initialized, or when the pointer leaves the widget. The subclasser should update
     * the view here, probably by calling {@link #updateStarImages}.
     */
    protected abstract void update ();

    /**
     * This method is called when the mouse pointer enters the widget. The subclasser should
     * update the view here, probably by calling {@link #updateStarImages}.
     */
    protected abstract void update (double rating);
    
    /**
     * This method is called when the user clicks on the widget.
     */
    protected abstract void starsClicked (byte rating);

    /**
     * Are we in half-size mode?
     */
    protected boolean _halfSize;
    
    /**
     * Are we read-only, write-only, or both?
     */
    protected int _mode;
}
