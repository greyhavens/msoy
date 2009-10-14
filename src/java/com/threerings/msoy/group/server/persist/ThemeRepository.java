//
// $Id: MedalRepository.java 18045 2009-09-10 03:16:45Z mdb $

package com.threerings.msoy.group.server.persist;

import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Where;
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
     * Create a new Theme aspect to the given group. Returns true if the record was created;
     * false suggests the Theme already existed.
     */
    public boolean createTheme (int groupId)
    {
        return store(new ThemeRecord(groupId));
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
     * Fetch the avatar lineup for a given theme.
     */
    public List<ThemeAvatarLineupRecord> loadAvatarLineup (int groupId)
    {
        return findAll(ThemeAvatarLineupRecord.class,
            new Where(ThemeAvatarLineupRecord.GROUP_ID, groupId));
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
