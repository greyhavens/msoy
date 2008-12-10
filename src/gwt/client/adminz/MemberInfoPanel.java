//
// $Id$

package client.adminz;

import java.util.EnumSet;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.admin.gwt.MemberAdminInfo;
import com.threerings.msoy.data.all.CharityInfo;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebCreds;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.BillingURLs;
import client.util.ClickCallback;
import client.util.Link;
import client.util.MsoyCallback;

import client.util.ServiceUtil;

/**
 * Displays admin info for a particular member.
 */
public class MemberInfoPanel extends SmartTable
{
    public MemberInfoPanel (int memberId)
    {
        super("memberInfo", 0, 5);

        _adminsvc.getMemberInfo(memberId, new MsoyCallback<MemberAdminInfo>() {
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
        setWidget(2, 0, new Anchor(BillingURLs.getUserStatusPage(info.accountName, info.permaName), 
            "Billing Transactions", "_blank"));
        
        row = addText("Account name:", 1, "Label");
        setText(row, 1, info.accountName);

        row = addText("Perma name:", 1, "Label");
        setText(row, 1, info.permaName == null ? "" : info.permaName);

        final ListBox role = new ListBox();
        for (WebCreds.Role rtype : EnumSet.allOf(WebCreds.Role.class)) {
            role.addItem(rtype.toString());
        }
        role.setSelectedIndex(info.role.ordinal());
        role.setEnabled(CShell.creds.role.ordinal() > info.role.ordinal());

        new ClickCallback<Void>(role) {
            @Override protected boolean callService () {
                _role = Enum.valueOf(WebCreds.Role.class, role.getItemText(role.getSelectedIndex()));
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
            public void onFailure (Throwable cause) {
                super.onFailure(cause);
                role.setSelectedIndex(info.role.ordinal());
            }
            protected WebCreds.Role _role;
        };

        row = addText("Role:", 1, "Label");
        setWidget(row, 1, role);

        row = addText("Flow:", 1, "Label");
        setText(row, 1, ""+info.flow);

        row = addText("Accum Flow:", 1, "Label");
        setText(row, 1, ""+info.accFlow);

        row = addText("Gold:", 1, "Label");
        setText(row, 1, ""+info.gold);

        row = addText("Sessions:", 1, "Label");
        setText(row, 1, ""+info.sessions);

        row = addText("Session Mins:", 1, "Label");
        setText(row, 1, ""+info.sessionMinutes);

        row = addText("Last session:", 1, "Label");
        setText(row, 1, ""+info.lastSession);

        row = addText("Humanity:", 1, "Label");
        setText(row, 1, ""+info.humanity);

        row = addText("Inviter:", 1, "Label");
        if (info.inviter != null) {
            setWidget(row, 1, infoLink(info.inviter));
        } else {
            setText(row, 1, "none");
        }

        row = addText("Invited:", 1, "Label");
        FlowPanel invited = new FlowPanel();
        for (int ii = 0; ii < info.invitees.size(); ii++) {
            if (ii > 0) {
                invited.add(MsoyUI.createHTML(", ", "inline"));
            }
            invited.add(infoLink(info.invitees.get(ii)));
        }
        setWidget(row, 1, invited);

        final CheckBox charity = new CheckBox();
        charity.setChecked(info.charity);
        row = addWidget(charity, 1, "Label");
        setText(row, 1, "Make this member a charity");
        
        final CheckBox coreCharity = new CheckBox("This charity can be selected randomly when a member " +
        		"making a purchase has not already selected a charity.");
        coreCharity.setChecked(info.coreCharity);
        coreCharity.setEnabled(charity.isChecked());
        setWidget(++row, 1, coreCharity);
        
        row = addText("Charity Description:", 1, "Label");
        final TextArea charityDescription = new TextArea();
        charityDescription.setText(info.charityDescription);
        charityDescription.setEnabled(charity.isChecked());
        charityDescription.setStylePrimaryName("CharityDescription");
        setWidget(row, 1, charityDescription);
        
        charity.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                if (charity.isChecked()) {
                    coreCharity.setEnabled(true);
                    charityDescription.setEnabled(true);
                } else {
                    coreCharity.setEnabled(false);
                    coreCharity.setChecked(false);
                    charityDescription.setEnabled(false);
                    charityDescription.setText("");
                }
            }
        });
        
        setWidget(++row, 1, new Button("Update Charity Info",  new ClickListener() {
            public void onClick (Widget sender) {
                final boolean isCharity = charity.isChecked();
                final boolean isCoreCharity = coreCharity.isChecked();
                final String description = charityDescription.getText();
                AsyncCallback<Void> callback = new MsoyCallback<Void>() {
                    public void onFailure (Throwable caught) {
                        super.onFailure(caught);
                        charity.setChecked(info.charity);
                        coreCharity.setChecked(info.coreCharity);
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
    
    protected Widget infoLink (MemberName name)
    {
        return Link.create("" + name, Pages.ADMINZ, Args.compose("info", name.getMemberId()));
    }

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
