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

import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.web.data.Group;

import client.util.MediaUtil;

/**
 * Display the public groups in a sensical manner, including a sorted list of characters that
 * start the groups, allowing people to select a subset of the public groups to view.
 */
public class GroupList extends VerticalPanel
{
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

        VerticalPanel leftPanel = new VerticalPanel();
        _popularTagsContainer = new FlowPanel();
        _popularTagsContainer.setStyleName("PopularTags");
        leftPanel.add(_popularTagsContainer);
        _featuredGroupsContainer = new VerticalPanel();
        _featuredGroupsContainer.setStyleName("FeaturedGroups");
        DOM.setStyleAttribute(_featuredGroupsContainer.getElement(), "width", "100%");
        leftPanel.add(_featuredGroupsContainer);
        table.setWidget(0, 0, leftPanel);
        table.getFlexCellFormatter().setRowSpan(0, 0, 3);
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

        _characterListContainer = new FlowPanel();
        _characterListContainer.setStyleName("CharacterList");
        table.setWidget(1, 0, _characterListContainer);
        // and again with the ridiculous height
        DOM.setStyleAttribute(table.getFlexCellFormatter().getElement(1, 0), "height", "10px");

        _groupListContainer = new VerticalPanel();
        _groupListContainer.setStyleName("Groups");
        _groupListContainer.add(new HTML(CGroup.msgs.listBrowseTip()));
        table.setWidget(2, 0, _groupListContainer);

        loadPopularTags();
        loadFeaturedGroups();
        loadCharacterList();
    }

    protected void loadCharacterList ()
    {
        _characterListContainer.clear();
        CGroup.groupsvc.getCharacters(CGroup.creds, new AsyncCallback() {
            public void onSuccess (Object result) {
                List characters = (List)result;
                Collections.sort(characters);
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
                    characterLabel.addStyleName("CharacterLabel");
                    characterLabel.addClickListener(new ClickListener() {
                        public void onClick (Widget sender) {
                            loadGroups(character);
                        }
                    });
                    _characterListContainer.add(characterLabel);
                }
            }
            public void onFailure (Throwable caught) {
                CGroup.log("getCharacters failed", caught);
                addError(CGroup.serverError(caught));
            }
        });
    }

    protected void loadPopularTags ()
    {
        _popularTagsContainer.clear();
        InlineLabel popularTagsLabel = new InlineLabel(CGroup.msgs.listPopularTags() + " ");
        popularTagsLabel.addStyleName("PopularTagsLabel");
        _popularTagsContainer.add(popularTagsLabel);
        // TODO: this is dummy data until tags get figured out
        String dummytags[] = { "Muppet", "cute", "scary", "Halloween", "fuzzy", "furry",
            "legs", "horns", "haunted", "spaghetti", "flying" };
        for (int i = 0; i < dummytags.length; i++) {
            _popularTagsContainer.add(new Anchor("", dummytags[i]));
            _popularTagsContainer.add(new InlineLabel(", "));
        }
        Anchor moreLink = new Anchor("", CGroup.msgs.listMore());
        DOM.setAttribute(moreLink.getElement(), "id", "moreLink");
        _popularTagsContainer.add(moreLink);
    }

    protected void loadFeaturedGroups ()
    {
        _featuredGroupsContainer.clear();
        Label featuredGroupsLabel = new Label(CGroup.msgs.listFeatured());
        featuredGroupsLabel.setStyleName("FeaturedGroupsTitle");
        _featuredGroupsContainer.add(featuredGroupsLabel);
        _featuredGroupsContainer.add(new HTML("<h1>TODO</h1>"));
    }

    protected void loadGroups (final String startingCharacter)
    {
        List groups = (List)_groupLists.get(startingCharacter);
        if (groups == null) {
            CGroup.groupsvc.getGroups(CGroup.creds, startingCharacter, new AsyncCallback() {
                public void onSuccess (Object result) {
                    List groups = (List)result;
                    Collections.sort(groups);
                    _groupLists.put(startingCharacter, groups);
                    displayGroups(groups);
                }
                public void onFailure (Throwable caught) {
                    CGroup.log("loadGroups(" + startingCharacter + ") failed", caught);
                    addError(CGroup.serverError(caught));
                }
            });
        } else {
            displayGroups(groups);
        }
    }

    protected void displayGroups (List groups)
    {
        _groupListContainer.clear();
        Iterator groupIter = groups.iterator();
        while (groupIter.hasNext()) {
            _groupListContainer.add(new GroupWidget((Group)groupIter.next()));
        }
    }

    protected void performSearch (final String searchString)
    {
        CGroup.groupsvc.searchGroups(CGroup.creds, searchString, new AsyncCallback() {
            public void onSuccess (Object result) {
                displayGroups((List)result);
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
    protected FlowPanel _characterListContainer;
    protected FlowPanel _popularTagsContainer;
    protected VerticalPanel _featuredGroupsContainer;
    protected VerticalPanel _groupListContainer;

    protected HashMap _groupLists;
}
