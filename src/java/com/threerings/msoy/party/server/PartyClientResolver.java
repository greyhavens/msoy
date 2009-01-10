//
// $Id$

package com.threerings.msoy.party.server;

import com.google.inject.Inject;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.ClientResolver;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;

import com.threerings.msoy.party.data.PartierObject;

/**
 * Handles the resolution of partier client information.
 */
public class PartyClientResolver extends ClientResolver
{
    @Override // from PresentsClientResolver
    public ClientObject createClientObject ()
    {
        return new PartierObject();
    }

    @Override // from PresentsSession
    protected void resolveClientData (ClientObject clobj)
        throws Exception
    {
        super.resolveClientData(clobj);

        PartierObject partObj = (PartierObject) clobj;

        // load up their member information using on their authentication (account) name
        MemberRecord member = _memberRepo.loadMember(_username.toString());

        // we need their profile photo as well
        ProfileRecord precord = _profileRepo.loadProfile(member.memberId);
        MediaDesc photo = (precord == null) ? VizMemberName.DEFAULT_PHOTO : precord.getPhoto();

        // NOTE: we avoid using the dobject setters here because we know the object is not out in
        // the wild and there's no point in generating a crapload of events during user
        // initialization when we know that no one is listening

        partObj.memberName = new VizMemberName(member.name, member.memberId, photo);
    }

    @Inject protected MemberRepository _memberRepo;
    @Inject protected ProfileRepository _profileRepo;
}
