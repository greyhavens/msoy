//
// $Id$

package client.groups;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.EnterClickAdapter;

import com.threerings.msoy.data.all.MediaDescSize;
import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.group.data.all.Medal;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.Link;
import client.util.MediaUtil;
import client.util.InfoCallback;

public class AwardMedalsPanel extends FlowPanel
{
    public AwardMedalsPanel (int groupId)
    {
        _groupId = groupId;
        setStyleName("awardMedalsPanel");
        reloadMedals();
        init();
    }

    public void reloadMedals ()
    {
        _medals = null;
        _medalMap.clear();
        clearSearchResults();

        _groupsvc.getMedals(_groupId, new InfoCallback<List<Medal>>() {
            public void onSuccess (List<Medal> medals) {
                _medals = medals;
                Collections.sort(_medals);
                // the drop downs need a map of String name to medal instance.
                for (Medal medal : _medals) {
                    _medalMap.put(medal.name, medal);
                }
                if (_medalsValidCommand != null) {
                    _medalsValidCommand.execute();
                    _medalsValidCommand = null;
                }
            }
        });
    }

    protected void init ()
    {
        ClickHandler onClick = new ClickHandler() {
            public void onClick (ClickEvent event) {
                search();
            }
        };

        HorizontalPanel searchBox = new HorizontalPanel();
        searchBox.setSpacing(10);
        searchBox.setStyleName("SearchBox");
        searchBox.add(new Label(_msgs.awardMedalsMemberSearch()));
        searchBox.add(_search = new TextBox());
        _search.addKeyPressHandler(new EnterClickAdapter(onClick));
        searchBox.add(new Button(_msgs.awardMedalsFind(), onClick));
        add(searchBox);
    }

    protected void displaySearchResults (final List<VizMemberName> members)
    {
        if (_medals == null) {
            // we need _medals to be valid before we can proceed with this operation.
            _medalsValidCommand = new Command() {
                public void execute () {
                    displaySearchResults(members);
                }
            };
            return;
        }

        if (members.size() == 0) {
            MsoyUI.info(_msgs.awardMedalsNoMembersFound());
            return;
        }

        for (final VizMemberName member : members) {
            HorizontalPanel row = new HorizontalPanel();
            row.setSpacing(10);
            row.add(MediaUtil.createMediaView(member.getPhoto(), MediaDescSize.HALF_THUMBNAIL_SIZE));
            row.add(Link.create(member.toString(), Pages.PEOPLE, "" + member.getMemberId()));
            // TODO: this will be really inefficient for groups that have a ton of Medals.  This
            // UI should contain only one drop down if at all possible.
            final ListBox awardDrop = createDropDown();
            row.add(awardDrop);
            row.add(new Button(_msgs.awardMedalsAward(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    int selected = Math.max(0, awardDrop.getSelectedIndex());
                    Medal medal = awardDrop.getItemCount() < 0 ?
                        null : _medalMap.get(awardDrop.getValue(selected));
                    grantMedal(member, medal);
                }
            }));
            add(row);
        }
    }

    protected ListBox createDropDown ()
    {
        ListBox dropDown = new ListBox();
        for (Medal medal : _medals) {
            dropDown.addItem(medal.name);
        }
        return dropDown;
    }

    protected void search ()
    {
        _groupsvc.searchGroupMembers(_groupId, _search.getText(),
            new InfoCallback<List<VizMemberName>>() {
                public void onSuccess (List<VizMemberName> members) {
                    displaySearchResults(members);
                }
            });

        clearSearchResults();
    }

    protected void grantMedal (final VizMemberName member, final Medal medal)
    {
        if (medal == null) {
            MsoyUI.error(_msgs.awardMedalsNoMedalChosen());
        }

        _groupsvc.awardMedal(member.getMemberId(), medal.medalId, new InfoCallback<Void>() {
            public void onSuccess (Void result) {
                MsoyUI.info(_msgs.awardMedalsMedalGranted(member.toString(), medal.name));
            }
        });
    }

    protected void clearSearchResults ()
    {
        while (getWidgetCount() > 1) {
            remove(1);
        }
    }

    protected int _groupId;
    protected List<Medal> _medals;
    protected Map<String, Medal> _medalMap = Maps.newHashMap();
    protected Command _medalsValidCommand;
    protected TextBox _search;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final GroupServiceAsync _groupsvc = GWT.create(GroupService.class);
}
