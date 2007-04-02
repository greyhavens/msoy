//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.msoy.server.persist.TagRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;

import static com.threerings.msoy.Log.log;

/**
 * Manages the persistent store of {@link Decor} items.
 */
public class DecorRepository extends ItemRepository<
    DecorRecord,
    DecorCloneRecord,
    DecorCatalogRecord,
    DecorRatingRecord>
{
    @Entity(name="DecorTagRecord")
    public static class DecorTagRecord extends TagRecord
    {
    }

    @Entity(name="DecorTagHistoryRecord")
    public static class DecorTagHistoryRecord extends TagHistoryRecord
    {
    }

    public DecorRepository (ConnectionProvider provider)
    {
        super(provider);
        
        // force the creation of appropriate db table. this will be used by MsoySceneRepository
        // decor migration code.
        //
        // FIXME ROBERT: this can be removed at the same time as Decor migration code,
        // after April 15 2007
        try {
            _ctx.getMarshaller(getItemClass());
        } catch (PersistenceException e) {
            // nada
        }
        // end fixme
    }

    @Override
    protected Class<DecorRecord> getItemClass () {
        return DecorRecord.class;
    }
    
    @Override
    protected Class<DecorCatalogRecord> getCatalogClass ()
    {
        return DecorCatalogRecord.class;
    }

    @Override
    protected Class<DecorCloneRecord> getCloneClass ()
    {
        return DecorCloneRecord.class;
    }
    
    @Override
    protected Class<DecorRatingRecord> getRatingClass ()
    {
        return DecorRatingRecord.class;
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new DecorTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new DecorTagHistoryRecord();
    }
}
