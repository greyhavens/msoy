//
// $Id$

package com.threerings.msoy.apps.server.persist;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.apps.gwt.AppInfo;

/**
 * Persistent representation of basic application information.
 */
@Entity
public class AppInfoRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<AppInfoRecord> _R = AppInfoRecord.class;
    public static final ColumnExp APP_ID = colexp(_R, "appId");
    public static final ColumnExp NAME = colexp(_R, "name");
    // AUTO-GENERATED: FIELDS END

    /**
     * Depot-inspected field regulating the update of the table's columns and migrations.
     */
    public static final int SCHEMA_VERSION = 1;

    /**
     * Function to {@link Lists#transform} a record to a runtime instance. 
     */
    public static final Function<AppInfoRecord, AppInfo> TO_APP_INFO =
        new Function<AppInfoRecord, AppInfo> () {
        public AppInfo apply (AppInfoRecord app) {
            return app.toAppInfo();
        }
    };

    /** The unique id of the application. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int appId;

    /** The name of the application. */
    @Column(length=AppInfo.MAX_NAME_LENGTH)
    public String name;

    /**
     * Converts this record to a new runtime instance.
     */
    public AppInfo toAppInfo ()
    {
        AppInfo appInfo = new AppInfo();
        appInfo.appId = appId;
        appInfo.name = name;
        return appInfo;
    }

    /**
     * Copies mutable fields from the given runtime instance into this record.
     */
    public void update (AppInfo appInfo)
    {
        this.name = appInfo.name;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link AppInfoRecord}
     * with the supplied key values.
     */
    public static Key<AppInfoRecord> getKey (int appId)
    {
        return new Key<AppInfoRecord>(
                AppInfoRecord.class,
                new ColumnExp[] { APP_ID },
                new Comparable[] { appId });
    }
    // AUTO-GENERATED: METHODS END
}
