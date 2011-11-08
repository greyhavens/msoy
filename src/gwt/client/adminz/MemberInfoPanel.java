//
// $Id$

package client.adminz;

import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.admin.gwt.MemberAdminInfo;
import com.threerings.msoy.data.all.CharityInfo;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebCreds;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.BillingUtil;
import client.util.ClickCallback;
import client.util.InfoCallback;
import client.util.Link;
import client.util.MsoyServiceBackedDataModel;

/**
 * Displays admin info for a particular member.
 */
public class MemberInfoPanel extends AdminDataPanel<MemberAdminInfo>
{
    public MemberInfoPanel (int memberId)
    {
        super("memberInfo");

        _adminsvc.getMemberInfo(memberId, AFFILIATE_OF_ROWS * AFFILIATE_OF_COLS, createCallback());
    }

    @Override // from AdminDataPanel
    protected void init (final MemberAdminInfo info)
    {
        if (info == null) {
            addNoDataMessage("No member with that id.");
            return;
        }

        _info = info;

        final SmartTable table = new SmartTable(5, 0);
        add(table);

        int row;
        table.setWidget(0, 0, Link.memberView(info.name), 2, "Name");
        table.setWidget(1, 0, Link.transactionsView(
                            "Transaction history", info.name.getId()));
        table.setWidget(2, 0, new Anchor(BillingUtil.getAdminStatusPage(
                                             info.accountName, info.permaName),
                                         "Billing Transactions", "_blank"));
        table.setWidget(3, 0, Link.create("Stuff Inventory", Pages.STUFF,
                                          MsoyItemType.AVATAR.toByte(), info.name.getId()));

        row = table.addText("Display name:", 1, "Label");
        final TextBox dispName = MsoyUI.createTextBox(
            info.name.toString(), MemberName.MAX_DISPLAY_NAME_LENGTH, 20);
        Button saveName = MsoyUI.createCrUpdateButton(false, null);
        table.setWidget(row, 1, MsoyUI.createButtonPair(dispName, saveName));
        new ClickCallback<Void>(saveName) {
            @Override protected boolean callService () {
                _adminsvc.setDisplayName(info.name.getId(), dispName.getText(), this);
                return true;
            }
            @Override protected boolean gotResult (Void nothing) {
                //?? _info.name = new MemberName(info.name.getMemberId(), dispName.getText());
                return true;
            }
        };

        row = table.addText("Account name:", 1, "Label");
        table.setText(row, 1, info.accountName);

        row = table.addText("Validated:", 1, "Label");
        final CheckBox validated = new CheckBox();
        validated.setValue(info.validated);
        table.setWidget(row, 1, validated);

        validated.addClickHandler(new ClickHandler() {
            public void onClick (ClickEvent event) {
                _adminsvc.setValidated(info.name.getId(), validated.getValue(),
                    new InfoCallback<Void>() {
                        public void onSuccess (Void result) {
                            Popups.info("Validated flag changed successfully");
                        }
                    });
            }
        });

        row = table.addText("Perma name:", 1, "Label");
        final TextBox permaName = MsoyUI.createTextBox(
            info.permaName == null ? "" : info.permaName,
            MemberName.MAXIMUM_PERMANAME_LENGTH, 20);
        Button savePermaName = MsoyUI.createCrUpdateButton(false, null);
        table.setWidget(row, 1, MsoyUI.createButtonPair(permaName, savePermaName));
        new ClickCallback<Void>(savePermaName) {
            @Override protected boolean callService () {
                _adminsvc.setPermaName(info.name.getId(), permaName.getText(), this);
                return true;
            }
            @Override protected boolean gotResult (Void nothing) {
                return true;
            }
        };

        final ListBox role = new ListBox();
        for (WebCreds.Role rtype : WebCreds.Role.values()) {
            role.addItem(rtype.toString());
        }
        role.setSelectedIndex(info.role.ordinal());
        role.setEnabled(info.role != WebCreds.Role.PERMAGUEST &&
                        CShell.creds.role.ordinal() > info.role.ordinal());

        new ClickCallback<Void>(role) {
            @Override protected boolean callService () {
                _role = Enum.valueOf(
                    WebCreds.Role.class, role.getItemText(role.getSelectedIndex()));
                if (_role == info.role) {
                    return false; // we're reverting due to failure, so do nothing
                }
                _adminsvc.setRole(info.name.getId(), _role, this);
                return true;
            }
            @Override protected boolean gotResult (Void result) {
                info.role = _role;
                MsoyUI.info(_msgs.mipChangedRole(_role.toString()));
                return true;
            }
            @Override public void onFailure (Throwable cause) {
                super.onFailure(cause);
                role.setSelectedIndex(info.role.ordinal());
            }
            protected WebCreds.Role _role;
        };

        row = table.addText("Role:", 1, "Label");
        table.setWidget(row, 1, role);

        row = table.addText("Coins:", 1, "Label");
        table.setText(row, 1, ""+info.flow);

        row = table.addText("Accum Coins:", 1, "Label");
        table.setText(row, 1, ""+info.accFlow);

        row = table.addText("Bars:", 1, "Label");
        table.setText(row, 1, ""+info.gold);

        row = table.addText("Sessions:", 1, "Label");
        table.setText(row, 1, ""+info.sessions);

        row = table.addText("Session Mins:", 1, "Label");
        table.setText(row, 1, ""+info.sessionMinutes);

        row = table.addText("Last session:", 1, "Label");
        table.setText(row, 1, ""+info.lastSession);

        row = table.addText("Affiliate:", 1, "Label");
        if (info.affiliate != null) {
            table.setWidget(row, 1, infoLink(info.affiliate));
        } else {
            table.setText(row, 1, "none");
        }

        row = table.addText("Affiliate of:", 1, "Label");
        FlowPanel affiliateOf = new FlowPanel();
        int maxInline = AFFILIATE_OF_ROWS * AFFILIATE_OF_COLS;
        for (int ii = 0; ii < Math.min(maxInline, info.affiliateOf.size()); ii++) {
            if (ii > 0) {
                affiliateOf.add(MsoyUI.createHTML(", ", "inline"));
            }
            affiliateOf.add(infoLink(info.affiliateOf.get(ii)));
        }
        if (info.affiliateOfCount > maxInline) {
            final int affiliateOfRow = row;
            Button more = new Button("See All " + info.affiliateOfCount + "...",
                new ClickHandler() {
                public void onClick (ClickEvent event) {
                    table.setWidget(affiliateOfRow, 1, new AffiliateOfGrid());
                }
            });
            affiliateOf.add(MsoyUI.createHTML(" ", "inline"));
            affiliateOf.add(more);
        }
        table.setWidget(row, 1, affiliateOf);

        final CheckBox charity = new CheckBox();
        charity.setValue(info.charity);
        row = table.addWidget(charity, 1, "Label");
        table.setText(row, 1, "Make this member a charity");

        final CheckBox coreCharity = new CheckBox(
            "This charity can be selected randomly when a member " +
            "making a purchase has not already selected a charity.");
        coreCharity.setValue(info.coreCharity);
        coreCharity.setEnabled(charity.getValue());
        table.setWidget(++row, 1, coreCharity);

        row = table.addText("Charity Description:", 1, "Label");
        final TextArea charityDescription = new TextArea();
        charityDescription.setText(info.charityDescription);
        charityDescription.setEnabled(charity.getValue());
        charityDescription.setStylePrimaryName("CharityDescription");
        table.setWidget(row, 1, charityDescription);

        charity.addClickHandler(new ClickHandler() {
            public void onClick (ClickEvent event) {
                if (charity.getValue()) {
                    coreCharity.setEnabled(true);
                    charityDescription.setEnabled(true);
                } else {
                    coreCharity.setEnabled(false);
                    coreCharity.setValue(false);
                    charityDescription.setEnabled(false);
                    charityDescription.setText("");
                }
            }
        });

        table.setWidget(++row, 1, new Button("Update Charity Info",  new ClickHandler() {
            public void onClick (ClickEvent event) {
                final boolean isCharity = charity.getValue();
                final boolean isCoreCharity = coreCharity.getValue();
                final String description = charityDescription.getText();
                AsyncCallback<Void> callback = new InfoCallback<Void>() {
                    @Override public void onFailure (Throwable caught) {
                        super.onFailure(caught);
                        charity.setValue(info.charity);
                        coreCharity.setValue(info.coreCharity);
                        coreCharity.setEnabled(info.charity);
                        charityDescription.setText(info.charityDescription);
                        charityDescription.setEnabled(info.charity);
                    }
                    public void onSuccess (Void result) {
                        info.charity = isCharity;
                        info.coreCharity = isCoreCharity;
                        info.charityDescription = description;
                        coreCharity.setEnabled(info.charity);
                        charityDescription.setEnabled(isCharity);
                        MsoyUI.info(_msgs.mipUpdatedCharityStatus());
                    }
                };
                if (isCharity) {
                    _adminsvc.setCharityInfo(new CharityInfo(info.name.getId(), isCoreCharity,
                        charityDescription.getText()), callback);
                } else {
                    _adminsvc.removeCharityStatus(info.name.getId(), callback);
                }
            }
        }));
    }

    protected static Widget infoLink (MemberName name)
    {
        return Link.create("" + name, Pages.ADMINZ, "info", name.getId());
    }

    protected class AffiliateOfGrid extends PagedGrid<MemberName>
    {
        public AffiliateOfGrid () {
            super(AFFILIATE_OF_ROWS, AFFILIATE_OF_COLS);
            setModel(new MsoyServiceBackedDataModel<MemberName, List<MemberName>>() {
                @Override
                protected void callFetchService (int start, int count, boolean needCount,
                    AsyncCallback<List<MemberName>> callback)
                {
                    _adminsvc.getAffiliates(_info.name.getId(), start, count, callback);
                }

                @Override
                protected int getCount (List<MemberName> result)
                {
                    return _info.affiliateOfCount;
                }

                @Override
                protected List<MemberName> getRows (List<MemberName> result)
                {
                    return result;
                }
            }, 1);
            addStyleName("AffiliateOf");
        }

        protected Widget createWidget (MemberName item) {
            return infoLink(item);
        }

        protected String getEmptyMessage () {
            return "No one";
        }
    }

    protected MemberAdminInfo _info;

    protected static final int AFFILIATE_OF_ROWS = 10;
    protected static final int AFFILIATE_OF_COLS = 4;
}
