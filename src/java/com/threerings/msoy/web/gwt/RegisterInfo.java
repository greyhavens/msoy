//
// $Id$

package com.threerings.msoy.web.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.VisitorInfo;

/**
 * Contains all the information needed from the client when registering.
 */
public class RegisterInfo
    implements IsSerializable
{
    public String email;
    public String password;
    public String displayName;
    public int[] birthday;
    public MediaDesc photo;
    public AccountInfo info;

    public int expireDays;

    public String inviteId;
    public int guestId;
    public VisitorInfo visitor;

    public String captchaChallenge;
    public String captchaResponse;
}
