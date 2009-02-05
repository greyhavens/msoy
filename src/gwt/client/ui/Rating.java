//
// $Id$

package client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.data.all.RatingResult;

import com.threerings.gwt.ui.WidgetUtil;

import client.shell.CShell;
import client.shell.ShellMessages;
import client.util.MsoyCallback;

public abstract class Rating extends FlexTable
    implements Stars.StarMouseListener
{
    /**
     * Construct a new display for the given target with member's previous rating of the
     * target and a specified display mode.
     */
    public Rating (float averageRating, int ratingCount, byte myRating, boolean horiz)
    {
        setStyleName("Rating");

        _horiz = horiz;
        _averageStars = new Stars(averageRating, true, false, null);

        _ratingCount = new Label();
        _ratingCount.setStyleName(STYLE_COUNT);
        setRatingCount(ratingCount);

        boolean writable = !CShell.isGuest();

        if (writable) {
            _memberRating = myRating;
            _playerStars = new Stars(_memberRating, false, false, this);

            _ratingTip = new Label();
            _ratingDesc = new Label();

            _ratingTip.setStyleName(STYLE_RATING);
            _ratingDesc.setStyleName(STYLE_RATING);

            // Initialize the default context tips
            starMouseOff();
        }

        Label ratingAverage = new Label(_cmsgs.averageRating());
        ratingAverage.setStyleName(STYLE_RATING);

        HTML nbsp = new HTML("&#160;");
        if (_horiz) {
            HorizontalPanel panel = new HorizontalPanel();
            panel.add(_averageStars);
            panel.add(nbsp);
            panel.add(_ratingCount);

            setWidget(0, 0, ratingAverage);
            setWidget(1, 0, panel);

            if (writable) {
                setWidget(0, 1, WidgetUtil.makeShim(35, 5));
                setWidget(0, 2, _ratingTip);
                setWidget(0, 3, _ratingDesc);
                setWidget(1, 2, _playerStars);
                getFlexCellFormatter().setColSpan(1, 2, 2);
            }

        } else {
            setWidget(0, 0, ratingAverage);
            setWidget(0, 1, _averageStars);
            setWidget(1, 0, _ratingCount);

            if (writable) {
                setWidget(2, 0, _ratingTip);
                setWidget(2, 1, _playerStars);
                setWidget(1, 1, _ratingDesc);
                getFlexCellFormatter().setHorizontalAlignment(1, 1, HasAlignment.ALIGN_CENTER);
            }
        }
    }

    // from interface Stars.StarMouseListener
    public void starClicked (byte newRating)
    {
        _playerStars.setRating(_memberRating = newRating);
        handleRate(newRating, new MsoyCallback<RatingResult>() {
            public void onSuccess (RatingResult result) {
                _averageStars.setRating(result.getRating());
                setRatingCount(result.ratingCount);
            }
        });
    }

    /** Used to notify when the target has been rated by the player. */
    abstract protected void handleRate (byte newRating, MsoyCallback<RatingResult> callback);

    // from interface Stars.StarMouseListener
    public void starMouseOn (byte rating)
    {
        _ratingTip.setVisible( ! _horiz);
        _ratingDesc.setVisible(true);
        _ratingDesc.setText(RATING_DESCRIPTIONS[rating-1]);
    }

    // from interface Stars.StarMouseListener
    public void starMouseOff ()
    {
        _ratingDesc.setVisible(false);
        _ratingTip.setVisible(true);
        _ratingTip.setText(_memberRating > 0 ? _cmsgs.playerRating() : _cmsgs.playerUnrated());
    }

    protected void setRatingCount (int count)
    {
        String s = String.valueOf(count);
        if (count == 1) {
            _ratingCount.setText(_cmsgs.numberOfRatingsOne(s));
        } else {
            _ratingCount.setText(_cmsgs.numberOfRatings(s));
        }
    }

    /** True if both Stars are to be laid out on the same row. */
    protected boolean _horiz;

    protected byte _memberRating;
    protected Stars _averageStars, _playerStars;
    protected Label _ratingCount, _ratingTip, _ratingDesc;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);

    protected static final String [] RATING_DESCRIPTIONS = {
        _cmsgs.descRating1(),
        _cmsgs.descRating2(),
        _cmsgs.descRating3(),
        _cmsgs.descRating4(),
        _cmsgs.descRating5()
    };

   protected static final String STYLE_RATING = "ratingText";
   protected static final String STYLE_COUNT = "ratingCount";
}
