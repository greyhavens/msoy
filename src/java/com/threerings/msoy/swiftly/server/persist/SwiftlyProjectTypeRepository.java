//
// $Id$

package com.threerings.msoy.swiftly.server.persist;


import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.DuplicateKeyException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.DepotRepository;

import java.util.ArrayList;

/**
 * Manages the persistent information associated with a member's project templates.
 */
public class SwiftlyProjectTypeRepository extends DepotRepository
{
    public SwiftlyProjectTypeRepository (ConnectionProvider conprov)
    {
        super(conprov);
    }

    public ArrayList<SwiftlyProjectTypeRecord> getProjectTypes (int memberId)
        throws PersistenceException
    {
        // TODO: Use privileges, instead of just ownership.
        ArrayList<SwiftlyProjectTypeRecord> types = new ArrayList<SwiftlyProjectTypeRecord>();
        for (SwiftlyProjectTypeRecord record : findAll(SwiftlyProjectTypeRecord.class)) {
            types.add(record);                  
        }

        return types;
    }

}
