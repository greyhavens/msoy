//
// $Id$

package client.groups;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.group.gwt.GroupDetail;

import client.ui.MsoyUI;
import client.ui.PrettyTextPanel;

/**
 * Displays multiple panels: Discussions, Charter, Members, Rooms. Panels are cached and not
 * regenerated when swapping back to one.
 */
public class DetailContentPanel extends FlowPanel
{
    public DetailContentPanel (GroupDetail detail) {
        _detail = detail;

        setStyleName("ContentPanel");
        add(_title = MsoyUI.createSimplePanel(null, "ContentPanelTitle"));

        // contains content and discussions button for css min-height
        FlowPanel container = new FlowPanel();
        container.setStyleName("ContentPanelContainer");
        add(container);
        container.add(_content = new SimplePanel());
        _content.setStyleName("ContentPanelContent");

        // back to discussions button hidden by default
        container.add(_backButton = new Label(_msgs.detailBackToDiscussions()));
        _backButton.setVisible(false);
        _backButton.addStyleName("actionLabel");
        _backButton.addStyleName("ContentBackButton");
        ClickListener backClick = new ClickListener() {
            public void onClick (Widget sender) {
                showDiscussions();
            }
        };
        _backButton.addClickListener(backClick);
    }

    public void showDiscussions () {
        if (_discussions != null && _content.getWidget() == _discussions) {
            return;
        }
        _title.setWidget(new Label(_msgs.detailTabDiscussions()));
        if (_discussions == null) {
            _discussions = new GroupDiscussionsPanel(_detail);
        }
        _content.setWidget(_discussions);
        _backButton.setVisible(false);
    }

    public void showCharter () {
        if (_charter != null && _content.getWidget() == _charter) {
            return;
        }
        _title.setWidget(new Label(_msgs.detailTabCharter()));
        if (_charter == null) {
            String charterText = (_detail.extras.charter == null) ? _msgs.detailNoCharter()
                : _detail.extras.charter;
            _charter = new PrettyTextPanel(charterText);
        }
        _content.setWidget(_charter);
        _backButton.setVisible(true);
    }

    public void showMembers () {
        if (_members != null && _content.getWidget() == _members) {
            return;
        }
        _title.setWidget(new Label(_msgs.detailTabMembers()));
        if (_members == null) {
            _members = new GroupMembersPanel(_detail);
        }
        _content.setWidget(_members);
        _backButton.setVisible(true);
    }

    public void showRooms () {
        if (_rooms != null && _content.getWidget() == _rooms) {
            return;
        }
        _title.setWidget(new Label(_msgs.detailTabRooms()));
        if (_rooms == null) {
            _rooms = new GroupRoomsPanel(_detail);
        }
        _content.setWidget(_rooms);
        _backButton.setVisible(true);
    }

    public void showAwardMedals () {
        if (_awardMedals != null && _content.getWidget() == _awardMedals) {
                return;
        }
        _title.setWidget(new Label(_msgs.detailTabAwardMedals()));
        if (_awardMedals == null) {
            _awardMedals = new AwardMedalsPanel(_detail.group.groupId);
        }
        _content.setWidget(_awardMedals);
        _backButton.setVisible(true);
    }

    /**
     * If a content page requires access to this panel for displaying a temporary sub-content
     * area (that does not get cached), it may receive an instance of this object, and use this
     * method to display it.
     */
    public void showSubContent (String title, Widget content, boolean showDiscussionsLink)
    {
        _title.setWidget(new Label(title));
        _content.setWidget(content);
        _backButton.setVisible(showDiscussionsLink);
    }

    protected GroupDetail _detail;
    protected SimplePanel _title;
    protected SimplePanel _content;
    protected Label _backButton;
    protected GroupDiscussionsPanel _discussions;
    protected PrettyTextPanel _charter;
    protected GroupMembersPanel _members;
    protected GroupRoomsPanel _rooms;
    protected AwardMedalsPanel _awardMedals;
    protected Widget _previousContent;
    protected Widget _previousTitle;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
}
