//
// $Id$

package client.group;

import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Collections;

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

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.Hyperlink;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.web.data.Group;

import client.util.MediaUtil;

/**
 * Display the public groups in a sensical manner, including a sorted list of characters that
 * start the groups, allowing people to select a subset of the public groups to view.
 */
public class GroupList extends VerticalPanel
{
    /** The number of columns to show in the PagedGrid */
    public static final int GRID_COLUMNS = 2;

    /** The number of rows to show in the PagedGrid. */
    public static final int GRID_ROWS = 4;

    public GroupList ()
    {
        super();
        setStyleName("groupList");
        DOM.setStyleAttribute(getElement(), "width", "100%");

        _groupLists = new HashMap();

        _errorContainer = new VerticalPanel();
        _errorContainer.setStyleName("GroupListErrors");
        add(_errorContainer);

        FlexTable table = new FlexTable();
        DOM.setStyleAttribute(table.getElement(), "width", "100%");
        add(table);

        _popularTagsContainer = new FlowPanel();
        _popularTagsContainer.setStyleName("PopularTags");
        table.setWidget(0, 0, _popularTagsContainer);
        table.getFlexCellFormatter().setStyleName(0, 0, "LeftColumn");

        final TextBox searchInput = new TextBox();
        searchInput.setMaxLength(255);
        searchInput.setVisibleLength(20);
        DOM.setAttribute(searchInput.getElement(), "id", "searchInput");
        FlexTable search = new FlexTable();
        ClickListener doSearch = new ClickListener() {
            public void onClick (Widget sender) {
                performSearch(searchInput.getText());
            }
        };
        searchInput.addKeyboardListener(new EnterClickAdapter(doSearch));
        search.setWidget(0, 0, searchInput);
        search.setWidget(0, 1, new Button(CGroup.msgs.listSearch(), doSearch));
        search.setWidget(0, 3, new Button(CGroup.msgs.listNewGroup(), new ClickListener() {
            public void onClick (Widget sender) {
                new GroupEdit().show();
            }
        }));
        DOM.setStyleAttribute(search.getFlexCellFormatter().getElement(0, 2), "width", "100%");
        table.setWidget(0, 1, search);
        // This is a nasty place to set a static height in pixels, but for some reason I cannot
        // fathom, the height of this cell is defaulting to way too large.
        DOM.setStyleAttribute(table.getFlexCellFormatter().getElement(0, 1), "height", "10px");

        _groupListContainer = new PagedGrid(GRID_ROWS, GRID_COLUMNS) {
            protected Widget createWidget (Object item) {
                return new GroupWidget((Group)item);
            }
            protected String getEmptyMessage () {
                return CGroup.msgs.listNoGroups();
            }
        };
        _groupListContainer.setStyleName("Groups");
        _groupListContainer.setWidth("100%");
        table.setWidget(1, 0, _groupListContainer);
        table.getFlexCellFormatter().setColSpan(1, 0, 2);

        loadPopularTags();
        loadGroupsList();
    }

    protected void loadPopularTags ()
    {
        _popularTagsContainer.clear();
        InlineLabel popularTagsLabel = new InlineLabel(CGroup.msgs.listPopularTags() + " ");
        popularTagsLabel.addStyleName("PopularTagsLabel");
        _popularTagsContainer.add(popularTagsLabel);

        CGroup.groupsvc.getPopularTags(CGroup.creds, 10, new AsyncCallback() {
            public void onSuccess (Object result) {
                Iterator iter = ((List)result).iterator();
                if (!iter.hasNext()) {
                    _popularTagsContainer.add(new InlineLabel(CGroup.msgs.listNoPopularTags()));
                } else {
                    while (iter.hasNext()) {
                        final String tag = (String)iter.next();
                        InlineLabel tagLink = new InlineLabel(tag);
                        tagLink.addClickListener(new ClickListener() {
                            public void onClick (Widget sent) {
                                CGroup.groupsvc.searchForTag(CGroup.creds, tag, 
                                    new AsyncCallback() {
                                        public void onSuccess (Object result) {
                                            _groupListContainer.setModel(new SimpleDataModel(
                                                (List)result));
                                        }
                                        public void onFailure (Throwable caught) {
                                            CGroup.log("searchForTag failed", caught);
                                            addError(CGroup.serverError(caught));
                                        }
                                    }
                                );
                            }
                        });
                        _popularTagsContainer.add(tagLink);
                        if (iter.hasNext()) {
                            _popularTagsContainer.add(new InlineLabel(", "));
                        }
                    }
                }
            }
            public void onFailure (Throwable caught) {
                CGroup.log("getPopularTags failed", caught);
                addError(CGroup.serverError(caught));
            }
        });
    }

    protected void loadGroupsList ()
    {
        CGroup.groupsvc.getGroupsList(CGroup.creds, new AsyncCallback() {
            public void onSuccess (Object result) {
                _groupListContainer.setModel(new SimpleDataModel((List)result));
            }
            public void onFailure (Throwable caught) {
                CGroup.log("getGroupsList failed", caught);
                addError(CGroup.serverError(caught));
            }
        });
    }

    protected void performSearch (final String searchString)
    {
        CGroup.groupsvc.searchGroups(CGroup.creds, searchString, new AsyncCallback() {
            public void onSuccess (Object result) {
                _groupListContainer.setModel(new SimpleDataModel((List)result));
            }
            public void onFailure (Throwable caught) {
                CGroup.log("searchGroups(" + searchString + ") failed", caught);
                addError(CGroup.serverError(caught));
            }
        });
    }

    protected void addError (String error)
    {
        _errorContainer.add(new Label(error));
    }

    protected void clearErrors ()
    {
        _errorContainer.clear();
    }

    protected class GroupWidget extends FlexTable
    {
        GroupWidget (Group group)
        {
            super();
            setStyleName("GroupWidget");

            Widget logo = MediaUtil.createMediaView(group.logo, MediaDesc.HALF_THUMBNAIL_SIZE);
            setWidget(0, 0, logo);
            getFlexCellFormatter().setStyleName(0, 0, "Logo");
            getFlexCellFormatter().setRowSpan(0, 0, 2);

            FlowPanel titleLine = new FlowPanel();
            Hyperlink title = new Hyperlink(group.name, "" + group.groupId);
            title.addStyleName("Title");
            titleLine.add(title);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
            InlineLabel establishedDate = new InlineLabel("Est. " +
                dateFormat.format(group.creationDate) + ",");
            establishedDate.addStyleName("EstablishedDate");
            titleLine.add(establishedDate);
            InlineLabel memberCount = new InlineLabel(
                CGroup.msgs.listMemberCount("" + group.memberCount));
            memberCount.addStyleName("MemberCount");
            titleLine.add(memberCount);
            setWidget(0, 1, titleLine);

            InlineLabel blurb = new InlineLabel(group.blurb);
            blurb.setStyleName("Blurb");
            setWidget(1, 0, blurb);
        }
    }

    protected VerticalPanel _errorContainer;
    protected FlowPanel _popularTagsContainer;
    protected PagedGrid _groupListContainer;

    protected HashMap _groupLists;
}
