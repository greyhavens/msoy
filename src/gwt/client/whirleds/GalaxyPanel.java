//
// $Id$

package client.whirleds;

import java.util.List;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.web.data.GalaxyData;
import com.threerings.msoy.web.data.GroupCard;

import client.shell.Args;
import client.shell.Page;
import client.util.ItemBox;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.MsoyUI;

/**
 * Display the public groups in a sensical manner, including a sorted list of characters that
 * start the groups, allowing people to select a subset of the public groups to view.
 */
public class GalaxyPanel extends VerticalPanel
{
    public GalaxyPanel ()
    {
        setStyleName("galaxy");

        // add our favorites and featured whirled
        SmartTable features = new SmartTable("Features", 0, 0);
        features.setText(0, 0, CWhirleds.msgs.galaxyIntro(), 1, "Intro"); // TODO: favorites
        features.setWidget(0, 1, _featured = new FeaturedWhirledPanel(false, false));
        features.getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);
        features.getFlexCellFormatter().setHorizontalAlignment(0, 1, HasAlignment.ALIGN_CENTER);
        add(features);
        add(WidgetUtil.makeShim(10, 10));

        // now add a UI for browsing and searching Whirleds
        SmartTable browse = new SmartTable("Browse", 0, 0);
        browse.setText(0, 0, CWhirleds.msgs.galaxyBrowseTitle(), 1, "Title");
        browse.setWidget(0, 1, _currentTag = new FlowPanel(), 1, "Current");

        HorizontalPanel search = new HorizontalPanel();
        search.add(_searchInput = MsoyUI.createTextBox("", 255, 20));
        ClickListener doSearch = new ClickListener() {
            public void onClick (Widget sender) {
                Link.go(Page.WHIRLEDS, Args.compose("search", "0", _searchInput.getText()));
            }
        };
        _searchInput.addKeyboardListener(new EnterClickAdapter(doSearch));
        search.add(WidgetUtil.makeShim(5, 5));
        search.add(new Button(CWhirleds.msgs.galaxySearch(), doSearch));
        browse.setWidget(0, 2, search, 1, "Search");
        browse.getFlexCellFormatter().setHorizontalAlignment(0, 2, HasAlignment.ALIGN_RIGHT);
        add(browse);
        add(WidgetUtil.makeShim(5, 5));

        SmartTable contents = new SmartTable("Contents", 0, 0);
        contents.setWidget(0, 0, _popularTags = new FlowPanel(), 1, "tagCloud");
        contents.getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        contents.setWidget(0, 1, WidgetUtil.makeShim(10, 10));
        contents.setWidget(0, 2, _groupGrid = new PagedGrid<GroupCard>(GRID_ROWS, GRID_COLUMNS) {
            protected void displayPageFromClick (int page) {
                Link.go(Page.WHIRLEDS, Args.compose(_action, ""+page, _arg));
            }
            protected Widget createWidget (GroupCard card) {
                return new GroupWidget(card);
            }
            protected String getEmptyMessage () {
                return CWhirleds.msgs.galaxyNoGroups();
            }
        });
        _groupGrid.setWidth("100%");
        contents.getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
        add(contents);

        // add info on creating a Whirled
        add(WidgetUtil.makeShim(10, 10));
        SmartTable create = new SmartTable("Create", 0, 0);
        create.setText(0, 0, CWhirleds.msgs.galaxyCreateTitle(), 3, "Header");
        create.setText(1, 0, CWhirleds.msgs.galaxyCreateBlurb(), 1, "Pitch");
        create.setWidget(1, 1, WidgetUtil.makeShim(10, 10));
        ClickListener onClick = Link.createListener(Page.WHIRLEDS, "edit");
        create.setWidget(1, 2, new Button(CWhirleds.msgs.galaxyCreate(), onClick), 1, "Button");
        add(create);

        CWhirleds.groupsvc.getGalaxyData(CWhirleds.ident, new MsoyCallback<GalaxyData>() {
            public void onSuccess (GalaxyData galaxy) {
                init(galaxy);
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
            public void loadModel (MsoyCallback<List<GroupCard>> callback) {
                // TODO: this eventually needs to be a ServiceBackedDataModel
                CWhirleds.groupsvc.getGroupsList(CWhirleds.ident, callback);
            }
        });
    }

    protected void init (GalaxyData data)
    {
        // set up our featured whirled
        _featured.setWhirleds(data.featuredWhirleds);

        // set up our popular tags
        if (data.popularTags.size() == 0) {
            _popularTags.add(MsoyUI.createLabel(CWhirleds.msgs.galaxyNoPopularTags(), "Link"));
        } else {
            for (String tag : data.popularTags) {
                Widget link = Link.create(
                    tag, Page.WHIRLEDS, Args.compose("tag", "0", tag));
                link.addStyleName("Link");
                link.removeStyleName("inline");
                _popularTags.add(link);
            }
        }
    }

    protected boolean displayTag (final String tag, int page)
    {
        if (tag.equals("")) {
            return false;
        }

        InlineLabel tagLabel = new InlineLabel(CWhirleds.msgs.galaxyCurrentTag(tag) + " ");
        tagLabel.addStyleName("Label");
        _currentTag.add(tagLabel);
        _currentTag.add(new InlineLabel("("));
        Widget clear = Link.create(CWhirleds.msgs.galaxyTagClear(), Page.WHIRLEDS, "");
        clear.addStyleName("inline");
        _currentTag.add(clear);
        _currentTag.add(new InlineLabel(")"));

        setModel("tag", tag, page, new ModelLoader() {
            public void loadModel (MsoyCallback<List<GroupCard>> callback) {
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
            public void loadModel (MsoyCallback<List<GroupCard>> callback) {
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
            loader.loadModel(new MsoyCallback<List<GroupCard>>() {
                public void onSuccess (List<GroupCard> list) {
                    _action = action;
                    _arg = arg;
                    _groupGrid.setModel(new SimpleDataModel<GroupCard>(list), page);
                }
            });
        }
    }

    protected static interface ModelLoader
    {
        void loadModel (MsoyCallback<List<GroupCard>> callback);
    }

    protected class GroupWidget extends ItemBox
    {
        public GroupWidget (GroupCard group) {
            super(group.logo, group.name.toString(), Page.WHIRLEDS,
                  Args.compose("d", group.name.getGroupId()), false);
            int row = getRowCount();
            if (group.population == 0) {
                setHTML(row, 0, "&nbsp;");
            } else {
                setText(row, 0, CWhirleds.msgs.galaxyMemberCount("" + group.population));
            }
            getFlexCellFormatter().setStyleName(row, 0, "Population");
        }
    }

    protected String _action, _arg;

    protected FeaturedWhirledPanel _featured;
    protected FlowPanel _popularTags, _currentTag;
    protected TextBox _searchInput;
    protected PagedGrid<GroupCard> _groupGrid;

    protected static final int POP_TAG_COUNT = 9;
    protected static final int GRID_ROWS = 2;
    protected static final int GRID_COLUMNS = 4;
}
