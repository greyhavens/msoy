//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.expression.ColumnExp;

@Computed
@Entity
public class MemberCountRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #population} field. */
    public static final String POPULATION = "population";

    /** The qualified column identifier for the {@link #population} field. */
    public static final ColumnExp POPULATION_C =
        new ColumnExp(MemberCountRecord.class, POPULATION);
    // AUTO-GENERATED: FIELDS END

    /** The active member population. */
    public int population;
}
