//
// $Id$

package client.group;

import java.util.Iterator;
import java.util.List;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.msoy.web.data.Group;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.InlineLabel;

import client.util.WebContext;
import client.item.ItemUtil;

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
        DOM.setStyleAttribute(getElement(), "width", "100%");
        _ctx = ctx;

        _errorContainer = new VerticalPanel();
        _errorContainer.setStyleName("groupListErrors");
        add(_errorContainer);

        FlexTable table = new FlexTable();
        DOM.setStyleAttribute(table.getElement(), "width", "100%");
        add(table);

        VerticalPanel leftPanel = new VerticalPanel();
        _popularTagsContainer = new FlowPanel();
        _popularTagsContainer.setStyleName("popularTags");
        leftPanel.add(_popularTagsContainer);
        _featuredGroupsContainer = new VerticalPanel();
        _featuredGroupsContainer.setStyleName("featuredGroups");
        DOM.setStyleAttribute(_featuredGroupsContainer.getElement(), "width", "100%");
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
        DOM.setStyleAttribute(search.getFlexCellFormatter().getElement(0, 2), "width", "100%");
        table.setWidget(0, 1, search);
        // This is a nasty place to set a static height in pixels, but for some reason I cannot
        // fathom, the height of this cell is defaulting to way too large.
        DOM.setStyleAttribute(table.getFlexCellFormatter().getElement(0, 1), "height", "10px");
        
        _characterListContainer = new FlowPanel();
        _characterListContainer.setStyleName("characterList");
        table.setWidget(1, 0, _characterListContainer);
        // and again with the ridiculous height
        DOM.setStyleAttribute(table.getFlexCellFormatter().getElement(1, 0), "height", "10px");

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
        _characterListContainer.clear();
        _ctx.groupsvc.getCharacters(_ctx.creds, new AsyncCallback() {
            public void onSuccess (Object result) {
                List characters = (List)result;
                boolean firstCharacter = true;
                Iterator charIter = characters.iterator();
                while (charIter.hasNext()) {
                    if (firstCharacter) {
                        firstCharacter = false;
                    } else {
                        _characterListContainer.add(new InlineLabel(" | "));
                    }
                    final String character = (String)charIter.next();
                    InlineLabel characterLabel = new InlineLabel(character);
                    characterLabel.addStyleName("characterLabel");
                    characterLabel.addClickListener(new ClickListener() {
                        public void onClick (Widget sender) {
                            loadGroups(character);
                        }
                    });
                    _characterListContainer.add(characterLabel);
                }
            }
            public void onFailure (Throwable caught) {
                GWT.log("getCharacters failed", caught);
                addError("Failed to get group prefix characters.");
            }
        });
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

    protected void loadGroups (final String startingCharacter) 
    {
        _groupListContainer.clear();
        _ctx.groupsvc.getGroups(_ctx.creds, startingCharacter, new AsyncCallback() {
            public void onSuccess (Object result) {
                Iterator groupIter = ((List)result).iterator();
                while (groupIter.hasNext()) {
                    _groupListContainer.add(new GroupWidget((Group)groupIter.next()));
                }
            }
            public void onFailure (Throwable caught) {
                GWT.log("loadGroups failed", caught);
                addError("Failed to get groups starting with " + startingCharacter);
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
            setStyleName("groupWidget");
            
            Widget logo = ItemUtil.createMediaView(group.logo, 80, 60);
            logo.setStyleName("logo");
            setWidget(0, 0, logo);
            getFlexCellFormatter().setRowSpan(0, 0, 2);
            
            FlowPanel titleLine = new FlowPanel();
            Hyperlink title = new Hyperlink(group.name, "" + group.groupId);
            title.addStyleName("title");
            titleLine.add(title);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
            InlineLabel establishedDate = new InlineLabel("Est. " + 
                dateFormat.format(group.creationDate));
            establishedDate.addStyleName("establishedDate");
            titleLine.add(establishedDate);
            // TODO: fill in real member count when its been added to GroupRecord and Group
            InlineLabel memberCount = new InlineLabel("42 members");
            memberCount.addStyleName("memberCount");
            titleLine.add(memberCount);
            setWidget(0, 1, titleLine);

            InlineLabel blurb = new InlineLabel(group.blurb);
            blurb.setStyleName("blurb");
            setWidget(1, 0, blurb);
        }
    }

    protected WebContext _ctx;
    protected VerticalPanel _errorContainer;
    protected FlowPanel _characterListContainer;
    protected FlowPanel _popularTagsContainer;
    protected VerticalPanel _featuredGroupsContainer;
    protected VerticalPanel _groupListContainer;
}
