//
// $Id$

package com.threerings.msoy.server;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.MsoyTokenRing;

/** Provides authentication information and services for a particular partner. */
public interface AuthenticationDomain
{
    /** Used to coordinate with authentication domains. */
    public static class Account
    {
        /** The account name in question. */
        public String accountName;

        /** The access privileges conferred to this account. */
        public MsoyTokenRing tokens;

        /** Whether or not this account is logging on for the first time. */
        public boolean firstLogon;
    }

    /** A string that can be passed to the Domain to bypass password checking. Pass this actual
     * instance. */
    public static final String PASSWORD_BYPASS = "pwBypass";

    /**
     * Creates a new account for this authentication domain.
     */
    public Account createAccount (String accountName, String password)
        throws ServiceException;

    /**
     * Uncreates an account that was created but needs to be deleted because of a later failure
     * in the account creation process.
     */
    public void uncreateAccount (String accountName);

    /**
     * Updates the authentication account name (email address) for the supplied account.
     *
     * @exception ServiceException thrown with {@link MsoyAuthCodes#NO_SUCH_USER} if the account in
     * question does not exist or {@link MsoyAuthCodes#DUPLICATE_EMAIL} if the account name in
     * question is already in use.
     */
    public void updateAccountName (String accountName, String newAccountName)
        throws ServiceException;

    /**
     * Updates the permaname associated with the supplied account.
     *
     * @exception ServiceException thrown with {@link MsoyAuthCodes#NO_SUCH_USER} if the account in
     * question does not exist or {@link MsoyAuthCodes#DUPLICATE_PERMANAME} if the name in question
     * is already in use.
     */
    public void updatePermaName (String accountName, String newPermaName)
        throws ServiceException;

    /**
     * Updates the password for the supplied account.
     *
     * @exception com.threerings.web.gwt.ServiceException thrown with {@link MsoyAuthCodes#NO_SUCH_USER} if the account in
     * question does not exist.
     */
    public void updatePassword (String accountName, String newPassword)
        throws ServiceException;

    /**
     * Loads up account information for the specified account and checks the supplied password.
     *
     * @exception com.threerings.web.gwt.ServiceException thrown with {@link MsoyAuthCodes#INVALID_LOGON} to indicate
     * that the account doesn't exist OR the password is incorrect. Differentiating between
     * those is not as secure.
     */
    public Account authenticateAccount (String accountName, String password)
        throws ServiceException;

    /**
     * Called with an account loaded from {@link #authenticateAccount} to check whether the
     * specified account is banned or if the supplied machine identifier should be prevented
     * from creating a new account.
     *
     * @param machIdent a unique identifier assigned to the machine from which this account is
     * logging in.
     * @param newIdent if the machIdent was generated on the server
     *
     * @exception com.threerings.web.gwt.ServiceException thrown with {@link MsoyAuthCodes#BANNED} if the account is
     * banned or {@link MsoyAuthCodes#MACHINE_TAINTED} if the machine identifier provided is
     * associated with a banned account and this is the account's first logon.
     */
    public void validateAccount (Account account, String machIdent, boolean newIdent)
        throws ServiceException;

    /**
     * Called with an account loaded from {@link #authenticateAccount} to check whether the
     * specified account is banned. This is used when the account logs in from a
     * non-machine-ident supporting client (like the web browser).
     *
     * @exception com.threerings.web.gwt.ServiceException thrown with {@link MsoyAuthCodes#BANNED} if the account is
     * banned.
     */
    public void validateAccount (Account account)
        throws ServiceException;

    /**
     * Generates a secret code that can be emailed to a user and then subsequently passed to
     * {@link #validatePasswordResetCode} to confirm that the user is in fact receiving email
     * sent to the address via which their account is registered.
     *
     * @return null if no account is registered for that address, a secret code otherwise.
     */
    public String generatePasswordResetCode (String accountName)
        throws ServiceException;

    /**
     * Validates that the supplied password reset code is the one earlier provided by a call to
     * {@link #generatePasswordResetCode}.
     *
     * @return true if the code is valid, false otherwise.
     */
    public boolean validatePasswordResetCode (String accountName, String code)
        throws ServiceException;

    /**
     * Validates that this is a unique machine identifier.
     */
    public boolean isUniqueIdent (String machIdent);
}
