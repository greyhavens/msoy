//
// $Id$

package com.threerings.msoy.group.server.persist;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.operator.Conditionals.Equals;
import com.samskivert.depot.operator.Conditionals.In;
import com.samskivert.depot.operator.Logic.And;

import com.threerings.presents.annotation.BlockingThread;

/**
 * Manages the persistent store of Medal data.
 */
@Singleton @BlockingThread
public class MedalRepository extends DepotRepository
{
    @Inject public MedalRepository (PersistenceContext ctx)
    {
        super(ctx);

        // New maximum values were imposed on these fields on 2008.12.11
        ctx.registerMigration(MedalRecord.class, new SchemaMigration.Retype(3, "name"));
        ctx.registerMigration(MedalRecord.class, new SchemaMigration.Retype(3, "description"));
    }

    /**
     * If the medal's medalId is valid, it will update that medalId's row in the database.
     * Otherwise, it will insert a new row.
     */
    public boolean storeMedal (MedalRecord medal)
    {
        return store(medal);
    }

    /**
     * Returns the required MedalRecord
     */
    public MedalRecord loadMedal (int medalId)
    {
        return load(MedalRecord.class, medalId);
    }

    public List<MedalRecord> loadMedals (Collection<Integer> medalIds)
    {
        if (medalIds.size() == 0) {
            return Collections.emptyList();
        }
        return loadAll(MedalRecord.class, medalIds);
    }

    /**
     * Returns true if the groupId and name combination is already in use.
     */
    public boolean groupContainsMedalName (int groupId, String name)
    {
        Equals groupIdEquals = new Equals(MedalRecord.GROUP_ID_C, groupId);
        Equals nameEquals = new Equals(MedalRecord.NAME_C, name);
        return load(MedalRecord.class, new Where(new And(groupIdEquals, nameEquals))) != null;
    }

    /**
     * Returns a list of MedalRecords for the medals that belong to the given group.
     */
    public List<MedalRecord> loadGroupMedals (int groupId)
    {
        return findAll(MedalRecord.class, new Where(MedalRecord.GROUP_ID_C, groupId));
    }

    public EarnedMedalRecord loadEarnedMedal (int memberId, int medalId)
    {
        return load(EarnedMedalRecord.class, EarnedMedalRecord.getKey(medalId, memberId));
    }

    /**
     * Returns a list of the EarnedMedalRecords that have been earned by the given individual.
     */
    public List<EarnedMedalRecord> loadEarnedMedals (int memberId)
    {
        return findAll(EarnedMedalRecord.class, new Where(EarnedMedalRecord.MEMBER_ID_C, memberId));
    }

    /**
     * Returns a list of the recently earned EarnedMedalRecords.
     */
    public List<EarnedMedalRecord> loadRecentEarnedMedals (int memberId, int limit)
    {
        return findAll(EarnedMedalRecord.class, new Where(EarnedMedalRecord.MEMBER_ID_C, memberId),
            new Limit(0, limit), OrderBy.descending(EarnedMedalRecord.WHEN_EARNED_C));
    }

    /**
     * Returns a list of the EarnedMedalRecords that match the given set of medalIds.
     */
    public List<EarnedMedalRecord> loadEarnedMedals (Collection<Integer> medalIds)
    {
        if (medalIds.size() == 0) {
            return Collections.emptyList();
        }
        return findAll(EarnedMedalRecord.class,
            new Where(new In(EarnedMedalRecord.MEDAL_ID_C, medalIds)));
    }

    /**
     * Awards the given medal to the given member.
     *
     * @throws DuplicateKeyException If this member has already earned that medal.
     */
    public void awardMedal (int memberId, int medalId)
    {
        EarnedMedalRecord earnedMedalRec = new EarnedMedalRecord();
        earnedMedalRec.memberId = memberId;
        earnedMedalRec.medalId = medalId;
        earnedMedalRec.whenEarned = new Timestamp(System.currentTimeMillis());
        insert(earnedMedalRec);
    }

    public boolean deleteEarnedMedal (int memberId, int medalId)
    {
        int result = delete(EarnedMedalRecord.class, EarnedMedalRecord.getKey(medalId, memberId));
        return result > 0;
    }

    @Override
    protected void getManagedRecords(Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(MedalRecord.class);
        classes.add(EarnedMedalRecord.class);
    }
}
