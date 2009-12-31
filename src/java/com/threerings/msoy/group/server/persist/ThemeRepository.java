//
// $Id: MedalRepository.java 18045 2009-09-10 03:16:45Z mdb $

package com.threerings.msoy.group.server.persist;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.XList;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.threerings.msoy.group.data.all.GroupMembership.Rank;
import com.threerings.presents.annotation.BlockingThread;

/**
 * Manages the persistent store of themes.
 */
@Singleton @BlockingThread
public class ThemeRepository extends DepotRepository
{
    @Inject public ThemeRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Load the configuration data for a theme.
     */
    public ThemeRecord loadTheme (int groupId)
    {
        return load(ThemeRecord.class, ThemeRecord.getKey(groupId));
    }

    /**
     * Fetch all the themes, ordered by popularity.
     */
    public XList<ThemeRecord> loadThemes ()
    {
        return findAll(ThemeRecord.class, OrderBy.descending(ThemeRecord.POPULARITY));
    }

    /**
     * Load the themes in which the given member has at least the given rank. This warrants
     * a repository function because the quick way to do it is through a join.
     */
    public List<ThemeRecord> getManagedThemes (int memberId, Rank rank)
    {
        return findAll(ThemeRecord.class,
            new Join(ThemeRecord.GROUP_ID, GroupMembershipRecord.GROUP_ID),
            new Where(Ops.and(
                GroupMembershipRecord.RANK.greaterEq(rank),
                GroupMembershipRecord.MEMBER_ID.eq(memberId))));
    }

    /**
     * Create a new Theme aspect to the given group. Returns true if the record was created;
     * false suggests the Theme already existed.
     */
    public boolean createTheme (ThemeRecord trec)
    {
        return store(trec);
    }

    /**
     * Updates the specified theme record with supplied field/value mapping.
     */
    public void updateTheme (int groupId, Map<ColumnExp, Object> updates)
    {
        updatePartial(ThemeRecord.getKey(groupId), updates);
    }

    /**
     * Updates the specified theme record in the database.
     */
    public void updateTheme (ThemeRecord rec)
    {
        update(rec);
    }

    /**
     * Load the most recently used avatar record a given player and theme.
     */
    public ThemeAvatarUseRecord getLastWornAvatar (int memberId, int groupId)
    {
        return load(ThemeAvatarUseRecord.getKey(memberId, groupId));
    }

    /**
     * Update the most recently used avatar for a given player and theme.
     */
    public void noteAvatarWorn (int memberId, int groupId, int itemId)
    {
        store(new ThemeAvatarUseRecord(memberId, groupId, itemId));
    }

    /**
     * Test whether the given avatar is in the given theme's lineup.
     */
    public boolean isAvatarInLineup (int groupId, int catalogId)
    {
        return null != load(ThemeAvatarLineupRecord.getKey(groupId, catalogId));
    }

    /**
     * Fetch the avatar lineup for a given theme, limited to count results.
     */
    public List<ThemeAvatarLineupRecord> loadAvatarLineup (int groupId, int offset, int count)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new Where(ThemeAvatarLineupRecord.GROUP_ID, groupId));
        if (count != -1) {
            clauses.add(new Limit(offset, count));
        }
        return findAll(ThemeAvatarLineupRecord.class, clauses);
    }

    /**
     * Fetch the lineups which contain the given avatar.
     */
    public List<ThemeAvatarLineupRecord> loadLineups (int catalogId)
    {
        return findAll(ThemeAvatarLineupRecord.class,
            new Where(ThemeAvatarLineupRecord.CATALOG_ID, catalogId));
    }


    /**
     * Add or modify an avatar to a lineup. Returns true if the avatar did not previously
     * exist in the lineup.
     */
    public boolean setAvatarInLineup (int groupId, int catalogId)
    {
        return store(new ThemeAvatarLineupRecord(
            groupId, catalogId, ThemeAvatarLineupRecord.GENDER_OTHER));
    }

    /**
     * Remove an avatar from a lineup. Returns true if the avatar existed in the lineup.
     */
    public boolean removeAvatarFromLineup (int groupId, int catalogId)
    {
        return 1 == delete(ThemeAvatarLineupRecord.getKey(groupId, catalogId));
    }

    /**
     * Fetch the home room templates for a given theme.
     */
    public List<ThemeHomeTemplateRecord> loadHomeTemplates (int groupId)
    {
        return findAll(ThemeHomeTemplateRecord.class,
            new Where(ThemeHomeTemplateRecord.GROUP_ID, groupId));
    }

    /**
     * Add or modify a home room template for a theme. Returns true if the template did not
     * previously exist for the theme.
     */
    public boolean setHomeTemplate (int groupId, int sceneId)
    {
        return store(new ThemeHomeTemplateRecord(groupId, sceneId));
    }

    /**
     * Remove a home template from a theme. Returns true if the template existed.
     */
    public boolean removeHomeTemplate (int groupId, int sceneId)
    {
        return 1 == delete(ThemeAvatarLineupRecord.getKey(groupId, sceneId));
    }

    @Override
    protected void getManagedRecords(Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ThemeRecord.class);
        classes.add(ThemeAvatarUseRecord.class);
        classes.add(ThemeAvatarLineupRecord.class);
        classes.add(ThemeHomeTemplateRecord.class);
    }
}
