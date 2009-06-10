//
// $Id$

package client.util;

import java.util.Date;

import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.web.gwt.AccountInfo;
import com.threerings.msoy.web.gwt.DateUtil;
import com.threerings.msoy.web.gwt.Invitation;
import com.threerings.msoy.web.gwt.RegisterInfo;

import client.shell.CShell;
import client.ui.DateFields;
import client.ui.MsoyUI;

/**
 * Utility methods relating to user registration. Shared by the account-create page and the landing
 * page registration UI.
 */
public class RegisterUtil
{
    public static void initRegiUI (TextBox email)
    {
        Invitation invite = CShell.frame.getActiveInvitation();
        if (invite != null && invite.inviteeEmail.matches(MsoyUI.EMAIL_REGEX)) {
            // provide the invitation email as the default
            email.setText(invite.inviteeEmail);
        }
    }

    public static boolean checkIsThirteen (DateFields birthday)
    {
        String[] today = new Date().toString().split(" ");
        String thirteenYearsAgo = "";
        for (int ii = 0; ii < today.length; ii++) {
            if (today[ii].matches("[0-9]{4}")) {
                int year = Integer.valueOf(today[ii]).intValue();
                today[ii] = "" + (year - 13);
            }
            thirteenYearsAgo += today[ii] + " ";
        }
        return DateUtil.newDate(thirteenYearsAgo).compareTo(
            DateUtil.toDate(birthday.getDate())) >= 0;
    }

    public static RegisterInfo createRegInfo (TextBox email, TextBox password, DateFields birthday)
    {
        RegisterInfo info = new RegisterInfo();
        info.email = email.getText().trim();
        info.password = CShell.frame.md5hex(password.getText().trim());
        // info.displayName = _name.getText().trim();
        info.displayName = "???";
        info.birthday = birthday.getDate();
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
}
