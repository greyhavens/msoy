//
// $Id$

package client.ui;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.ButtonBase;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.CookieUtil;
import com.threerings.gwt.util.DateUtil;
import com.threerings.gwt.util.StringUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberMailUtil;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.AccountInfo;
import com.threerings.msoy.web.gwt.CaptchaException;
import com.threerings.msoy.web.gwt.Invitation;
import com.threerings.msoy.web.gwt.RegisterInfo;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.shell.CShell;
import client.shell.ServerMessages;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.RecaptchaUtil;

/**
 * Displays a standard registration UI. Designed to be embedded in either the landing page or
 * account-create. Requires that the hosting page have the following JavaScript in index.html:
 *
 * <script id="recaptcha" type="text/javascript"></script>
 */
public class RegisterPanel extends FlowPanel
{
    public RegisterPanel ()
    {
        if (CookieUtil.get(NOT_THIRTEEN_COOKIE) != null) {
            blockKiddies();
            return;
        }

        // Use HTML5 accessibility hints
        // TODO: someday GWT may support this directly
        _newemail.getElement().setAttribute("type", "email");
        // _newemail.getElement().setAttribute("placeholder", "example@domain.com");

        setStyleName("register");
        setStepOne();

        // if we have an active invitation, use the invite email as our default
        Invitation invite = CShell.frame.getActiveInvitation();
        if (invite != null && invite.inviteeEmail.matches(MsoyUI.EMAIL_REGEX)) {
            _newemail.setText(invite.inviteeEmail);
        }
    }

    protected void setStepOne ()
    {
        clear();
        addHeader(false);

        SmartTable regi = new SmartTable(0, 5);
        regi.setText(0, 0, _cmsgs.regiRegEmail(), 1, "Right");
        regi.setWidget(0, 1, _newemail);
        regi.setText(1, 0, _cmsgs.regiRegPass(), 1, "Right");
        regi.setWidget(1, 1, _newpass);
        String ageTitle = _cmsgs.regiRegAge();
        _birthday = new AgeDropdown();
        regi.setText(2, 0, ageTitle, 1, "Right");
        regi.setWidget(2, 1, _birthday);
        ButtonBase doCreate = MsoyUI.createButton(MsoyUI.SHORT_THIN, _cmsgs.regiRegGo(), null);
        regi.setWidget(3, 1, doCreate);
        regi.getFlexCellFormatter().setHorizontalAlignment(3, 1, HasAlignment.ALIGN_RIGHT);
        add(regi);
        for (int row = 0; row < regi.getRowCount(); row++) {
            regi.getFlexCellFormatter().setVerticalAlignment(row, 0, HasAlignment.ALIGN_MIDDLE);
        }

        doCreate.addClickHandler(new ClickHandler() {
            public void onClick (ClickEvent event) {
                if (StringUtil.isBlank(_newemail.getText().trim())) {
                    MsoyUI.errorNear(_cmsgs.regiFillAll(), _newemail);
                } else if (StringUtil.isBlank(_newpass.getText().trim())) {
                    MsoyUI.errorNear(_cmsgs.regiFillAll(), _newpass);
                } else if (!isThirteen()) {
                    CookieUtil.set("/", 9001, NOT_THIRTEEN_COOKIE, "1");
                    blockKiddies();
                } else if (!MemberMailUtil.isValidAddress(_newemail.getValue())) {
                    MsoyUI.errorNear(_smsgs.invalid_email(), _newemail);
                } else {
                    setStepTwo();
                }
            }
        });
    }

    protected void setStepTwo ()
    {
        clear();
        addHeader(false);

        SmartTable regi = new SmartTable(0, 5);

        regi.setText(0, 0, _cmsgs.regiRegSecurity(), 2);
        if (RecaptchaUtil.isEnabled()) {
            regi.setWidget(1, 0, RecaptchaUtil.createDiv("recaptchaDiv"), 2);
            DeferredCommand.addCommand(new Command() { // delay init until div is added to DOM
                public void execute () {
                    RecaptchaUtil.init("recaptchaDiv");
                }
            });
        } else {
            regi.setText(1, 0, "CAPTCHA not enabled.", 2, "NoCaptcha");
        }
        final CheckBox tosBox = new CheckBox(_cmsgs.regiRegTOS(), true);
        regi.setWidget(2, 0, tosBox, 2);

        regi.setWidget(3, 0, MsoyUI.createActionLabel(_cmsgs.regiRegBack(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                setStepOne();
            }
        }));

        ButtonBase doCreate = MsoyUI.createButton(MsoyUI.SHORT_THIN, _cmsgs.regiRegGo(), null);
        regi.setWidget(3, 1, doCreate);
        regi.getFlexCellFormatter().setHorizontalAlignment(3, 1, HasAlignment.ALIGN_RIGHT);
        add(regi);

