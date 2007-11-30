//
// $Id$

package client.group;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.shell.Application;
import client.shell.Page;
import client.util.MediaUtil;

/**
 * Display the public groups in a sensical manner, including a sorted list of characters that
 * start the groups, allowing people to select a subset of the public groups to view.
 */
public class GroupList extends VerticalPanel
{
    /** The number of columns to show in the PagedGrid */
    public static final int GRID_COLUMNS = 2;

    public GroupList () 
    {
        this(null);
    }

    public GroupList (String tag)
    {
        super();
        setStyleName("groupList");
        DOM.setStyleAttribute(getElement(), "width", "100%");

        _errorContainer = new VerticalPanel();
        _errorContainer.setStyleName("GroupListErrors");
        add(_errorContainer);

        FlexTable table = new FlexTable();
        DOM.setStyleAttribute(table.getElement(), "width", "100%");
        add(table);

        _popularTags = new FlowPanel();
        _popularTags.setStyleName("PopularTags");
        table.setWidget(0, 0, _popularTags);
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
        if (CGroup.getMemberId() > 0) {
            search.setWidget(0, 3, new Button(CGroup.msgs.listNewGroup(), new ClickListener() {
                public void onClick (Widget sender) {
                    new GroupEdit().show();
                }
            }));
        } else {
            search.setWidget(0, 3, new Label(""));
        }
        DOM.setStyleAttribute(search.getFlexCellFormatter().getElement(0, 2), "width", "100%");
        table.setWidget(0, 1, search);
        // This is a nasty place to set a static height in pixels, but for some reason I cannot
        // fathom, the height of this cell is defaulting to way too large.
        DOM.setStyleAttribute(table.getFlexCellFormatter().getElement(0, 1), "height", "10px");

        int rows = (Window.getClientHeight() - Application.HEADER_HEIGHT -
                    HEADER_HEIGHT - NAV_BAR_ETC) / BOX_HEIGHT;
        _groupGrid = new PagedGrid(rows, GRID_COLUMNS) {
            protected Widget createWidget (Object item) {
                return new GroupWidget((Group)item);
            }
            protected String getEmptyMessage () {
                return CGroup.msgs.listNoGroups();
            }
        };
        _groupGrid.setWidth("100%");
        table.setWidget(1, 0, _groupGrid);
        table.getFlexCellFormatter().setColSpan(1, 0, 2);

        _currentTag = new FlowPanel();
        loadPopularTags();
        loadGroupsList(tag);
    }

    protected void loadPopularTags ()
    {
        _popularTags.clear();
        InlineLabel popularTagsLabel = new InlineLabel(CGroup.msgs.listPopularTags() + " ");
        popularTagsLabel.addStyleName("PopularTagsLabel");
        _popularTags.add(popularTagsLabel);

        CGroup.groupsvc.getPopularTags(CGroup.ident, 10, new AsyncCallback() {
            public void onSuccess (Object result) {
                Iterator iter = ((List)result).iterator();
                if (!iter.hasNext()) {
                    _popularTags.add(new InlineLabel(CGroup.msgs.listNoPopularTags()));
                } else {
                    while (iter.hasNext()) {
                        final String tag = (String)iter.next();
                        Hyperlink tagLink = Application.createLink(tag, "group", "tag=" + tag);
                        DOM.setStyleAttribute(tagLink.getElement(), "display", "inline");
                        _popularTags.add(tagLink);
                        if (iter.hasNext()) {
                            _popularTags.add(new InlineLabel(", "));
                        }
                    }
                    _popularTags.add(_currentTag);
                }
            }
            public void onFailure (Throwable caught) {
                CGroup.log("getPopularTags failed", caught);
                addError(CGroup.serverError(caught));
            }
        });
    }

    protected void loadGroupsList (final String tag)
    {
        AsyncCallback groupsListCallback = new AsyncCallback() {
            public void onSuccess (Object result) {
                _groupGrid.setModel(new SimpleDataModel((List)result), 0);
                _currentTag.clear();
                if (tag != null) {
                    InlineLabel tagLabel = new InlineLabel(CGroup.msgs.listCurrentTag() + " " + 
                        tag + " ");
                    DOM.setStyleAttribute(tagLabel.getElement(), "fontWeight", "bold");
                    _currentTag.add(tagLabel);
                    _currentTag.add(new InlineLabel("("));
                    Hyperlink clearLink = Application.createLink(
                        CGroup.msgs.listTagClear(), "group", "");
                    DOM.setStyleAttribute(clearLink.getElement(), "display", "inline");
                    _currentTag.add(clearLink);
                    _currentTag.add(new InlineLabel(")"));
                }
            }
            public void onFailure (Throwable caught) {
                CGroup.log("getGroupsList failed", caught);
                addError(CGroup.serverError(caught));
            }
        };
        
        if (tag != null) {
            CGroup.groupsvc.searchForTag(CGroup.ident, tag, groupsListCallback);
        } else {
            CGroup.groupsvc.getGroupsList(CGroup.ident, groupsListCallback);
        }
    }

    protected void performSearch (final String searchString)
    {
        CGroup.groupsvc.searchGroups(CGroup.ident, searchString, new AsyncCallback() {
            public void onSuccess (Object result) {
                _groupGrid.setModel(new SimpleDataModel((List)result), 0);
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
        GroupWidget (final Group group)
        {
            super();
            setStyleName("GroupWidget");

            Widget logo = MediaUtil.createMediaView(group.getLogo(), MediaDesc.THUMBNAIL_SIZE);
            setWidget(0, 0, logo);
            getFlexCellFormatter().setStyleName(0, 0, "Logo");
            getFlexCellFormatter().setRowSpan(0, 0, 2);
            if (logo instanceof Image) {
                ((Image) logo).addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        Application.go(Page.GROUP, "" + group.groupId);
                    }
                });
            }

            FlowPanel titleLine = new FlowPanel();
            Hyperlink title = Application.createLink(group.name, "group", "" + group.groupId);
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
    protected FlowPanel _popularTags;
    protected FlowPanel _currentTag;
    protected PagedGrid _groupGrid;

    protected static final int HEADER_HEIGHT = 15 /* gap */ + 45 /* top tags, etc. */;
    protected static final int NAV_BAR_ETC = 15 /* gap */ + 20 /* bar height */ + 10 /* gap */;
    protected static final int BOX_HEIGHT = MediaDesc.THUMBNAIL_HEIGHT + 15 /* gap */;
}
