//
// $Id$

package com.threerings.msoy.server;

import java.util.Random;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.admin.server.persist.ABTestRecord;
import com.threerings.msoy.admin.server.persist.ABTestRepository;

import com.threerings.msoy.web.data.ABTest;

import static com.threerings.msoy.Log.log;

/**
 * Contains member related services that are used by servlets and other blocking thread code.
 */
@BlockingThread @Singleton
public class MemberLogic
{
    /**
     * Return the a/b test group that a member or visitor belongs to for a given a/b test,
     * generated psudo-randomly based on their tracking ID and the test name.  If the visitor is
     * not eligible for the a/b test, return < 0.
     *
     * @param testName String identifier for the test
     * @param trackingId Visitor's tracking guid
     * @param affiliate String identifier for the visitor's affiliate (eg miniclip)
     * @param vector String identifier for the visitor's vector
     * @param creative String identifier for the visitor's creative
     * @param newVisitor Is this the visitor's first session?
     * 
     * @return The a/b group the visitor has been assigned to, or < 0 for no group.
     */
    public int getABTestGroup (
        String testName, String trackingId, String affiliate, String vector, String creative, 
        boolean newVisitor)
    {
        ABTest test = null;
        try {
            ABTestRecord record = _testRepo.loadTestByName(testName);
            if (record == null) {
                log.warning("Unknown A/B Test in getABTestGroup: " + testName);
                return -1;
            }
            test = record.toABTest();
        } catch (PersistenceException pe) {
            log.warning("Failed to select A/B Test", pe);
            return -1;
        }
        
        // test is not running
        if (test.enabled == false) {
            return -1;
        }
        
        // do affiliate, etc match the requirements for the test
        if (!eligibleForABTest(test, affiliate, vector, creative, newVisitor)) {
            return -1;
        }
        
        // generate the group number based on trackingID + testName
        int seed = new String(trackingId + testName).hashCode();
        final Random rand = new Random(seed);
        final int group = rand.nextInt(test.numGroups) + 1;
        
        return group;
    }
    
    /**
     * Return true if the visitor's attributes match those required by the given a/b test 
     */
    protected boolean eligibleForABTest (
        ABTest test, String affiliate, String vector, String creative, boolean newVisitor)
    {
        // test runs only on new users and visitor is returning
        // (visitor may have been in a group during a previous session!)
        if (test.onlyNewVisitors == true && newVisitor == false) {
            return false;
        }

        // wrong affiliate
        if (test.affiliate != null && test.affiliate.length() > 0 
                && (affiliate == null || !affiliate.trim().equals(test.affiliate))) {
            return false;
        }

        // wrong vector
        if (test.vector != null && test.vector.length() > 0 
                && (vector == null || !vector.trim().equals(test.vector))) {
            return false;
        }
        
        // wrong creative
        if (test.creative != null && test.creative.length() > 0 
                && (creative == null || !creative.trim().equals(test.creative))) {
            return false;
        }
        
        return true;
    }

    // dependencies
    @Inject protected ABTestRepository _testRepo;
}