        // create our click callback that handles the actual registation process
        new ClickCallback<WebUserService.RegisterData>(doCreate) {
            @Override protected boolean callService () {
                if (!tosBox.getValue()) {
                    MsoyUI.errorNear(_cmsgs.regiMustTOS(), tosBox);
                    return false;
                }
                _usersvc.register(DeploymentConfig.version, createRegInfo(), true, this);
                return true;
            }

            @Override protected boolean gotResult (final WebUserService.RegisterData session) {
                setComplete(session);
                return false;
            }

            @Override protected void reportFailure (Throwable cause) {
                if (RecaptchaUtil.isEnabled()) {
                    RecaptchaUtil.reload();
                    if (cause instanceof CaptchaException) {
                        RecaptchaUtil.focus();
                    }
                }
                super.reportFailure(cause);
            }
        };
    }

    protected void setComplete (WebUserService.RegisterData session)
    {
        if (!DeploymentConfig.devDeployment) {
            add(createAdWordsTracker());
            add(createBeacon(session.entryVector));
        }

        CShell.frame.dispatchDidLogon(session);
    }

    protected void addHeader (boolean complete)
    {
        // nada
    }

    protected RegisterInfo createRegInfo ()
    {
        RegisterInfo info = new RegisterInfo();
        info.email = _newemail.getText().trim();
        info.password = CShell.frame.md5hex(_newpass.getText().trim());
        // info.displayName = _name.getText().trim();
        info.displayName = "???";
        info.birthday = getBirthday();
        info.info = new AccountInfo();
        // info.info.realName = _rname.getText().trim();
        info.info.realName = "";
        info.expireDays = 1; // TODO: unmagick?
        Invitation invite = CShell.frame.getActiveInvitation();
        info.inviteId = (invite == null) ? null : invite.inviteId;
        info.permaguestId = CShell.isPermaguest() ? CShell.getMemberId() : 0;
        info.visitor = CShell.frame.getVisitorInfo();
        info.captchaChallenge = RecaptchaUtil.isEnabled() ? RecaptchaUtil.getChallenge() : null;
        info.captchaResponse = RecaptchaUtil.isEnabled() ? RecaptchaUtil.getResponse() : null;
        return info;
    }

    protected int[] getBirthday ()
    {
        return ((AgeDropdown)_birthday).getDate();
    }

    protected boolean isThirteen ()
    {
        return ((AgeDropdown)_birthday).getAge() >= 13;
    }

    protected void blockKiddies ()
    {
        clear();
        add(MsoyUI.createLabel(_cmsgs.regiNotThirteen(), "errorPopup"));
    }

    protected static Widget createAdWordsTracker ()
    {
        Frame tracker = new Frame("./googleconversion.html");
        tracker.setStyleName("AdWordsFrame");
        return tracker;
    }

    protected static Widget createBeacon (String vector)
    {
        for (String[] entry : BEACONS) {
            final String vecpat = entry[0], url = entry[1];
            if (vector != null && vector.startsWith(vecpat)) {
                return MsoyUI.createImage(url, "Beacon");
            }
        }
        return null;
    }

    /**
     * Dropdown of ages which produces a date in the same format as the DateFields class.
     */
    protected static class AgeDropdown extends FlowPanel
    {
        public AgeDropdown () {
            setStyleName("ageDropdown");
            int minAge = 4;
            for (int ii = 0; ii < 100; ii++) {
                _agelist.addItem("" + (minAge + ii));
            }
            _agelist.setSelectedIndex(13-minAge); // Start with 13 selected
            add(_agelist);
        }

        /**
         * Return January 1st in the estimated year that the person was born. Subtract their age
         * from the current year, minus one so we are rounding older rather than younger.
         */
        public int[] getDate () {
            int currentYear = DateUtil.getYear(new Date());
            int year = currentYear - getAge() - 1;
            return new int[] { year, 0, 1 };
        }
        public int getAge () {
            return Integer.parseInt(_agelist.getItemText(_agelist.getSelectedIndex()));
        }
        protected ListBox _agelist = new ListBox();
    }

    protected TextBox _newemail = MsoyUI.createTextBox("", MemberName.MAX_EMAIL_LENGTH, -1);
    protected PasswordTextBox _newpass = new PasswordTextBox();
    protected Widget _birthday;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final ServerMessages _smsgs = GWT.create(ServerMessages.class);
    protected static final WebUserServiceAsync _usersvc = GWT.create(WebUserService.class);

    // post-registration beacon URLs: yay for partners!
    protected static final String[][] BEACONS = new String[][] {
        { "a.shizmoo", "http://server.cpmstar.com/action.aspx?advertiserid=275&gif=1" }
    };

    protected static final String NOT_THIRTEEN_COOKIE = "NotThirteen";
}
