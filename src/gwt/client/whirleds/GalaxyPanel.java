//
// $Id$

package client.whirleds;

import java.util.List;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.web.data.GalaxyData;
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
public class GalaxyPanel extends SmartTable
{
    public GalaxyPanel ()
    {
        super("galaxy", 0, 5);

        int row = 0;
        setText(row++, 0, CWhirleds.msgs.listIntro(), 1, "Intro");

        SmartTable filter = new SmartTable(0, 0);
        filter.setWidth("100%");
        filter.setWidget(0, 0, _popularTags = new FlowPanel(), 1, "PopularTags");

        RowPanel search = new RowPanel();
        _searchInput = MsoyUI.createTextBox("", 255, 20);
        ClickListener doSearch = new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.WHIRLEDS, Args.compose("search", "0", _searchInput.getText()));
            }
        };
        _searchInput.addKeyboardListener(new EnterClickAdapter(doSearch));
        search.add(_searchInput);
        search.add(new Button(CWhirleds.msgs.listSearch(), doSearch), HasAlignment.ALIGN_MIDDLE);
        filter.setWidget(0, 1, search);
        filter.getFlexCellFormatter().setHorizontalAlignment(0, 1, HasAlignment.ALIGN_RIGHT);
        setWidget(row++, 0, filter, 2, null);

        _groupGrid = new PagedGrid(GRID_ROWS, GRID_COLUMNS) {
            protected void displayPageFromClick (int page) {
                Application.go(Page.WHIRLEDS, Args.compose(_action, ""+page, _arg));
            }
            protected Widget createWidget (Object item) {
                return new GroupWidget((Group)item);
            }
            protected String getEmptyMessage () {
                return CWhirleds.msgs.listNoGroups();
            }
        };
        _groupGrid.setWidth("100%");
        setWidget(row++, 0, _groupGrid, 2, null);

        if (CWhirleds.getMemberId() > 0) {
            setWidget(row++, 0, new Button(CWhirleds.msgs.listNewGroup(), new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WHIRLEDS, "edit");
                }
            }), 2, null);
        }

        _currentTag = new FlowPanel();
        _popularTags.clear();
        InlineLabel popularTagsLabel = new InlineLabel(CWhirleds.msgs.listPopularTags() + " ");
        popularTagsLabel.addStyleName("Label");
        _popularTags.add(popularTagsLabel);

        CWhirleds.groupsvc.getGalaxyData(CWhirleds.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                init((GalaxyData)result);
            }
        });
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
                CWhirleds.groupsvc.getGroupsList(CWhirleds.ident, callback);
            }
        });
    }

    protected void init (GalaxyData data)
    {
        // set up our featured whirled
        setWidget(0, 1, new FeaturedWhirledPanel(data.featuredWhirled));

        // set up our popular tags
        if (data.popularTags.size() == 0) {
            _popularTags.add(new InlineLabel(CWhirleds.msgs.listNoPopularTags()));
        } else {
            for (int ii = 0; ii < data.popularTags.size(); ii++) {
                if (ii > 0) {
                    _popularTags.add(new InlineLabel(", "));
                }
                String tag = (String)data.popularTags.get(ii);
                Widget link = Application.createLink(
                    tag, Page.WHIRLEDS, Args.compose("tag", "0", tag));
                link.addStyleName("inline");
                _popularTags.add(link);
            }
            _popularTags.add(_currentTag);
        }
    }

    protected boolean displayTag (final String tag, int page)
    {
        if (tag.equals("")) {
            return false;
        }

        InlineLabel tagLabel = new InlineLabel(CWhirleds.msgs.listCurrentTag() + " " + tag + " ");
        tagLabel.addStyleName("Label");
        _currentTag.add(tagLabel);
        _currentTag.add(new InlineLabel("("));
        Widget clear = Application.createLink(CWhirleds.msgs.listTagClear(), Page.WHIRLEDS, "");
        clear.addStyleName("inline");
        _currentTag.add(clear);
        _currentTag.add(new InlineLabel(")"));

        setModel("tag", tag, page, new ModelLoader() {
            public void loadModel (MsoyCallback callback) {
                CWhirleds.groupsvc.searchForTag(CWhirleds.ident, tag, callback);
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
                CWhirleds.groupsvc.searchGroups(CWhirleds.ident, query, callback);
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

    protected static interface ModelLoader
    {
        public void loadModel (MsoyCallback callback);
    }

    protected class GroupWidget extends SmartTable
    {
        public GroupWidget (final Group group) {
            super("GroupWidget", 2, 2);

            setWidget(0, 0, MediaUtil.createMediaView(
                          group.getLogo(), MediaDesc.THUMBNAIL_SIZE, new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WHIRLEDS, Args.compose("d", group.groupId));
                }
            }), 1, "Logo");
            getFlexCellFormatter().setRowSpan(0, 0, 3);

            setWidget(0, 1, Application.createLink(group.name, Page.WHIRLEDS,
                                                   Args.compose("d", group.groupId)));

            setText(1, 0, group.blurb);

            FlowPanel info = new FlowPanel();
            InlineLabel estab = new InlineLabel(
                CWhirleds.msgs.groupEst(EST_FMT.format(group.creationDate) + ", "));
            estab.addStyleName("EstablishedDate");
            info.add(estab);
            info.add(new InlineLabel(CWhirleds.msgs.listMemberCount("" + group.memberCount)));
            setWidget(2, 0, info);
        }
    }

    protected String _action, _arg;

    protected FlowPanel _popularTags, _currentTag;
    protected TextBox _searchInput;
    protected PagedGrid _groupGrid;

    protected static final int POP_TAG_COUNT = 9;
    protected static final int GRID_ROWS = 4;
    protected static final int GRID_COLUMNS = 2;

    protected static final SimpleDateFormat EST_FMT = new SimpleDateFormat("MMM dd, yyyy");
}
