//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Date;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Summarizes the amount of each type of flow granted on any given day.
 */
@Entity
public class DailyFlowSummary extends PersistentRecord
{
    public static final int SCHEMA_VERSION = 1;
    
    public static final String GRANT_TYPE = "grantType";
    public static final ColumnExp GRANT_TYPE_C =
        new ColumnExp(DailyFlowSummary.class, GRANT_TYPE);
    public static final String GRANTED = "granted";
    public static final ColumnExp GRANTED_C = new ColumnExp(DailyFlowSummary.class, GRANTED);
    public static final String GRANT_DATE = "grantDate";
    public static final ColumnExp GRANT_DATE_C = new ColumnExp(DailyFlowSummary.class, GRANT_DATE);

    /** The type of grant summarized by this entry. */
    @Id
    public String grantType;

    /** The date for which this entry is a summary. */
    @Id
    public Date grantDate;
    
    /** The total amount of flow granted. */
    public int granted;
}
