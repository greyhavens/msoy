//
// $Id$

package client.ui;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.RatingHistoryResult;
import com.threerings.msoy.data.all.RatingResult;
import com.threerings.msoy.data.all.RatingHistoryResult.RatingHistoryEntry;
import com.threerings.msoy.web.gwt.Pages;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedWidget;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.SimpleDataModel;

import client.shell.CShell;
import client.shell.ShellMessages;
import client.util.InfoCallback;
import client.util.Link;

public class Rating extends FlexTable
    implements Stars.StarMouseListener
{
    public interface RateService
    {
        /** Used to notify when the target has been rated by the player. */
        void handleRate (byte newRating, InfoCallback<RatingResult> callback);
    }

    public interface HistoryService
    {
        /** Used to fetch rating history. */
        void getHistory (InfoCallback<RatingHistoryResult> callback);
    }
    /**
     * Construct a new display for the given target with member's previous rating of the
     * target and a specified display mode.
     */
    public Rating (float averageRating, int ratingCount, byte myRating, boolean horiz,
        RateService rateSvc, HistoryService historySvc)
    {
        setStyleName("Rating");

        _rateSvc = rateSvc;
        _historySvc = historySvc;

        _horiz = horiz;
        _averageStars = new Stars(averageRating, true, false, null);

        _ratingCount = new Label();
        _ratingCount.setStyleName(STYLE_COUNT);
        setRatingCount(ratingCount);

        // only validated members can rate things
        boolean writable = (_rateSvc != null) && CShell.isValidated();

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
                setWidget(0, 1, WidgetUtil.makeShim(5, 5));
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
        if (_historySvc != null && CShell.isSupport()) {
            InlineLabel historyLabel = new InlineLabel(_cmsgs.ratingHistory());
            historyLabel.addStyleName("LabelLink");
            historyLabel.addClickHandler(new ClickHandler() {
                public void onClick (ClickEvent event) {
                    toggleRatingHistory();
                }
            });
            int row = getRowCount();
            setWidget(row, 0, historyLabel);
            getFlexCellFormatter().setVerticalAlignment(row, 0, HasAlignment.ALIGN_MIDDLE);
        }
    }

    // from interface Stars.StarMouseListener
    public void starClicked (final byte newRating)
    {
        // we should never get here without a _rateSvc
        _rateSvc.handleRate(newRating, new InfoCallback<RatingResult>() {
            public void onSuccess (RatingResult result) {
                _playerStars.setRating(_memberRating = newRating);
                _averageStars.setRating(result.getRating());
                setRatingCount(result.ratingCount);
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

    protected void setRatingCount (int count)
    {
        String s = String.valueOf(count);
        if (count == 1) {
            _ratingCount.setText(_cmsgs.numberOfRatingsOne(s));
        } else {
            _ratingCount.setText(_cmsgs.numberOfRatings(s));
        }
    }

    protected void toggleRatingHistory ()
    {
         if (_ratingHistory != null) {
             if (_ratingHistory.isShowing()) {
                 _ratingHistory.hide();
             } else {
                 _ratingHistory.show();
             }
             return;
         }

         final PagedWidget<RatingHistoryEntry> pager =
             new PagedWidget<RatingHistoryEntry>(12, PagedWidget.NAV_ON_BOTTOM) {
             @Override public void displayPage (final int page, boolean forceRefresh) {
                 super.displayPage(page, forceRefresh);
                 if (page == 0) {
                     _ratingHistory.center();
                 }
             }

            protected Widget createContents (int start, int count, List<RatingHistoryEntry> list) {
                FlexTable contents = new FlexTable();
                contents.setStyleName("ratingHistory");
                contents.getColumnFormatter().setWidth(0, "150px");
                contents.getColumnFormatter().setWidth(1, "180px");
                contents.getColumnFormatter().setWidth(2, "20px");
                contents.setBorderWidth(1);
                contents.setCellSpacing(0);
                contents.setCellPadding(5);
                FlexCellFormatter formatter = contents.getFlexCellFormatter();

                int tRow = 0;
                for (RatingHistoryEntry history : list) {
                    String fullDate = history.timestamp.toString();
                    // Fri Sep 2006 12:46:12 GMT 2006
                    String date = fullDate.substring(4, 20) + fullDate.substring(26);
                    CShell.log("fullDate: " + fullDate);
                    contents.setText(tRow, 0, date);
                    formatter.setHorizontalAlignment(tRow, 0, HasAlignment.ALIGN_LEFT);

                    String memName = history.member.toString();
                    if (memName.length() > MAX_NAME_LENGTH-3) {
                        memName = memName.substring(0, MAX_NAME_LENGTH) + "...";
                    }
                    contents.setWidget(tRow, 1, Link.create(
                        memName, Pages.PEOPLE, history.member.getId()));
                    formatter.setHorizontalAlignment(tRow, 1, HasAlignment.ALIGN_LEFT);

                    contents.setText(tRow, 2, Byte.toString(history.rating));
                    tRow ++;
                }
                return contents;
            }

            protected String getEmptyMessage () {
                return "No known tag history.";
            }
         };

         // while it's not, let this pager pretend to be a pagedGrid because our CSS is
         // so oddly organized
         pager.setStyleName("pagedGrid");

         _ratingHistory = new BorderedPopup(true);
         _ratingHistory.setWidget(pager);

         // kick off the request to load the history
         _historySvc.getHistory(new InfoCallback<RatingHistoryResult>() {
             public void onSuccess (RatingHistoryResult result) {
                 pager.setModel(new SimpleDataModel<RatingHistoryEntry>(result.ratings), 0);
             }
         });

         // we don't actually show ourselves until we receive the data (bad UI)
    }


    /** True if both Stars are to be laid out on the same row. */
    protected boolean _horiz;

    protected RateService _rateSvc;
    protected HistoryService _historySvc;

    protected byte _memberRating;
    protected Stars _averageStars, _playerStars;
    protected Label _ratingCount, _ratingTip, _ratingDesc;
    protected BorderedPopup _ratingHistory;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);

    protected static final String [] RATING_DESCRIPTIONS = {
        _cmsgs.descRating1(),
        _cmsgs.descRating2(),
        _cmsgs.descRating3(),
        _cmsgs.descRating4(),
        _cmsgs.descRating5()
    };

    protected static final int MAX_NAME_LENGTH = 22;

    protected static final String STYLE_RATING = "ratingText";
    protected static final String STYLE_COUNT = "ratingCount";
}
