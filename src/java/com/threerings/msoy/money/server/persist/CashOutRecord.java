//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.sql.Timestamp;

import net.jcip.annotations.NotThreadSafe;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.threerings.msoy.money.data.all.CashOutBillingInfo;
import com.threerings.msoy.money.data.all.CashOutInfo;

/**
 * Represents an attempt to cash out bling.  The request may be pending, fulfilled, or canceled.
 * If pending, it must be fulfilled or canceled by whoever is processing cash out requests.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Entity(indices={ @Index(name="ixTimeCompleted", fields={ CashOutRecord.TIME_FINISHED }) })
@NotThreadSafe
public class CashOutRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(CashOutRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #firstName} field. */
    public static final String FIRST_NAME = "firstName";

    /** The qualified column identifier for the {@link #firstName} field. */
    public static final ColumnExp FIRST_NAME_C =
        new ColumnExp(CashOutRecord.class, FIRST_NAME);

    /** The column identifier for the {@link #lastName} field. */
    public static final String LAST_NAME = "lastName";

    /** The qualified column identifier for the {@link #lastName} field. */
    public static final ColumnExp LAST_NAME_C =
        new ColumnExp(CashOutRecord.class, LAST_NAME);

    /** The column identifier for the {@link #paypalEmailAddress} field. */
    public static final String PAYPAL_EMAIL_ADDRESS = "paypalEmailAddress";

    /** The qualified column identifier for the {@link #paypalEmailAddress} field. */
    public static final ColumnExp PAYPAL_EMAIL_ADDRESS_C =
        new ColumnExp(CashOutRecord.class, PAYPAL_EMAIL_ADDRESS);

    /** The column identifier for the {@link #phoneNumber} field. */
    public static final String PHONE_NUMBER = "phoneNumber";

    /** The qualified column identifier for the {@link #phoneNumber} field. */
    public static final ColumnExp PHONE_NUMBER_C =
        new ColumnExp(CashOutRecord.class, PHONE_NUMBER);

    /** The column identifier for the {@link #streetAddress} field. */
    public static final String STREET_ADDRESS = "streetAddress";

    /** The qualified column identifier for the {@link #streetAddress} field. */
    public static final ColumnExp STREET_ADDRESS_C =
        new ColumnExp(CashOutRecord.class, STREET_ADDRESS);

    /** The column identifier for the {@link #city} field. */
    public static final String CITY = "city";

    /** The qualified column identifier for the {@link #city} field. */
    public static final ColumnExp CITY_C =
        new ColumnExp(CashOutRecord.class, CITY);

    /** The column identifier for the {@link #state} field. */
    public static final String STATE = "state";

    /** The qualified column identifier for the {@link #state} field. */
    public static final ColumnExp STATE_C =
        new ColumnExp(CashOutRecord.class, STATE);

    /** The column identifier for the {@link #postalCode} field. */
    public static final String POSTAL_CODE = "postalCode";

    /** The qualified column identifier for the {@link #postalCode} field. */
    public static final ColumnExp POSTAL_CODE_C =
        new ColumnExp(CashOutRecord.class, POSTAL_CODE);

    /** The column identifier for the {@link #country} field. */
    public static final String COUNTRY = "country";

    /** The qualified column identifier for the {@link #country} field. */
    public static final ColumnExp COUNTRY_C =
        new ColumnExp(CashOutRecord.class, COUNTRY);

    /** The column identifier for the {@link #timeRequested} field. */
    public static final String TIME_REQUESTED = "timeRequested";

    /** The qualified column identifier for the {@link #timeRequested} field. */
    public static final ColumnExp TIME_REQUESTED_C =
        new ColumnExp(CashOutRecord.class, TIME_REQUESTED);

    /** The column identifier for the {@link #timeFinished} field. */
    public static final String TIME_FINISHED = "timeFinished";

    /** The qualified column identifier for the {@link #timeFinished} field. */
    public static final ColumnExp TIME_FINISHED_C =
        new ColumnExp(CashOutRecord.class, TIME_FINISHED);

    /** The column identifier for the {@link #successful} field. */
    public static final String SUCCESSFUL = "successful";

    /** The qualified column identifier for the {@link #successful} field. */
    public static final ColumnExp SUCCESSFUL_C =
        new ColumnExp(CashOutRecord.class, SUCCESSFUL);

    /** The column identifier for the {@link #blingAmount} field. */
    public static final String BLING_AMOUNT = "blingAmount";

    /** The qualified column identifier for the {@link #blingAmount} field. */
    public static final ColumnExp BLING_AMOUNT_C =
        new ColumnExp(CashOutRecord.class, BLING_AMOUNT);

    /** The column identifier for the {@link #blingWorth} field. */
    public static final String BLING_WORTH = "blingWorth";

    /** The qualified column identifier for the {@link #blingWorth} field. */
    public static final ColumnExp BLING_WORTH_C =
        new ColumnExp(CashOutRecord.class, BLING_WORTH);

    /** The column identifier for the {@link #cancelReason} field. */
    public static final String CANCEL_REASON = "cancelReason";

    /** The qualified column identifier for the {@link #cancelReason} field. */
    public static final ColumnExp CANCEL_REASON_C =
        new ColumnExp(CashOutRecord.class, CANCEL_REASON);

    /** The column identifier for the {@link #actualCashedOut} field. */
    public static final String ACTUAL_CASHED_OUT = "actualCashedOut";

    /** The qualified column identifier for the {@link #actualCashedOut} field. */
    public static final ColumnExp ACTUAL_CASHED_OUT_C =
        new ColumnExp(CashOutRecord.class, ACTUAL_CASHED_OUT);
    // AUTO-GENERATED: FIELDS END

    /** ID of the member who requested a cash out. */
    @Id
    public int memberId;
    
    /** First name of the member. */
    public String firstName;
    
    /** Last name of the member. */
    public String lastName;
    
    /** Member's PayPal email address, to which the money will be sent. */
    public String paypalEmailAddress;
    
    /** Member's phone number. */
    public String phoneNumber;
    
    /** Member's street address. */
    public String streetAddress;
    
    /** Member's city. */
    public String city;
    
    /** Member's state. */
    public String state;
    
    /** Member's postal code. */
    public String postalCode;
    
    /** Member's country. */
    public String country;
    
    /** Date/time when this cash out was requested. */
    public Timestamp timeRequested;
    
    /** Date/time when this cash out was completed, or null if not completed. */
    @Column(nullable=true)
    public Timestamp timeFinished;
    
    /** True of the cash out completed successfully, false otherwise. */
    public boolean successful;
    
    /** Amount of bling (centibling) requested for cash out. */
    public int blingAmount;
    
    /** Worth of the bling at the time it was cashed out. */
    public float blingWorth;
    
    /** If completed unsuccessfully, indicates the reason this cash out was canceled. */
    @Column(nullable=true)
    public String cancelReason;
    
    /** If completed successfully, the actual amount of bling that was cashed out. */
    @Column(nullable=true)
    public Integer actualCashedOut;
    
    public static final int SCHEMA_VERSION = 4;

    public CashOutRecord () { }
    
    /**
     * Constructs a new cash out record in the pending state.
     * 
     * @param memberId ID of the member this record is for.
     * @param blingAmount Amount of centibling the user requested to cash out.
     * @param blingWorth Worth of bling in USD
     * @param info The user's billing information, indicating how their request should be
     * fulfilled.
     */
    public CashOutRecord (int memberId, int blingAmount, float blingWorth, CashOutBillingInfo info)
    {
        this.memberId = memberId;
        this.blingAmount = blingAmount;
        this.blingWorth = blingWorth;
        this.timeRequested = new Timestamp(System.currentTimeMillis());
        this.firstName = info.firstName;
        this.lastName = info.lastName;
        this.paypalEmailAddress = info.paypalEmailAddress;
        this.phoneNumber = info.phoneNumber;
        this.streetAddress = info.streetAddress;
        this.city = info.city;
        this.state = info.state;
        this.postalCode = info.postalCode;
        this.country = info.country;
    }
    
    /**
     * Constructs a CashOutInfo from the information in this record.
     */
    public CashOutInfo toInfo ()
    {
        return new CashOutInfo(blingAmount, (int)(blingWorth * blingAmount), 
            new CashOutBillingInfo(firstName, lastName, paypalEmailAddress, phoneNumber, 
            streetAddress, city, state, postalCode, country), timeRequested, timeFinished, 
            successful, actualCashedOut, cancelReason);
    }
    
    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link CashOutRecord}
     * with the supplied key values.
     */
    public static Key<CashOutRecord> getKey (int memberId)
    {
        return new Key<CashOutRecord>(
                CashOutRecord.class,
                new String[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
}
