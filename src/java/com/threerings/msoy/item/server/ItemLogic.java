//
// $Id$

package com.threerings.msoy.item.server;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntSet;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.data.ListingCard;

/**
 * Contains item related services that are used by servlets and other blocking thread code.
 */
@BlockingThread @Singleton
public class ItemLogic
{
    /**
     * Resolves the member names in the supplied list of listing cards.
     */
    public void resolveCardNames (List<ListingCard> list)
        throws PersistenceException
    {
        // determine which member names we need to look up
        IntSet members = new ArrayIntSet();
        for (ListingCard card : list) {
            members.add(card.creator.getMemberId());
        }
        // now look up the names and build a map of memberId -> MemberName
        IntMap<MemberName> map = IntMaps.newHashIntMap();
        for (MemberName record: _memberRepo.loadMemberNames(members)) {
            map.put(record.getMemberId(), record);
        }
        // finally fill in the listings using the map
        for (ListingCard card : list) {
            card.creator = map.get(card.creator.getMemberId());
        }
    }

    @Inject MemberRepository _memberRepo;
}
