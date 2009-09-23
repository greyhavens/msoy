//
// $Id$

package client.game;

import client.ui.MsoyUI;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.game.gwt.GameInfo;

/**
 * Displays a sorted list of games with a sort menu and sorting list headers.
 */
public abstract class SortedGameListPanel extends GameListPanel
{
    /**
     * Creates a new list with a given sort mode.
     */
    public SortedGameListPanel (GameInfo.Sort sort)
    {
        _sortBox = new ListBox();
        for (int ii = 0; ii < SORT_LABELS.length; ii ++) {
            _sortBox.addItem(SORT_LABELS[ii]);
            if (SORT_VALUES[ii] == sort) {
                _sortBox.setSelectedIndex(ii);
            }
        }
        _sortBox.addChangeHandler(new ChangeHandler() {
            public void onChange (ChangeEvent event) {
                onSortChanged(SORT_VALUES[_sortBox.getSelectedIndex()]);
            }
        });
    }

    protected abstract void onSortChanged (GameInfo.Sort sort);

    @Override // from GameListPanel
    protected int addCustomControls (FlexTable controls, int row)
    {
        controls.setWidget(row, 0, new InlineLabel(_gmsgs.genreSortBy(), false, false, false));
        controls.getFlexCellFormatter().setStyleName(row, 0, "SortBy");
        controls.setWidget(row, 1, _sortBox);
        return super.addCustomControls(controls, ++row);
    }

    @Override // from GameListPanel
    protected Widget createTitle (String text, String styleName, GridColumn column)
    {
        final GameInfo.Sort sort = toSort(column);
        return MsoyUI.createActionLabel(text, styleName, new ClickHandler() {
            public void onClick (ClickEvent event) {
                onSortChanged(sort);
            }
        });
    }

    protected static GameInfo.Sort toSort (GridColumn column)
    {
        switch (column) {
        case CATEGORY: return GameInfo.Sort.BY_GENRE;
        case RATING: return GameInfo.Sort.BY_RATING;
        case NOW_PLAYING: return GameInfo.Sort.BY_ONLINE;
        case NAME: return GameInfo.Sort.BY_NAME;
        default: throw new IllegalArgumentException();
        }
    }

    /** Dropdown of sort methods */
    protected ListBox _sortBox;

    protected static final String[] SORT_LABELS = new String[] {
        _gmsgs.genreSortByRating(),
        _gmsgs.genreSortByNewest(),
        _gmsgs.genreSortByAlphabetical(),
        _gmsgs.genreSortByCategory(),
        _gmsgs.genreSortByNowPlaying()
    };

    protected static final GameInfo.Sort[] SORT_VALUES = new GameInfo.Sort[] {
        GameInfo.Sort.BY_RATING,
        GameInfo.Sort.BY_NEWEST,
        GameInfo.Sort.BY_NAME,
        GameInfo.Sort.BY_GENRE,
        GameInfo.Sort.BY_ONLINE
    };
}
