//
// $Id$

package com.threerings.msoy.money.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

public class CashOutBillingInfo
    implements IsSerializable
{
    /** First name of the member. */
    public /* final */ String firstName;
    
    /** Last name of the member. */
    public /* final */ String lastName;
    
    /** Member's PayPal email address, to which the money will be sent. */
    public /* final */ String paypalEmailAddress;
    
    /** Member's phone number. */
    public /* final */ String phoneNumber;
    
    /** Member's street address. */
    public /* final */ String streetAddress;
    
    /** Member's city. */
    public /* final */ String city;
    
    /** Member's state. */
    public /* final */ String state;
    
    /** Member's postal code. */
    public /* final */ String postalCode;
    
    /** Member's country. */
    public /* final */ String country;
    
    public CashOutBillingInfo (String firstName, String lastName, String paypalEmailAddress,
            String phoneNumber, String streetAddress, String city, String state,
            String postalCode, String country)
    {
        this.firstName = firstName;
        this.lastName = lastName;
        this.paypalEmailAddress = paypalEmailAddress;
        this.phoneNumber = phoneNumber;
        this.streetAddress = streetAddress;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }
    
    /** For serialization purposes. */
    public CashOutBillingInfo () { }
}
