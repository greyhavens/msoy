//
// $Id$

package com.threerings.msoy.facebook.server.persist;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.threerings.msoy.facebook.gwt.FacebookTemplate;
import com.threerings.msoy.server.util.DropPrimaryKey;

/**
 * Manages persistent structures for integrating with Facebook.
 */
@Singleton
public class FacebookRepository extends DepotRepository
{
    /**
     * Creates a new repository.
     */
    @Inject
    public FacebookRepository (PersistenceContext context)
    {
        super(context);

        // workaround to add the new @Id column properly
        context.registerMigration(FacebookInfoRecord.class, new DropPrimaryKey(5));

        // workaround to add the new @Id column properly
        context.registerMigration(FacebookTemplateRecord.class, new DropPrimaryKey(3));

        // workaround to add the new @Id column properly
        context.registerMigration(FacebookActionRecord.class, new DropPrimaryKey(2));

        registerMigration(new DataMigration("2009-09 FacebookActionRecord appId") {
            @Override public void invoke ()
                throws DatabaseException {
                // change all app id fields from the old default to the new
                final int OLD_DEFAULT = 0;
                final int NEW_DEFAULT = 1;
                updatePartial(FacebookActionRecord.class, new Where(
                    FacebookActionRecord.APP_ID.eq(OLD_DEFAULT)),
                    null, FacebookActionRecord.APP_ID, NEW_DEFAULT);
            }
        });

        // set the default values for the new stream publishing columns - we don't want these to
        // be nullable or have permanent default values
        context.registerMigration(FacebookTemplateRecord.class,
            new SchemaMigration.Add(4, new ColumnExp<String>(
                FacebookTemplateRecord.class, "caption"), "''"));
        context.registerMigration(FacebookTemplateRecord.class,
            new SchemaMigration.Add(4, FacebookTemplateRecord.DESCRIPTION, "''"));
        context.registerMigration(FacebookTemplateRecord.class,
            new SchemaMigration.Add(4, FacebookTemplateRecord.PROMPT, "''"));
        context.registerMigration(FacebookTemplateRecord.class,
            new SchemaMigration.Add(4, FacebookTemplateRecord.LINK_TEXT, "''"));
        context.registerMigration(FacebookTemplateRecord.class,
            new SchemaMigration.Add(4, FacebookTemplateRecord.ENABLED, "'t'"));

        // old captions become the gender neutral caption
        context.registerMigration(FacebookTemplateRecord.class,
            new SchemaMigration.Rename(5, "caption", FacebookTemplateRecord.CAPTION_NEUTRAL));
    }

    /**
     * Loads the Facebook info for the specified game. If no info is registered for the game in
     * question a blank record is created with gameId filled in but no key or secret.
     */
    public FacebookInfoRecord loadGameFacebookInfo (int gameId)
    {
        return loadFacebookInfo(gameId, 0);
    }

    /**
     * Loads the Facebook info for the specified application. If no info is registered for the game
     * in question a blank record is created with gameId filled in but no key or secret.
     */
    public FacebookInfoRecord loadAppFacebookInfo (int appId)
    {
        return loadFacebookInfo(0, appId);
    }

    /**
     * Creates or updates the Facebook info for the game referenced by the supplied record.
     */
    public void updateFacebookInfo (FacebookInfoRecord info)
    {
        Preconditions.checkArgument(info.appId == 0 ^ info.gameId == 0);
        store(info);
    }

    /**
     * Deletes the facebook info for the given game id.
     */
    public void deleteGameFacebookInfo (int gameId)
    {
        delete(FacebookInfoRecord.getKey(gameId, 0));
    }

    /**
     * Deletes the facebook info for the given application id.
     */
    public void deleteAppFacebookInfo (int appId)
    {
        delete(FacebookInfoRecord.getKey(0, appId));
    }

    /**
     * Adds or updates the given template.
     */
    public void storeTemplate (FacebookTemplateRecord template)
    {
        store(template);
    }

    /**
     * Sets or clears the enabled flag of a template.
     */
    public void updateTemplateEnabling (int appId, FacebookTemplate.Key key, Boolean enabled)
    {
        updatePartial(FacebookTemplateRecord.getKey(appId, key.code, key.variant),
            FacebookTemplateRecord.ENABLED, enabled);
    }

    /**
     * Deletes the template record with the given code.
     */
    public void deleteTemplate (int appId, FacebookTemplate.Key key)
    {
        delete(FacebookTemplateRecord.getKey(appId, key.code, key.variant));
    }

    /**
     * Loads a list of all saved templates.
     */
    public List<FacebookTemplateRecord> loadTemplates (int appId)
    {
        return from(FacebookTemplateRecord.class).where(FacebookTemplateRecord.APP_ID.eq(appId)).
            noCache().select();
    }

    /**
     * Loads a list of all templates for the given code.
     */
    public List<FacebookTemplateRecord> loadVariants (int appId, String code)
    {
        return from(FacebookTemplateRecord.class).where(
            FacebookTemplateRecord.CODE.eq(code), FacebookTemplateRecord.APP_ID.eq(appId),
            FacebookTemplateRecord.ENABLED.eq(Boolean.TRUE)).select();
    }

