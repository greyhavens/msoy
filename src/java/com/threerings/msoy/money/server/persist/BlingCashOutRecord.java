//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.sql.Timestamp;

import net.jcip.annotations.NotThreadSafe;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;
import com.threerings.msoy.money.data.all.CashOutBillingInfo;
import com.threerings.msoy.money.data.all.CashOutInfo;

/**
 * Represents an attempt to cash out bling.  The request may be pending, fulfilled, or canceled.
 * If pending, it must be fulfilled or canceled by whoever is processing cash out requests.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Entity
@NotThreadSafe
public class BlingCashOutRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<BlingCashOutRecord> _R = BlingCashOutRecord.class;
    public static final ColumnExp ID = colexp(_R, "id");
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp FIRST_NAME = colexp(_R, "firstName");
    public static final ColumnExp LAST_NAME = colexp(_R, "lastName");
    public static final ColumnExp PAYPAL_EMAIL_ADDRESS = colexp(_R, "paypalEmailAddress");
    public static final ColumnExp PHONE_NUMBER = colexp(_R, "phoneNumber");
    public static final ColumnExp STREET_ADDRESS = colexp(_R, "streetAddress");
    public static final ColumnExp CITY = colexp(_R, "city");
    public static final ColumnExp STATE = colexp(_R, "state");
    public static final ColumnExp POSTAL_CODE = colexp(_R, "postalCode");
    public static final ColumnExp COUNTRY = colexp(_R, "country");
    public static final ColumnExp TIME_REQUESTED = colexp(_R, "timeRequested");
    public static final ColumnExp TIME_FINISHED = colexp(_R, "timeFinished");
    public static final ColumnExp SUCCESSFUL = colexp(_R, "successful");
    public static final ColumnExp BLING_AMOUNT = colexp(_R, "blingAmount");
    public static final ColumnExp BLING_WORTH = colexp(_R, "blingWorth");
    public static final ColumnExp CANCEL_REASON = colexp(_R, "cancelReason");
    public static final ColumnExp ACTUAL_CASHED_OUT = colexp(_R, "actualCashedOut");
    // AUTO-GENERATED: FIELDS END

    /** Unique ID of the record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    /** ID of the member who requested a cash out. */
    @Index(name="ixMemberId")
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
    @Column(nullable=true) @Index(name="ixTimeCompleted")
    public Timestamp timeFinished;

    /** True of the cash out completed successfully, false otherwise. */
    public boolean successful;

    /** Amount of bling (centibling) requested for cash out. */
    public int blingAmount;

    /** Worth per bling in USD cents at the time it was cashed out. */
    public int blingWorth;

    /** If completed unsuccessfully, indicates the reason this cash out was canceled. */
    @Column(nullable=true)
    public String cancelReason;

    /** If completed successfully, the actual amount of bling that was cashed out. */
    @Column(nullable=true)
    public Integer actualCashedOut;

    public static final int SCHEMA_VERSION = 2;

    public BlingCashOutRecord () { }

    /**
     * Constructs a new cash out record in the pending state.
     *
     * @param memberId ID of the member this record is for.
     * @param blingAmount Amount of centibling the user requested to cash out.
     * @param blingWorth Worth of bling in USD
     * @param info The user's billing information, indicating how their request should be
     * fulfilled.
     */
    public BlingCashOutRecord (int memberId, int blingAmount, int blingWorth,
        CashOutBillingInfo info)
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
        return new CashOutInfo(blingAmount, blingWorth * blingAmount / 100,
            new CashOutBillingInfo(firstName, lastName, paypalEmailAddress, phoneNumber,
            streetAddress, city, state, postalCode, country), timeRequested, timeFinished,
            successful, actualCashedOut, cancelReason);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link BlingCashOutRecord}
     * with the supplied key values.
     */
    public static Key<BlingCashOutRecord> getKey (int id)
    {
        return new Key<BlingCashOutRecord>(
                BlingCashOutRecord.class,
                new ColumnExp[] { ID },
                new Comparable[] { id });
    }
    // AUTO-GENERATED: METHODS END
}
