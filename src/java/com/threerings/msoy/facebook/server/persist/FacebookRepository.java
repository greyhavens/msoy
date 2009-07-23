//
// $Id$

package com.threerings.msoy.facebook.server.persist;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Where;

/**
 * Manages persistent structures for integrating with Facebook.
 */
@Singleton
public class FacebookRepository extends DepotRepository
{
    /**
     * Assembles the unique id for a trophy published action.
     */
    public static String getTrophyPublishedActionId (int gameId, String trophyIdent)
    {
        return gameId + ":" + trophyIdent;
    }

    /**
     * Creates a new repository.
     */
    @Inject
    public FacebookRepository (PersistenceContext context)
    {
        super(context);
    }

    /**
     * Adds or updates the given template.
     */
    public void storeTemplate (FacebookTemplateRecord template)
    {
        store(template);
    }

    /**
     * Deletes the template record with the given code.
     */
    public void deleteTemplate (String code, String variant)
    {
        delete(FacebookTemplateRecord.getKey(code, variant));
    }

    /**
     * Loads a list of all saved templates.
     */
    public List<FacebookTemplateRecord> loadTemplates ()
    {
        return findAll(FacebookTemplateRecord.class, CacheStrategy.NONE);
    }

    /**
     * Loads a list of all templates for the given code.
     */
    public List<FacebookTemplateRecord> loadVariants (String code)
    {
        return findAll(FacebookTemplateRecord.class,
            new Where(FacebookTemplateRecord.CODE.eq(code)));
    }

    /**
     * Records a member having published a trophy with the given identifier and game.
     */
    public void noteTrophyPublished (int memberId, int gameId, String trophyIdent)
    {
        store(createAction(memberId, FacebookActionRecord.Type.PUBLISHED_TROPHY,
            getTrophyPublishedActionId(gameId, trophyIdent)));
    }

    /**
     * Loads all actions performed by the member of the given id.
     */
    public List<FacebookActionRecord> loadActions (int memberId)
    {
        return findAll(FacebookActionRecord.class, new Where(
            FacebookActionRecord.MEMBER_ID, memberId));
    }

    /**
     * Removes old facbook action records.
     */
    public void pruneActions ()
    {
        // TODO: implement and schedule with cron logic
    }

    /**
     * Loads all notifications.
     */
    public List<FacebookNotificationRecord> loadNotifications ()
    {
        return findAll(FacebookNotificationRecord.class);
    }

    /**
     * Updates or adds a new notification.
     */
    public void storeNotification (String id, String text)
    {
        FacebookNotificationRecord notifRec = new FacebookNotificationRecord();
        notifRec.id = id;
        notifRec.text = text;
        store(notifRec);
    }

    /**
     * Loads and returns the text of the notification with the given id, or null if it does not
     * exist.
     */
    public String getNotification (String id)
    {
        FacebookNotificationRecord notifRec = load(FacebookNotificationRecord.getKey(id));
        return notifRec == null ? null : notifRec.text;
    }

    /**
     * Deletes the notification with the given id.
     */
    public void deleteNotification (String id)
    {
        delete(FacebookNotificationRecord.getKey(id));
    }

    /**
     * Creates a new action with the given fields and the current time.
     */
    protected FacebookActionRecord createAction (
        int memberId, FacebookActionRecord.Type type, String id)
    {
        FacebookActionRecord action = new FacebookActionRecord();
        action.memberId = memberId;
        action.type = type;
        action.id = id;
        action.timestamp = new Timestamp(System.currentTimeMillis());
        return action;
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(FacebookTemplateRecord.class);
        classes.add(FacebookActionRecord.class);
        classes.add(FacebookNotificationRecord.class);
    }
}
