//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.ClientLocal;

import com.threerings.crowd.server.CrowdClientResolver;

import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.server.MsoyUserLocal;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.game.data.GameAuthName;
import com.threerings.msoy.game.data.PlayerObject;

import com.threerings.msoy.party.data.PartySummary;


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

    @Override
    public ClientLocal createLocalAttribute ()
    {
        return new MsoyUserLocal();
    }

    @Override // from PresentsSession
    protected void resolveClientData (ClientObject clobj)
        throws Exception
    {
        super.resolveClientData(clobj);

        GameAuthName authName = (GameAuthName)_username;
        MemberRecord member = _memberRepo.loadMember(authName.getMemberId());
        ProfileRecord precord = _profileRepo.loadProfile(member.memberId);
        MediaDesc photo = (precord == null) ? VizMemberName.DEFAULT_PHOTO : precord.getPhoto();

        // NOTE: we avoid using the dobject setters here because we know the object is not out in
        // the wild and there's no point in generating a crapload of events during user
        // initialization when we know that no one is listening
        PlayerObject plobj = (PlayerObject) clobj;
        plobj.memberName = new VizMemberName(member.name, member.memberId, photo);
        plobj.humanity = member.humanity;
        plobj.visitorInfo = new VisitorInfo(member.visitorId, true);
    }

    @Override
    protected void finishResolution (ClientObject clobj)
    {
        super.finishResolution(clobj);

        PlayerObject plobj = (PlayerObject) clobj;
        // try filling in their party info, if applicable
        PartySummary party = _peerMan.getPartySummary(plobj.getMemberId());
        if (party != null) {
            plobj.partyId = party.id;
            plobj.getLocal(MsoyUserLocal.class).party = party;
        }
    }

    // our dependencies
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected ProfileRepository _profileRepo;
}
