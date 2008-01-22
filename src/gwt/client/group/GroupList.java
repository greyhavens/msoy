//
// $Id$

package client.group;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.RowPanel;

/**
 * Display the public groups in a sensical manner, including a sorted list of characters that
 * start the groups, allowing people to select a subset of the public groups to view.
 */
public class GroupList extends FlexTable
{
    /** The number of columns to show in the PagedGrid */
    public static final int GRID_COLUMNS = 2;

    public GroupList ()
    {
        super();
        setStyleName("groupList");

        int col = 0;
        getFlexCellFormatter().setStyleName(0, col, "Intro");
        setText(0, col++, CGroup.msgs.listIntro());

        getFlexCellFormatter().setStyleName(0, col, "PopularTags");
        setWidget(0, col++, _popularTags = new FlowPanel());

        RowPanel search = new RowPanel();
        _searchInput = MsoyUI.createTextBox("", 255, 20);
        ClickListener doSearch = new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.GROUP, Args.compose("search", "0", _searchInput.getText()));
            }
        };
        _searchInput.addKeyboardListener(new EnterClickAdapter(doSearch));
        search.add(_searchInput);
        search.add(new Button(CGroup.msgs.listSearch(), doSearch), HasAlignment.ALIGN_MIDDLE);
        setWidget(0, col++, search);

        if (CGroup.getMemberId() > 0) {
            getFlexCellFormatter().setHorizontalAlignment(0, col, HasAlignment.ALIGN_RIGHT);
            setWidget(0, col++, new Button(CGroup.msgs.listNewGroup(), new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.GROUP, "edit");
                }
            }));
        } else {
            setText(0, col++, "");
        }

        int rows = (Window.getClientHeight() - Application.HEADER_HEIGHT -
                    HEADER_HEIGHT - NAV_BAR_ETC) / BOX_HEIGHT;
        _groupGrid = new PagedGrid(rows, GRID_COLUMNS) {
            protected void displayPageFromClick (int page) {
                Application.go(Page.GROUP, Args.compose(_action, ""+page, _arg));
            }
            protected Widget createWidget (Object item) {
                return new GroupWidget((Group)item);
            }
            protected String getEmptyMessage () {
                return CGroup.msgs.listNoGroups();
            }
        };
        _groupGrid.setWidth("100%");
        setWidget(1, 0, _groupGrid);
        getFlexCellFormatter().setColSpan(1, 0, getCellCount(0));

        _currentTag = new FlowPanel();
        loadPopularTags();
    }

    public void setArgs (Args args)
    {
        String action = args.get(0, ""), arg = args.get(2, "");
        int page = args.get(1, 0);

        // clear out our status indicators (they'll be put back later)
        _currentTag.clear();
        _searchInput.setText("");

        // group-tag_NN_TAG
        if (action.equals("tag") && displayTag(arg, page)) {
            return;
        }

        // group-search_NN_QUERY
        if (action.equals("search") && displaySearch(arg, page)) {
            return;
        }

        // group-p_NN or group
        setModel("p", "", page, new ModelLoader() {
            public void loadModel (MsoyCallback callback) {
                // TODO: this eventually needs to be a ServiceBackedDataModel
                CGroup.groupsvc.getGroupsList(CGroup.ident, callback);
            }
        });
    }

    protected boolean displayTag (final String tag, int page)
    {
        if (tag.equals("")) {
            return false;
        }

        InlineLabel tagLabel = new InlineLabel(CGroup.msgs.listCurrentTag() + " " + tag + " ");
        DOM.setStyleAttribute(tagLabel.getElement(), "fontWeight", "bold");
        _currentTag.add(tagLabel);
        _currentTag.add(new InlineLabel("("));
        Hyperlink clearLink = Application.createLink(CGroup.msgs.listTagClear(), Page.GROUP, "");
        DOM.setStyleAttribute(clearLink.getElement(), "display", "inline");
        _currentTag.add(clearLink);
        _currentTag.add(new InlineLabel(")"));

        setModel("tag", tag, page, new ModelLoader() {
            public void loadModel (MsoyCallback callback) {
                CGroup.groupsvc.searchForTag(CGroup.ident, tag, callback);
            }
        });
        return true;
    }

    protected boolean displaySearch (final String query, int page)
    {
        if (query.equals("")) {
            return false;
        }
        _searchInput.setText(query);
        setModel("search", query, page, new ModelLoader() {
            public void loadModel (MsoyCallback callback) {
                CGroup.groupsvc.searchGroups(CGroup.ident, query, callback);
            }
        });
        return true;
    }

    protected void setModel (final String action, final String arg, final int page,
                             ModelLoader loader)
    {
        if (action.equals(_action) && arg.equals(_arg)) {
            _groupGrid.displayPage(page, false);
        } else {
            loader.loadModel(new MsoyCallback() {
                public void onSuccess (Object result) {
                    _action = action;
                    _arg = arg;
                    _groupGrid.setModel(new SimpleDataModel((List)result), page);
                }
            });
        }
    }

    protected void loadPopularTags ()
    {
        _popularTags.clear();
        InlineLabel popularTagsLabel = new InlineLabel(CGroup.msgs.listPopularTags() + " ");
        popularTagsLabel.addStyleName("PopularTagsLabel");
        _popularTags.add(popularTagsLabel);

        CGroup.groupsvc.getPopularTags(CGroup.ident, POP_TAG_COUNT, new MsoyCallback() {
            public void onSuccess (Object result) {
                List tags = (List)result;
                if (tags.size() == 0) {
                    _popularTags.add(new InlineLabel(CGroup.msgs.listNoPopularTags()));
                    return;
                }
                for (Iterator iter = tags.iterator(); iter.hasNext(); ) {
                    String tag = (String)iter.next();
                    Hyperlink tagLink = Application.createLink(
                        tag, Page.GROUP, Args.compose("tag", "0", tag));
                    DOM.setStyleAttribute(tagLink.getElement(), "display", "inline");
                    _popularTags.add(tagLink);
                    if (iter.hasNext()) {
                        _popularTags.add(new InlineLabel(", "));
                    }
                }
                _popularTags.add(_currentTag);
            }
        });
    }

    protected static interface ModelLoader
    {
        public void loadModel (MsoyCallback callback);
    }

    protected class GroupWidget extends FlexTable
    {
        GroupWidget (final Group group)
        {
            super();
            setStyleName("GroupWidget");

            setWidget(0, 0, MediaUtil.createMediaView(group.getLogo(), MediaDesc.THUMBNAIL_SIZE,
                                                      new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.GROUP, "" + group.groupId);
                }
            }));
            getFlexCellFormatter().setStyleName(0, 0, "Logo");
            getFlexCellFormatter().setRowSpan(0, 0, 3);

            setWidget(0, 1, Application.createLink(group.name, Page.GROUP, "" + group.groupId));

            FlowPanel info = new FlowPanel();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
            InlineLabel estab = new InlineLabel(
                CGroup.msgs.groupEst(dateFormat.format(group.creationDate) + ", "));
            estab.addStyleName("EstablishedDate");
            info.add(estab);
            info.add(new InlineLabel(CGroup.msgs.listMemberCount("" + group.memberCount)));
            setWidget(1, 0, info);

            setText(2, 0, group.blurb);
        }
    }

    protected String _action, _arg;

    protected FlowPanel _popularTags;
    protected FlowPanel _currentTag;
    protected TextBox _searchInput;
    protected PagedGrid _groupGrid;

    protected static final int HEADER_HEIGHT = 15 /* gap */ + 45 /* top tags, etc. */;
    protected static final int NAV_BAR_ETC = 15 /* gap */ + 20 /* bar height */ + 10 /* gap */;
    protected static final int BOX_HEIGHT = MediaDesc.THUMBNAIL_HEIGHT + 15 /* gap */;

    protected static final int POP_TAG_COUNT = 10;
}
