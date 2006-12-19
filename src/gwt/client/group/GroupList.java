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

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.InlineLabel;

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
        setStyleName("groupList");
        _ctx = ctx;

        _errorContainer = new VerticalPanel();
        _errorContainer.setStyleName("groupListErrors");
        add(_errorContainer);

        FlexTable table = new FlexTable();
        add(table);

        VerticalPanel leftPanel = new VerticalPanel();
        _popularTagsContainer = new FlowPanel();
        _popularTagsContainer.setStyleName("popularTags");
        leftPanel.add(_popularTagsContainer);
        _featuredGroupsContainer = new VerticalPanel();
        _featuredGroupsContainer.setStyleName("featuredGroups");
        DOM.setAttribute(_featuredGroupsContainer.getElement(), "width", "100%");
        leftPanel.add(_featuredGroupsContainer);
        table.setWidget(0, 0, leftPanel);
        table.getFlexCellFormatter().setRowSpan(0, 0, 3);
        table.getFlexCellFormatter().setStyleName(0, 0, "leftColumn");
        
        FlexTable search = new FlexTable();
        TextBox searchInput = new TextBox();
        searchInput.setMaxLength(255);
        searchInput.setVisibleLength(20);
        DOM.setAttribute(searchInput.getElement(), "id", "searchInput");
        search.setWidget(0, 0, searchInput);
        search.setWidget(0, 1, new Button("Search"));
        search.setWidget(0, 3, new Button("Form New Group"));
        DOM.setAttribute(search.getFlexCellFormatter().getElement(0, 2), "width", "100%");
        table.setWidget(0, 1, search);
        // This is a nasty place to set a static height in pixels, but for some reason I cannot
        // fathom, the height of this cell is defaulting to way too large.
        DOM.setAttribute(table.getFlexCellFormatter().getElement(0, 1), "height", "10px");
        
        _characterListContainer = new FlowPanel();
        table.setWidget(1, 0, _characterListContainer);

        _groupListContainer = new VerticalPanel();
        _groupListContainer.setStyleName("groups");
        _groupListContainer.add(new HTML("Click a letter above to browse groups that start " +
            "with that character, or complete a search above."));
        table.setWidget(2, 0, _groupListContainer);

        loadPopularTags();
        loadFeaturedGroups();
        loadCharacterList();
    }

    protected void loadCharacterList ()
    {
    }

    protected void loadPopularTags ()
    {
        _popularTagsContainer.clear();
        InlineLabel popularTagsLabel = new InlineLabel("Popular Tags: ");;
        popularTagsLabel.addStyleName("popularTagsLabel");
        _popularTagsContainer.add(popularTagsLabel);
        // TODO: this is dummy data until tags get figured out
        String dummytags[] = { "Muppet", "cute", "scary", "Halloween", "fuzzy", "furry", 
            "legs", "horns", "haunted", "spaghetti", "flying" };
        for (int i = 0; i < dummytags.length; i++) {
            _popularTagsContainer.add(new Anchor("", dummytags[i]));
            _popularTagsContainer.add(new InlineLabel(", "));
        }
        Anchor moreLink = new Anchor("", "more...");
        DOM.setAttribute(moreLink.getElement(), "id", "moreLink");
        _popularTagsContainer.add(moreLink);
    }

    protected void loadFeaturedGroups ()
    {
        _featuredGroupsContainer.clear();
        Label featuredGroupsLabel = new Label("Featured Groups:");
        featuredGroupsLabel.setStyleName("featuredGroupsTitle");
        _featuredGroupsContainer.add(featuredGroupsLabel);
        _featuredGroupsContainer.add(new HTML("<h1>TODO</h1>"));
    }

    protected void addError (String error) 
    {
        _errorContainer.add(new Label(error));
    }

    protected void clearErrors ()
    {
        _errorContainer.clear();
    }

    protected WebContext _ctx;
    protected VerticalPanel _errorContainer;
    protected FlowPanel _characterListContainer;
    protected FlowPanel _popularTagsContainer;
    protected VerticalPanel _featuredGroupsContainer;
    protected VerticalPanel _groupListContainer;
}
