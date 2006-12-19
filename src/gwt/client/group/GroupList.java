//
// $Id$

package client.group;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.Group;

import client.shell.MsoyEntryPoint;
import client.util.WebContext;

/**
 * Display the public groups in a sensical manner, including a sorted list of characters that
 * start the groups, allowing people to select a subset of the public groups to view.
 */
public class GroupList extends VerticalPanel
{
    public GroupList(WebContext ctx)
    {
        super();
        _ctx = ctx;

        _errorContainer = new VerticalPanel();
        _errorContainer.setStyleName("groupListErrors");
        add(_errorContainer);

        FlexTable table = new FlexTable();
        add(table);

        VerticalPanel leftPanel = new VerticalPanel();
        _popularTagsContainer = new FlowPanel();
        leftPanel.add(_popularTagsContainer);
        _featuredGroupsContainer = new VerticalPanel();
        leftPanel.add(_featuredGroupsContainer);
        table.setWidget(0, 0, leftPanel);
        table.getFlexCellFormatter().setRowSpan(0, 0, 3);
        
        MyFlexTable search = new MyFlexTable();
        TextBox searchInput = new TextBox();
        searchInput.setMaxLength(255);
        searchInput.setVisibleLength(20);
        DOM.setAttribute(searchInput.getElement(), "id", "searchInput");
        search.setWidget(0, 0, searchInput);
        search.setWidget(0, 1, new Button("Search"));
        search.setWidget(0, 3, new Button("Form New Group"));
        // have the empty cell hog as much space as it can, pushing the from group button to the 
        // right.
        search.getMyFlexCellFormatter().setWidth(0, 2, "100%");
        table.setWidget(0, 1, search);
        
        _characterListContainer = new FlowPanel();
        table.setWidget(1, 1, _characterListContainer);

        _groupListContainer = new VerticalPanel();
        _groupListContainer.add(new HTML("Click a letter above to browse groups that start " +
            "with that character, or complete a search above."));
        table.setWidget(2, 1, _groupListContainer);

        fillPopularTags();
        fillFeaturedGroups();
        fillCharacterList();
    }

    protected void fillCharacterList ()
    {
    }

    protected void fillPopularTags ()
    {
    }

    protected void fillFeaturedGroups ()
    {
    }

    protected void addError (String error) 
    {
        _errorContainer.add(new Label(error));
    }

    protected void clearErrors ()
    {
        _errorContainer.clear();
    }

    protected class MyFlexTable extends FlexTable {
        public class MyFlexCellFormatter extends FlexTable.FlexCellFormatter {
            public void fillWidth (int row, int column) {
                DOM.setStyleAttribute(getElement(row, column), "width", "100%");
            }
        }

        public MyFlexTable () {
            setCellFormatter(new MyFlexCellFormatter());
        }

        public MyFlexCellFormatter getMyFlexCellFormatter() {
            return (MyFlexCellFormatter)getCellFormatter();
        }
    }

    protected WebContext _ctx;
    protected VerticalPanel _errorContainer;
    protected FlowPanel _characterListContainer;
    protected FlowPanel _popularTagsContainer;
    protected VerticalPanel _featuredGroupsContainer;
    protected VerticalPanel _groupListContainer;
}
