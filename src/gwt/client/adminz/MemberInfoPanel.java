//
// $Id$

package client.adminz;

import java.util.EnumSet;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.admin.gwt.MemberAdminInfo;
import com.threerings.msoy.data.all.CharityInfo;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebCreds;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.BillingUtil;
import client.util.ClickCallback;
import client.util.Link;
import client.util.InfoCallback;

import client.util.ServiceUtil;

/**
 * Displays admin info for a particular member.
 */
public class MemberInfoPanel extends SmartTable
{
    public MemberInfoPanel (int memberId)
    {
        super("memberInfo", 0, 5);

        _adminsvc.getMemberInfo(memberId, new InfoCallback<MemberAdminInfo>() {
            public void onSuccess (MemberAdminInfo info) {
                init(info);
            }
        });
    }

    protected void init (final MemberAdminInfo info)
    {
        if (info == null) {
            setText(0, 0, "No member with that id.");
            return;
        }

        int row;
        setWidget(0, 0, Link.memberView(info.name), 2, "Name");
        setWidget(1, 0, Link.transactionsView("Transaction history", info.name.getMemberId()));
        setWidget(2, 0, new Anchor(BillingUtil.getUserStatusPage(info.accountName, info.permaName),
            "Billing Transactions", "_blank"));
        setWidget(3, 0, Link.create("Stuff Inventory", Pages.STUFF, Args.compose(Item.AVATAR,
            info.name.getMemberId())));

        row = addText("Display name:", 1, "Label");
        final TextBox dispName = MsoyUI.createTextBox(
            info.name.toString(), MemberName.MAX_DISPLAY_NAME_LENGTH, 30);
        Button saveName = MsoyUI.createCrUpdateButton(false, null);
        setWidget(row, 1, MsoyUI.createButtonPair(dispName, saveName));
        new ClickCallback<Void>(saveName) {
            @Override protected boolean callService () {
                _adminsvc.setDisplayName(info.name.getMemberId(), dispName.getText(), this);
                return true;
            }
            @Override protected boolean gotResult (Void nothing) {
                //?? _info.name = new MemberName(info.name.getMemberId(), dispName.getText());
                return true;
            }
        };

        row = addText("Account name:", 1, "Label");
        setText(row, 1, info.accountName);

        row = addText("Perma name:", 1, "Label");
        setText(row, 1, info.permaName == null ? "" : info.permaName);

        final ListBox role = new ListBox();
        for (WebCreds.Role rtype : EnumSet.allOf(WebCreds.Role.class)) {
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
                _adminsvc.setRole(info.name.getMemberId(), _role, this);
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

        row = addText("Role:", 1, "Label");
        setWidget(row, 1, role);

        row = addText("Coins:", 1, "Label");
        setText(row, 1, ""+info.flow);

        row = addText("Accum Coins:", 1, "Label");
        setText(row, 1, ""+info.accFlow);

        row = addText("Bars:", 1, "Label");
        setText(row, 1, ""+info.gold);

        row = addText("Sessions:", 1, "Label");
        setText(row, 1, ""+info.sessions);

        row = addText("Session Mins:", 1, "Label");
        setText(row, 1, ""+info.sessionMinutes);

        row = addText("Last session:", 1, "Label");
        setText(row, 1, ""+info.lastSession);

        row = addText("Humanity:", 1, "Label");
        final Label humanityLabel = new Label(""+info.humanity);
        Button resetHumanity = new Button("Reset");
        setWidget(row, 1, MsoyUI.createButtonPair(humanityLabel, resetHumanity));
        new ClickCallback<Integer>(resetHumanity) {
            @Override protected boolean callService () {
                _adminsvc.resetHumanity(info.name.getMemberId(), this);
                return true;
            }
            @Override protected boolean gotResult (Integer newHumanity) {
                info.humanity = newHumanity;
                humanityLabel.setText(""+info.humanity);
                return true;
            }
        };

        row = addText("Affiliate:", 1, "Label");
        if (info.affiliate != null) {
            setWidget(row, 1, infoLink(info.affiliate));
        } else {
            setText(row, 1, "none");
        }

        row = addText("Affiliate of:", 1, "Label");
        FlowPanel affiliateOf = new FlowPanel();
        int maxInline = AFFILIATE_OF_ROWS * AFFILIATE_OF_COLS;
        for (int ii = 0; ii < Math.min(maxInline, info.affiliateOf.size()); ii++) {
            if (ii > 0) {
                affiliateOf.add(MsoyUI.createHTML(", ", "inline"));
            }
            affiliateOf.add(infoLink(info.affiliateOf.get(ii)));
        }
        if (info.affiliateOf.size() > maxInline) {
            final int affiliateOfRow = row;
            Button more = new Button("More...", new ClickHandler() {
                public void onClick (ClickEvent event) {
                    setWidget(affiliateOfRow, 1, new AffiliateOfGrid(info.affiliateOf));
                }
            });
            affiliateOf.add(more);
        }
        setWidget(row, 1, affiliateOf);

        final CheckBox charity = new CheckBox();
        charity.setValue(info.charity);
        row = addWidget(charity, 1, "Label");
        setText(row, 1, "Make this member a charity");

        final CheckBox coreCharity = new CheckBox(
            "This charity can be selected randomly when a member " +
            "making a purchase has not already selected a charity.");
        coreCharity.setValue(info.coreCharity);
        coreCharity.setEnabled(charity.getValue());
        setWidget(++row, 1, coreCharity);

        row = addText("Charity Description:", 1, "Label");
        final TextArea charityDescription = new TextArea();
        charityDescription.setText(info.charityDescription);
        charityDescription.setEnabled(charity.getValue());
        charityDescription.setStylePrimaryName("CharityDescription");
        setWidget(row, 1, charityDescription);

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

        setWidget(++row, 1, new Button("Update Charity Info",  new ClickHandler() {
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
                    _adminsvc.setCharityInfo(new CharityInfo(info.name.getMemberId(), isCoreCharity,
                        charityDescription.getText()), callback);
                } else {
                    _adminsvc.removeCharityStatus(info.name.getMemberId(), callback);
                }
            }
        }));
    }

    protected static Widget infoLink (MemberName name)
    {
        return Link.create("" + name, Pages.ADMINZ, Args.compose("info", name.getMemberId()));
    }

    protected static class AffiliateOfGrid extends PagedGrid<MemberName>
    {
        public AffiliateOfGrid (List<MemberName> affiliateOf)
        {
            super(AFFILIATE_OF_ROWS, AFFILIATE_OF_COLS);
            setModel(new SimpleDataModel<MemberName>(affiliateOf), 1);
            addStyleName("AffiliateOf");
        }

        protected Widget createWidget (MemberName item)
        {
            return infoLink(item);
        }

        protected String getEmptyMessage ()
        {
            return "No one";
        }
    }

    protected static final int AFFILIATE_OF_ROWS = 10;
    protected static final int AFFILIATE_OF_COLS = 4;

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