    /**
     * Records an action.
     */
    public void recordAction (FacebookActionRecord action)
    {
        store(action);
    }

    /**
     * Gets the most recent record of the given type, or null if there are none.
     */
    public FacebookActionRecord getLastAction (
        int appId, int memberId, FacebookActionRecord.Type type)
    {
        List<FacebookActionRecord> visits = from(FacebookActionRecord.class).where(
            FacebookActionRecord.TYPE.eq(type), FacebookActionRecord.MEMBER_ID.eq(memberId)).
            limit(1).descending(FacebookActionRecord.TIMESTAMP).select();
        return visits.size() > 0 ? visits.get(0) : null;
    }

    /**
     * Loads all Facebook actions related to the member of the given id.
     */
    public List<FacebookActionRecord> loadActions (int appId, int memberId)
    {
        return from(FacebookActionRecord.class).where(
            FacebookActionRecord.MEMBER_ID, memberId).select();
    }

    /**
     * Loads all actions related to a set of members and of the given type.
     */
    public List<FacebookActionRecord> loadActions (
        int appId, Collection<Integer> memberIds, FacebookActionRecord.Type type)
    {
        return from(FacebookActionRecord.class).where(
            FacebookActionRecord.TYPE.eq(type),
            FacebookActionRecord.MEMBER_ID.in(memberIds)).select();
    }

    /**
     * Removes old facbook action records.
     */
    public void pruneActions ()
    {
        // TODO: implement and schedule with cron logic
    }

    /**
     * Loads all thumbnails assigned to the given game.
     */
    public List<FeedThumbnailRecord> loadGameThumbnails (int gameId)
    {
        return loadThumbnails(null, gameId, 0);
    }

    /**
     * Loads all thumbnails assigned to the given game with the given code.
     */
    public List<FeedThumbnailRecord> loadGameThumbnails (String code, int gameId)
    {
        return loadThumbnails(code, gameId, 0);
    }

    /**
     * Sets the given thumbnails to be the ones assigned to the given game. I.e. DELETES all
     * current thumbnails for the game then stores the new ones.
     */
    public void saveGameThumbnails (int gameId, List<FeedThumbnailRecord> thumbnails)
    {
        saveThumbnails(gameId, 0, thumbnails);
    }

    /**
     * Loads all thumbnails assigned to the given application.
     */
    public List<FeedThumbnailRecord> loadAppThumbnails (int appId)
    {
        return loadThumbnails(null, 0, appId);
    }

    /**
     * Loads all thumbnails assigned to the given application with the given code.
     */
    public List<FeedThumbnailRecord> loadAppThumbnails(String code, int appId)
    {
        return loadThumbnails(code, 0, appId);
    }

    /**
     * Sets the given thumbnails to be the ones assigned to the given application. I.e. DELETES all
     * current thumbnails for the app then stores the new ones.
     */
    public void saveAppThumbnails (int appId, List<FeedThumbnailRecord> thumbnails)
    {
        saveThumbnails(0, appId, thumbnails);
    }

    /**
     * Loads the Kontagent info associated with the given application, or null if there is none.
     */
    public KontagentInfoRecord loadKontagentInfo (int appId)
    {
        return load(KontagentInfoRecord.getKey(appId));
    }

    /**
     * Saves the given kontagent info.
     */
    public void saveKontagentInfo (KontagentInfoRecord kinfo)
    {
        store(kinfo);
    }

    protected FacebookInfoRecord loadFacebookInfo (int gameId, int appId)
    {
        FacebookInfoRecord info = load(FacebookInfoRecord.getKey(gameId, appId));
        if (info == null) {
            info = new FacebookInfoRecord();
            info.gameId = gameId;
            info.appId = appId;
        }
        return info;
    }

    protected List<FeedThumbnailRecord> loadThumbnails (String code, int gameId, int appId)
    {
        List<SQLExpression<?>> conditions = Lists.newArrayList();
        conditions.add(FeedThumbnailRecord.GAME_ID.eq(gameId));
        conditions.add(FeedThumbnailRecord.APP_ID.eq(appId));
        if (code != null) {
            conditions.add(FeedThumbnailRecord.CODE.eq(code));
        }
        return from(FeedThumbnailRecord.class).where(conditions).select();
    }

    protected void saveThumbnails (int gameId, int appId, List<FeedThumbnailRecord> thumbs)
    {
        from(FeedThumbnailRecord.class).where(FeedThumbnailRecord.GAME_ID.eq(gameId),
                                              FeedThumbnailRecord.APP_ID.eq(appId)).delete();
        for (FeedThumbnailRecord thumb : thumbs) {
            insert(thumb);
        }
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(FacebookTemplateRecord.class);
        classes.add(FacebookActionRecord.class);
        classes.add(FacebookInfoRecord.class);
        classes.add(FeedThumbnailRecord.class);
        classes.add(KontagentInfoRecord.class);
    }
}
