//
// $Id$

package client.item;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;

import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import client.shell.CShell;
import client.util.MsoyCallback;
import client.util.Stars;

public class ItemRating extends FlexTable
    implements Stars.StarMouseListener
{
    /**
     * Construct a new display for the given item with member's previous rating of the item and a
     * specified display mode.
     */
    public ItemRating (Item item, byte memberRating, boolean horiz)
    {
        _item = item;
        _horiz = horiz;
        _averageStars = new Stars(_item.rating, true, false, null);

        // if we're not logged in, force read-only mode
        if (CShell.isGuest() || !item.isRatable()) {
            setWidget(0, 0, _averageStars);
            return;
        }

        _memberRating = memberRating;
        _playerStars = new Stars(_memberRating, false, false, this);

        _ratingCount = new Label();
        updateRatingCount();

        _ratingTip = new Label();
        _ratingDesc = new Label();

        // Initialize the default context tips
        starMouseOff();

        Label ratingAverage = new Label(CShell.cmsgs.averageRating());
        HTML nbsp = new HTML("&#160;");
        if (_horiz) {
            HorizontalPanel panel = new HorizontalPanel();
            panel.add(_averageStars);
            panel.add(nbsp);
            panel.add(_ratingCount);

            setWidget(0, 0, ratingAverage);
            setWidget(1, 0, panel);

            setWidget(0, 1, WidgetUtil.makeShim(35, 5));

            setWidget(0, 2, _ratingTip);
            setWidget(0, 3, _ratingDesc);
            setWidget(1, 2, _playerStars);
            getFlexCellFormatter().setColSpan(1, 2, 2);

        } else {
            setWidget(0, 0, ratingAverage);
            setWidget(0, 1, _averageStars);
            setWidget(1, 0, _ratingCount);
            setWidget(2, 0, _ratingTip);
            setWidget(2, 1, _playerStars);

            setWidget(3, 0, _ratingDesc);
            setWidget(3, 1, nbsp);
            getFlexCellFormatter().setColSpan(3, 0, 2);
        }

        ratingAverage.setStyleName(STYLE_RATING);
        _ratingTip.setStyleName(STYLE_RATING);
        _ratingDesc.setStyleName(STYLE_RATING);

        _ratingCount.setStyleName(STYLE_COUNT);
    }

    // from interface Stars.StarMouseListener
    public void starClicked (byte newRating)
    {
        final boolean isFirstRating = _memberRating == 0;

        _playerStars.setRating(_memberRating = newRating);
        ItemIdent ident = new ItemIdent(_item.getType(), _item.getPrototypeId());
        CShell.itemsvc.rateItem(CShell.ident, ident, newRating, new MsoyCallback() {
            public void onSuccess (Object result) {
                _averageStars.setRating(_item.rating = ((Float)result).floatValue());

                if (isFirstRating) {
                    _item.ratingCount += 1;
                }

                updateRatingCount();
            }
        });
    }

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
        _ratingTip.setText(_memberRating > 0 ?
                CShell.cmsgs.playerRating() : CShell.cmsgs.playerUnrated());
    }

    protected void updateRatingCount ()
    {
        String s = String.valueOf(_item.ratingCount);

        if (_item.ratingCount == 1) {
            _ratingCount.setText(CShell.cmsgs.numberOfRatingsOne(s));
        } else {
            _ratingCount.setText(CShell.cmsgs.numberOfRatings(s));
        }
    }

    /** True if both Stars are to be layed out on the same row. */
    protected boolean _horiz;

    protected Item _item;
    protected byte _memberRating;
    protected Stars _averageStars, _playerStars;
    protected Label _ratingCount, _ratingTip, _ratingDesc;

    protected static final String [] RATING_DESCRIPTIONS = {
        CShell.cmsgs.descRating1(),
        CShell.cmsgs.descRating2(),
        CShell.cmsgs.descRating3(),
        CShell.cmsgs.descRating4(),
        CShell.cmsgs.descRating5()
    };

    protected static final String STYLE_RATING = "ratingText",
                                  STYLE_COUNT = "ratingCount";
}
