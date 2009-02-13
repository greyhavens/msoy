//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;

import com.threerings.presents.data.ClientObject;

import com.threerings.crowd.server.CrowdClientResolver;

import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.game.data.GameAuthName;
import com.threerings.msoy.game.data.PlayerObject;

/**
 * Resolves an MSOY Game client's runtime data.
 */
public class GameClientResolver extends CrowdClientResolver
{
    @Override // from PresentsClientResolver
    public ClientObject createClientObject ()
    {
        return new PlayerObject();
    }

    @Override // from PresentsSession
    protected void resolveClientData (ClientObject clobj)
        throws Exception
    {
        super.resolveClientData(clobj);

        GameAuthName authName = (GameAuthName)_username;
        VizMemberName name;
        int humanity;
        VisitorInfo vinfo = null;

        if (authName.isGuest()) {
            // our auth username has our assigned name and member id, so use those
            name = new VizMemberName(
                authName.toString(), authName.getMemberId(), VizMemberName.DEFAULT_PHOTO);

            // guests operate at the default new user humanity level
            humanity = MsoyCodes.STARTING_HUMANITY;

        } else {
            MemberRecord member = _memberRepo.loadMember(authName.getMemberId());
            ProfileRecord precord = _profileRepo.loadProfile(member.memberId);
            MediaDesc photo = (precord == null) ? VizMemberName.DEFAULT_PHOTO : precord.getPhoto();
            name = new VizMemberName(member.name, member.memberId, photo);
            humanity = member.humanity;
            // guests will have a visitor info assigned later, in GameSession.sessionWillStart()
            vinfo = new VisitorInfo(member.visitorId, true);
        }

        // NOTE: we avoid using the dobject setters here because we know the object is not out in
        // the wild and there's no point in generating a crapload of events during user
        // initialization when we know that no one is listening
        PlayerObject plobj = (PlayerObject) clobj;
        plobj.memberName = name;
        plobj.humanity = humanity;
        plobj.visitorInfo = vinfo;
    }

    // our dependencies
    @Inject protected MemberRepository _memberRepo;
    @Inject protected ProfileRepository _profileRepo;
}
