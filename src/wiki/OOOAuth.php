<?php
#
# Copyright (C) 2009 Grey Havens, Inc.
# All Rights Reserved.
#
# Requires $wgOOOAuthServerURL to be set to http://www.whirled.com/ooo (or corollary)

require_once('AuthPlugin.php');

class OOOAuth extends AuthPlugin {

    function OOOAuth () {
    }

    /**
     * Disallow password change.
     *
     * @return bool
     */
    function allowPasswordChange () {
        return false;
    }

    /**
     * This should not be called because we do not allow password change.  Always
     * fail by returning false.
     *
     * @param $user User object.
     * @param $password String: password.
     * @return bool
     * @public
     */
    function setPassword ($user, $password) {
        return false;
    }

    /**
     * We don't support this but we have to return true for preferences to save.
     *
     * @param $user User object.
     * @return bool
     * @public
     */
    function updateExternalDB ($user) {
        return true;
    }

    /**
     * We can't create external accounts so return false.
     *
     * @return bool
     * @public
     */
    function canCreateAccounts () {
        return false;
    }

    /**
     * We don't support adding users to whatever service provides REMOTE_USER, so
     * fail by always returning false.
     *
     * @param User $user
     * @param string $password
     * @return bool
     * @public
     */
    function addUser ($user, $password) {
        return false;
    }

    /**
     * Pretend all users exist.  This is checked by authenticateUserData to
     * determine if a user exists in our 'db'.  By returning true we tell it that
     * it can create a local wiki user automatically.
     *
     * @param $username String: username.
     * @return bool
     * @public
     */
    function userExists ($username) {
        return true;
    }

    /**
     * Attempt to authenticate the user via XML-RPC.
     *
     * @param $username String: username.
     * @param $password String: user password.
     * @return bool
     * @public
     */
    function authenticate ($username, $password) {
        global $wgOOOAuthServerURL;

        $request = xmlrpc_encode_request("user.authUserForWiki", array($username, md5($password)));
        $context = stream_context_create(
            array('http' => array('method' => "POST",
                                  'header' => "Content-Type: text/xml",
                                  'content' => $request)));
        $file = file_get_contents($wgOOOAuthServerURL, false, $context);
        $response = xmlrpc_decode($file);
        if (is_array($response) && xmlrpc_is_fault($response)) {
            trigger_error("xmlrpc: $response[faultString] ($response[faultCode])");
            return false;
        } else {
            return ($response == 1);
        }
    }

    /**
     * Check to see if the specific domain is a valid domain.
     *
     * @param $domain String: authentication domain.
     * @return bool
     * @public
     */
    function validDomain ($domain) {
        return true;
    }

    /**
     * When a user logs in, optionally fill in preferences and such.
     * For instance, you might pull the email address or real name from the
     * external user database.
     *
     * The User object is passed by reference so it can be modified; don't
     * forget the & on your function declaration.
     *
     * @param User $user
     * @public
     */
    function updateUser (&$user) {
        // We only set this stuff when accounts are created.
        return true;
    }

    /**
     * Return true because the wiki should create a new local account
     * automatically when asked to login a user who doesn't exist locally but
     * does in the external auth database.
     *
     * @return bool
     * @public
     */
    function autoCreate () {
        return true;
    }

    /**
     * Return true to prevent logins that don't authenticate here from being
     * checked against the local database's password fields.
     *
     * @return bool
     * @public
     */
    function strict () {
        return true;
    }

    /**
     * When creating a user account, optionally fill in preferences and such.
     * For instance, you might pull the email address or real name from the
     * external user database.
     *
     * @param $user User object.
     * @public
     */
    function initUser (&$user) {
        $user->mEmailAuthenticated = wfTimestampNow();
        $user->setToken();
        $user->saveSettings();
    }

    /**
     * Modify options in the login template.  This shouldn't be very important
     * because no one should really be bothering with the login page.
     *
     * @param $template UserLoginTemplate object.
     * @public
     */
    function modifyUITemplate(&$template) {
        // disable the mail new password box
        $template->set('useemail', false);
        $template->set('create', false);
        $template->set('domain', false);
        $template->set('usedomain', false);
    }

    /**
     * Normalize user names to the mediawiki standard to prevent duplicate
     * accounts.
     *
     * @param $username String: username.
     * @return string
     * @public
     */
    function getCanonicalName($username) {
        // lowercase the username
        $username = strtolower($username);
        // uppercase first letter to make mediawiki happy
        $username[0] = strtoupper($username[0]);
        return $username;
    }
}
?>
