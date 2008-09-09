//
// $Id$

package client.item;

import client.shell.CShell;
import client.shell.ShellMessages;
import client.util.MsoyCallback;
import client.util.ServiceUtil;
import client.ui.Stars;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.gwt.MemberItemInfo;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.ItemServiceAsync;

public class ItemRating extends FlexTable
    implements Stars.StarMouseListener
{
    /**
     * Construct a new display for the given item with member's previous rating of the item and a
     * specified display mode.
     */
    public ItemRating (Item item, MemberItemInfo memberItemInfo, boolean horiz)
    {
        _item = item;
        _horiz = horiz;
        _averageStars = new Stars(_item.rating, true, false, null);

        // if we're not logged in, force read-only mode
        boolean writable = (_item.isRatable() && ! CShell.isGuest());

        _ratingCount = new Label();
        _ratingCount.setStyleName(STYLE_COUNT);
        updateRatingCount();

        FavoriteIndicator favoriteIndicator = null;

        if (writable) {
            _memberRating = memberItemInfo.memberRating;
            _playerStars = new Stars(_memberRating, false, false, this);

            _ratingTip = new Label();
            _ratingDesc = new Label();

            _ratingTip.setStyleName(STYLE_RATING);
            _ratingDesc.setStyleName(STYLE_RATING);

            // Initialize the default context tips
            starMouseOff();

            // indicate whether this a favorite item of the current member
            favoriteIndicator = new FavoriteIndicator(_item, memberItemInfo);
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
                if (item.catalogId != 0) {
                    setWidget(0, 4, favoriteIndicator);
                    getFlexCellFormatter().setRowSpan(0, 4, 2);
                }
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

                if (item.catalogId != 0) {
                    setWidget(3, 1, favoriteIndicator);
                }
            }
        }
    }

    // from interface Stars.StarMouseListener
    public void starClicked (byte newRating)
    {
        final boolean isFirstRating = _memberRating == 0;
        _playerStars.setRating(_memberRating = newRating);
        ItemIdent ident = _item.getIdent();
        _itemsvc.rateItem(ident, newRating, isFirstRating, new MsoyCallback<Float>() {
            public void onSuccess (Float result) {
                _averageStars.setRating(_item.rating = result);
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
        _ratingTip.setText(_memberRating > 0 ? _cmsgs.playerRating() : _cmsgs.playerUnrated());
    }

    protected void updateRatingCount ()
    {
        String s = String.valueOf(_item.ratingCount);
        if (_item.ratingCount == 1) {
            _ratingCount.setText(_cmsgs.numberOfRatingsOne(s));
        } else {
            _ratingCount.setText(_cmsgs.numberOfRatings(s));
        }
    }

    /** True if both Stars are to be laid out on the same row. */
    protected boolean _horiz;

    protected Item _item;
    protected byte _memberRating;
    protected Stars _averageStars, _playerStars;
    protected Label _ratingCount, _ratingTip, _ratingDesc;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final ItemServiceAsync _itemsvc =
        (ItemServiceAsync) ServiceUtil.bind(GWT.create(ItemService.class), ItemService.ENTRY_POINT);

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
