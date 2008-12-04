//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.AccountInfo;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.shell.CShell;
import client.shell.LogonPanel;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.ui.TongueBox;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays account information, allows twiddling.
 */
public class EditAccountPanel extends FlowPanel
{
    public EditAccountPanel ()
    {
        setStyleName("editAccount");

        if (CShell.getMemberId() == 0) {
            SmartTable table = new SmartTable(0, 10);
            table.setText(0, 0, _msgs.editMustLogon(), 1, "Header");
            table.setWidget(1, 0, new LogonPanel());
            add(new TongueBox(null, table));
            return;
        }

        _usersvc.getAccountInfo(new MsoyCallback<AccountInfo>() {
            public void onSuccess (AccountInfo info) {
                init(info);
            }
        });
    }

    protected void init (final AccountInfo accountInfo)
    {
        _accountInfo = accountInfo;

        // configure or display permaname interface
        SmartTable table = new SmartTable(0, 10);
        if (CShell.creds.permaName == null) {
            table.setText(0, 0, _msgs.editPermaName(), 1, "rightLabel");
            _pname = MsoyUI.createTextBox("", MemberName.MAXIMUM_PERMANAME_LENGTH, -1);
            table.setWidget(0, 1, _pname);
            _pname.addKeyboardListener(new DeferredKeyAdapter() {
                public void execute () {
                    validatePermaName();
                }
            });
            _uppname = new Button(_cmsgs.set(), new ClickListener() {
                public void onClick (Widget widget) {
                    configurePermaName();
                }
            });
            _uppname.setEnabled(false);
            table.setWidget(0, 2, _uppname);
            table.setHTML(1, 0, _msgs.editPermaNameTip(), 3, "Tip");

        } else {
            table.setText(0, 0, _msgs.editPermaName(), 1, "rightLabel");
            table.setText(0, 1, CShell.creds.permaName, 1, "PermaName");
        }
        add(new TongueBox(null, _perma = table));

        // configure email address interface
        table = new SmartTable(0, 10);
        table.setText(0, 0, _msgs.editEmail(), 1, "rightLabel");
        table.setWidget(0, 1, _email = MsoyUI.createTextBox("", MemberName.MAX_EMAIL_LENGTH, -1));
        _email.setText(CShell.creds.accountName);
        _email.addKeyboardListener(new DeferredKeyAdapter() {
            public void execute () {
                validateEmail();
            }
        });
        _upemail = new Button(_cmsgs.update(), new ClickListener() {
            public void onClick (Widget widget) {
                updateEmail();
            }
        });
        _upemail.setEnabled(false);
        table.setWidget(0, 2, _upemail);
        add(new TongueBox(_msgs.editEmailHeader(), table));

        // configure email preferences interface
        table = new SmartTable(0, 10);
        table.setText(0, 0, _msgs.editWhirledMailEmail(), 1, "rightLabel");
        _whirledEmail = new CheckBox(_msgs.editWhirledMailEmailTip());
        table.setWidget(0, 1, _whirledEmail, 2, null);
        _whirledEmail.addStyleName("tipLabel");
        _whirledEmail.setChecked(_accountInfo.emailWhirledMail);

        table.setText(1, 0, _msgs.editAnnounceEmail(), 1, "rightLabel");
        table.setWidget(1, 1, _announceEmail = new CheckBox(_msgs.editAnnounceEmailTip()), 2, null);
        _announceEmail.addStyleName("tipLabel");
        _announceEmail.setChecked(_accountInfo.emailAnnouncements);

        _upeprefs = new Button(_cmsgs.update(), new ClickListener() {
            public void onClick (Widget widget) {
                updateEmailPrefs();
            }
        });
        table.setWidget(1, 2, _upeprefs);
        add(new TongueBox(_msgs.editEPrefsHeader(), table));

        // configure real name interface
        table = new SmartTable(0, 10);
        table.setText(0, 0, _msgs.editRealName(), 1, "rightLabel");
        table.setWidget(0, 1, _rname = MsoyUI.createTextBox("", MemberName.MAX_REALNAME_LENGTH, -1));
        _rname.setText(_accountInfo.realName);
        _rname.addKeyboardListener(new DeferredKeyAdapter() {
            public void execute () {
                validateRealName();
            }
        });
        _uprname = new Button(_cmsgs.update(), new ClickListener() {
            public void onClick (Widget widget) {
                updateRealName();
            }
        });
        _uprname.setEnabled(false);
        table.setWidget(0, 2, _uprname);
        table.setHTML(1, 0, _msgs.editRealNameTip(), 3, "Tip");
        add(new TongueBox(_msgs.editRealNameHeader(), table));

        // configure password interface
        table = new SmartTable(0, 10);
        table.setText(0, 0, _msgs.editPassword(), 1, "rightLabel");
        table.setWidget(0, 1, _password = new PasswordTextBox());
        _password.addKeyboardListener(new DeferredKeyAdapter() {
            public void execute () {
                validatePassword();
            }
        });
        _password.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _confirm.setFocus(true);
            }
        }));

        table.setText(1, 0, _msgs.editConfirm(), 1, "rightLabel");
        table.setWidget(1, 1, _confirm = new PasswordTextBox());
        _uppass = new Button(_cmsgs.update(), new ClickListener() {
            public void onClick (Widget widget) {
                updatePassword();
            }
        });
        _uppass.setEnabled(false);
        table.setWidget(1, 2, _uppass);
        add(new TongueBox(_msgs.editPasswordHeader(), table));
        
        // Select charity interface
        if (DeploymentConfig.devDeployment) {
            final SmartTable charityTable = new SmartTable(0, 10);
            charityTable.setText(0, 0, _msgs.charities(), 3, "Tip");
            /*List<RadioButton> charityButtons = new ArrayList<RadioButton>(accountInfo.charityNames.size());
            RadioButton
            for (MemberName name : accountInfo.charityNames) {
                RadioButton rb = new RadioButton("charitiesGroup");
                charityButtons.add(rb);
                int row = charityTable.addWidget(rb, 1, null);
                charityTable.setText(row, 1, name.getNormal());
                charityTable.setText(++row, 1, accountInfo.charities.get(name.getMemberId()).description, 1, "Tip");
            }*/
            
            charityTable.setText(1, 0, _msgs.selectCharity(), 1, "rightLabel");
            _lstCharities = new ListBox();
            _lstCharities.addItem(_msgs.defaultCharity(), "0");
            _lstCharities.setStylePrimaryName("charityList");
            int i = 1;
            for (MemberName name : accountInfo.charityNames) {
                _lstCharities.addItem(name.getNormal(), Integer.toString(name.getMemberId()));
                if (name.getMemberId() == accountInfo.charityMemberId) {
                    _lstCharities.setSelectedIndex(i);
                }
                i++;
            }
            _lstCharities.addChangeListener(new ChangeListener() {
                public void onChange (Widget sender) {
                    if (_lstCharities.getSelectedIndex() > 0) {
                        int memberId = accountInfo.charityNames
                            .get(_lstCharities.getSelectedIndex() - 1).getMemberId();
                        charityTable.setText(2, 0, accountInfo.charities.get(memberId).description, 3, "Tip");
                    } else {
                        charityTable.setText(2, 0, "", 3, "Tip");
                    }
                }
            });
            charityTable.setWidget(1, 1, _lstCharities);
            charityTable.setWidget(1, 2, _upcharity = new Button(_cmsgs.update(), new ClickListener() {
                public void onClick (Widget sender) {
                    updateCharity();
                }
            }));
            add(new TongueBox(_msgs.charitiesHeader(), charityTable));
        }
    }

    protected void updateRealName ()
    {
        final String oldRealName = _accountInfo.realName;
        _accountInfo.realName = _rname.getText().trim();
        _uprname.setEnabled(false);
        _rname.setEnabled(false);
        _usersvc.updateAccountInfo(_accountInfo, new AsyncCallback<Void>() {
            public void onSuccess (Void result) {
                _rname.setEnabled(true);
                _uprname.setEnabled(false);
                MsoyUI.infoNear(_msgs.realNameUpdated(), _uprname);
            }
            public void onFailure (Throwable cause) {
                _rname.setText(_accountInfo.realName = oldRealName);
                _rname.setEnabled(true);
                _uprname.setEnabled(true);
                MsoyUI.errorNear(CShell.serverError(cause), _uprname);
            }
        });
    }

    protected void updateEmail ()
    {
        final String email = _email.getText().trim();
        _upemail.setEnabled(false);
        _usersvc.updateEmail(email, new AsyncCallback<Void>() {
            public void onSuccess (Void result) {
                CShell.creds.accountName = email;
                MsoyUI.infoNear(_msgs.emailUpdated(), _upemail);
            }
            public void onFailure (Throwable cause) {
                _upemail.setEnabled(true);
                MsoyUI.errorNear(CShell.serverError(cause), _upemail);
            }
        });
    }

    protected void updateEmailPrefs ()
    {
        _upeprefs.setEnabled(false);
        _usersvc.updateEmailPrefs(
            _whirledEmail.isChecked(), _announceEmail.isChecked(), new AsyncCallback<Void>() {
            public void onSuccess (Void result) {
                _upeprefs.setEnabled(true);
                MsoyUI.infoNear(_msgs.eprefsUpdated(), _upeprefs);
            }
            public void onFailure (Throwable cause) {
                _upeprefs.setEnabled(true);
                MsoyUI.errorNear(CShell.serverError(cause), _upeprefs);
            }
        });
    }
    
    protected void updateCharity ()
    {
        _upcharity.setEnabled(false);
        _usersvc.updateCharity(Integer.parseInt(_lstCharities.getValue(
            _lstCharities.getSelectedIndex())), new AsyncCallback<Void>() {
                public void onFailure (Throwable caught) {
                    _upcharity.setEnabled(true);
                    MsoyUI.errorNear(CShell.serverError(caught), _upcharity);
                }
                public void onSuccess (Void result) {
                    _upcharity.setEnabled(true);
                    MsoyUI.infoNear(_msgs.echarityUpdated(), _upcharity);
                }
        });
    }

    protected void updatePassword ()
    {
        String password = _password.getText().trim(), confirm = _confirm.getText().trim();
        if (confirm.length() == 0) {
            MsoyUI.errorNear(_msgs.editMissingConfirm(), _uppass);
            return;
        } else if (!password.equals(confirm)) {
            MsoyUI.errorNear(_msgs.editPasswordMismatch(), _uppass);
            return;
        }

        _uppass.setEnabled(false);
        _password.setEnabled(false);
        _confirm.setEnabled(false);
        _usersvc.updatePassword(CShell.frame.md5hex(password), new AsyncCallback<Void>() {
            public void onSuccess (Void result) {
                _password.setText("");
                _password.setEnabled(true);
                _confirm.setText("");
                _confirm.setEnabled(true);
                MsoyUI.infoNear(_msgs.passwordUpdated(), _uppass);
            }
            public void onFailure (Throwable cause) {
                _password.setEnabled(true);
                _confirm.setEnabled(true);
                _uppass.setEnabled(true);
                MsoyUI.errorNear(CShell.serverError(cause), _uppass);
            }
        });
    }

    protected void configurePermaName ()
    {
        final String pname = _pname.getText().trim();

        // now check it for legality
        for (int ii = 0; ii < pname.length(); ii++) {
            char c = pname.charAt(ii);
            if ((ii == 0 && !Character.isLetter(c)) ||
                (!Character.isLetter(c) && !Character.isDigit(c) && c != '_')) {
                MsoyUI.errorNear(_msgs.editPermaInvalid(), _uppname);
                return;
            }
        }
        if (pname.length() < MemberName.MINIMUM_PERMANAME_LENGTH) {
            MsoyUI.errorNear(_msgs.editPermaShort(), _uppname);
        } else if (pname.length() > MemberName.MAXIMUM_PERMANAME_LENGTH) {
            MsoyUI.errorNear(_msgs.editPermaLong(), _uppname);
        }

        _uppname.setEnabled(false);
        _pname.setEnabled(false);
        _usersvc.configurePermaName(pname, new AsyncCallback<Void>() {
            public void onSuccess (Void result) {
                CShell.creds.permaName = pname;
                _perma.getFlexCellFormatter().setStyleName(0, 1, "PermaName");
                _perma.setText(0, 1, pname);
                _perma.setText(0, 2, "");
                _perma.setText(0+1, 0, "");
                MsoyUI.infoNear(_msgs.permaNameConfigured(), _uppname);
            }
            public void onFailure (Throwable cause) {
                _pname.setEnabled(true);
                _uppname.setEnabled(true);
                MsoyUI.errorNear(CShell.serverError(cause), _uppname);
            }
        });
    }

    protected void validateRealName ()
    {
        String realName = _rname.getText().trim();
        _uprname.setEnabled(!_accountInfo.realName.equals(realName));
    }

    protected void validateEmail ()
    {
        String email = _email.getText().trim();
        _upemail.setEnabled(!(email.length() < 4 || email.indexOf("@") == -1 ||
                              email.equals(CShell.creds.accountName)));
    }

    protected void validatePassword ()
    {
        String password = _password.getText().trim();
        _uppass.setEnabled(password.length() > 0);
    }

    protected void validatePermaName ()
    {
        // extract the permaname, but also show the user exactly what we're using, since we
        // lowercase and trim it
        int cursor = _pname.getCursorPos();
        String raw = _pname.getText();
        String pname = raw.trim();
        cursor -= raw.indexOf(pname);
        pname = pname.toLowerCase();
        _pname.setText(pname);
        _pname.setCursorPos(cursor);
        _uppname.setEnabled(pname.length() > 0);
    }

    protected static abstract class DeferredKeyAdapter
        extends KeyboardListenerAdapter implements Command {
        public void onKeyPress (Widget sender, char keyCode, int modifiers) {
            // let the keypress go through, then validate our data
            DeferredCommand.addCommand(this);
        }
    };

    protected AccountInfo _accountInfo;
    protected SmartTable _perma;

    protected TextBox _email, _pname, _rname;
    protected CheckBox _whirledEmail, _announceEmail;
    protected PasswordTextBox _password, _confirm;
    protected Button _upemail, _upeprefs, _uppass, _uppname, _uprname, _upcharity;
    protected ListBox _lstCharities;
    
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
