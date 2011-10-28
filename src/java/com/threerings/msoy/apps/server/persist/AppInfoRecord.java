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
import com.threerings.msoy.web.gwt.ClientMode;

/**
 * Persistent representation of basic application information.
 */
@Entity
public class AppInfoRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<AppInfoRecord> _R = AppInfoRecord.class;
    public static final ColumnExp<Integer> APP_ID = colexp(_R, "appId");
    public static final ColumnExp<String> NAME = colexp(_R, "name");
    public static final ColumnExp<ClientMode> CLIENT_MODE = colexp(_R, "clientMode");
    public static final ColumnExp<Integer> GROUP_ID = colexp(_R, "groupId");
    // AUTO-GENERATED: FIELDS END

    /**
     * Depot-inspected field regulating the update of the table's columns and migrations.
     */
    public static final int SCHEMA_VERSION = 3;

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

    /** The mode the client should be in when accessing this application. */
    public ClientMode clientMode;

    /** The Whirled group associated with this application, if any. */
    public int groupId;

    /**
     * Converts this record to a new runtime instance.
     */
    public AppInfo toAppInfo ()
    {
        AppInfo appInfo = new AppInfo();
        appInfo.appId = appId;
        appInfo.name = name;
        appInfo.clientMode = clientMode;
        appInfo.groupId = groupId;
        return appInfo;
    }

    /**
     * Copies mutable fields from the given runtime instance into this record.
     */
    public void update (AppInfo appInfo)
    {
        this.name = appInfo.name;
        this.clientMode = appInfo.clientMode;
        this.groupId = appInfo.groupId;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link AppInfoRecord}
     * with the supplied key values.
     */
    public static Key<AppInfoRecord> getKey (int appId)
    {
        return newKey(_R, appId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(APP_ID); }
    // AUTO-GENERATED: METHODS END
}
