//
// $Id$

package client.people;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.group.gwt.BrandDetail;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.util.InfoCallback;
import client.util.Link;

/**
 * Displays a list of the groups of which a person is a member.
 */
public class GroupsBlurb extends Blurb
{
    @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return (pdata.groups != null);
    }

    @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);
        setHeader(_msgs.groupsTitle());
        setContent(new GroupsGrid(pdata.groups, pdata.brands, pdata.grantable));
    }

    protected static Widget createEmptyTable (String message, String link, Pages page, String args)
    {
        FlowPanel bits = new FlowPanel();
        bits.add(new InlineLabel(message, false, false, true));
        bits.add(Link.create(link, page, args));
        return bits;
    }

    protected class GroupsGrid extends PagedGrid<GroupCard>
    {
        protected class GroupWidget extends FlowPanel
        {
            public GroupWidget (GroupCard card, BrandDetail brand, boolean grant)
            {
                _brand = brand;
                if (_brand != null) {
                    _shares = brand.getShares(_name.getId());
                }
                _grant = grant;

                setStyleName("Group");
                add(new ThumbBox(card.getLogo(), Pages.GROUPS, "d", card.name.getGroupId()));
                add(Link.create(card.name.toString(), Pages.GROUPS, "d", card.name.getGroupId()));
                add(_brandBit = new FlowPanel());
                updateBrand();
            }

            protected void updateBrand ()
            {
                if (_shares > 0 || _grant) {
                    _brandBit.clear();
                    if (_editing) {
                        _brandBox = new ListBox();
                        _brandBox.addItem(_msgs.groupsNoShares());
                        for (int ii = 1; ii <= BrandDetail.MAX_SHARES; ii ++) {
                            _brandBox.addItem(String.valueOf(ii));
                        }
                        _brandBox.setSelectedIndex(Math.min(_shares, BrandDetail.MAX_SHARES));

                        _brandBit.add(MsoyUI.createLabel(_msgs.groupsBrandShares(""), null));
                        _brandBit.add(_brandBox);

                        _brandBit.add(MsoyUI.createTinyButton("Set", new ClickHandler() {
                            @Override public void onClick (ClickEvent event) {
                                final int newShares = _brandBox.getSelectedIndex();
                                if (newShares == _shares) {
                                    return;
                                }
                                // create a callback that updates the UI
                                InfoCallback<Void> callback = new InfoCallback<Void> () {
                                    @Override public void onSuccess (Void result) {
                                        _brand.setShares(_name.getId(), newShares);
                                        _shares = newShares;
                                        _editing = false;
                                        updateBrand();
                                    }
                                };
                                // then call the server to actually set the new share count
                                _groupsvc.setBrandShares(_brand.group.getGroupId(),
                                    _name.getId(), newShares, callback);
                            }
                        }));
                    } else {
                        _brandBit.add(MsoyUI.createLabel(_msgs.groupsBrandShares((_shares == 0) ?
                            "0" : (_shares + "/" + _brand.getTotalShares())), null));
                        if (_grant) {
                            _brandBit.add(MsoyUI.createTinyButton("Change", new ClickHandler() {
                                @Override public void onClick (ClickEvent event) {
                                    _editing = true;
                                    updateBrand();
                                }
                            }));
                        }
                    }
                }
            }

            protected BrandDetail _brand;
            protected FlowPanel _brandBit;
            protected ListBox _brandBox;
            protected int _shares;
            protected boolean _grant, _editing;
        }

        public GroupsGrid (List<GroupCard> groups, List<BrandDetail> brands, Set<Integer> grants)
        {
            super(GROUP_ROWS, GROUP_COLUMNS, PagedGrid.NAV_ON_BOTTOM);
            _brands = Maps.newHashMap();
            for (BrandDetail brand : brands) {
                _brands.put(brand.group.getGroupId(), brand);
            }
            _grants = grants;
            setModel(new SimpleDataModel<GroupCard>(groups), 0);
        }

        @Override // from PagedGrid
        protected String getEmptyMessage ()
        {
            return _msgs.notInGroupsOther();
        }

        @Override // from PagedGrid
        protected Widget createEmptyContents ()
        {
            if (CShell.getMemberId() != _name.getId()) {
                return super.createEmptyContents();
            }
            return createEmptyTable(_msgs.notInGroupsSelf(),
                                    _msgs.notInGroupsJoin(), Pages.GROUPS, "");
        }

        @Override // from PagedGrid
        protected boolean displayNavi (int items)
        {
            return (items > _rows * _cols);
        }

        @Override // from PagedGrid
        protected Widget createWidget (GroupCard card)
        {
            int groupId = card.name.getGroupId();
            return new GroupWidget(card, _brands.get(groupId), _grants.contains(groupId));
        }

        protected Map<Integer, BrandDetail> _brands;
        protected Set<Integer> _grants;
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final GroupServiceAsync _groupsvc = GWT.create(GroupService.class);

    protected static final int GROUP_COLUMNS = 6;
    protected static final int GROUP_ROWS = 2;
}
