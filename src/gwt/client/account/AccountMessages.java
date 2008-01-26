package client.account;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/account/AccountMessages.properties'.
 */
public interface AccountMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "Password:".
   * 
   * @return translated "Password:"
   * @gwt.key createPassword
   */
  String createPassword();

  /**
   * Translated "You can email or IM this URL to your friend in case they don''t receive our invitation email.".
   * 
   * @return translated "You can email or IM this URL to your friend in case they don''t receive our invitation email."
   * @gwt.key sendInvitesPendingTip
   */
  String sendInvitesPendingTip();

  /**
   * Translated "Your real name is not required, and will never be displayed in Whirled. It is used for searching purposes only.".
   * 
   * @return translated "Your real name is not required, and will never be displayed in Whirled. It is used for searching purposes only."
   * @gwt.key createRealNameTip
   */
  String createRealNameTip();

  /**
   * Translated "Welcome to Whirled! Register here to get your very own house, to join groups and to keep track of your friends.".
   * 
   * @return translated "Welcome to Whirled! Register here to get your very own house, to join groups and to keep track of your friends."
   * @gwt.key createIntro
   */
  String createIntro();

  /**
   * Translated "Invite Friends to Join You in Whirled".
   * 
   * @return translated "Invite Friends to Join You in Whirled"
   * @gwt.key sendInvitesSubtitle
   */
  String sendInvitesSubtitle();

  /**
   * Translated "Welcome!".
   * 
   * @return translated "Welcome!"
   * @gwt.key welcomeTitle
   */
  String welcomeTitle();

  /**
   * Translated "Failed for {0}: {1}".
   * 
   * @return translated "Failed for {0}: {1}"
   * @gwt.key sendInvitesResultsFailed
   */
  String sendInvitesResultsFailed(String arg0,  String arg1);

  /**
   * Translated "Custom Message:".
   * 
   * @return translated "Custom Message:"
   * @gwt.key sendInvitesCustomMessage
   */
  String sendInvitesCustomMessage();

  /**
   * Translated "You must be at least 13 years old to use Whirled.".
   * 
   * @return translated "You must be at least 13 years old to use Whirled."
   * @gwt.key createNotThirteen
   */
  String createNotThirteen();

  /**
   * Translated "Whirled is still an alpha product, with plenty of hiccups to go around.  The current audience is a small group of people trusted to help us test and expand Whirled.  If you would like to join the crew, click on "Accept Invitation" below, and you can create an account to call your own!".
   * 
   * @return translated "Whirled is still an alpha product, with plenty of hiccups to go around.  The current audience is a small group of people trusted to help us test and expand Whirled.  If you would like to join the crew, click on "Accept Invitation" below, and you can create an account to call your own!"
   * @gwt.key inviteBody2
   */
  String inviteBody2();

  /**
   * Translated "Anonymous".
   * 
   * @return translated "Anonymous"
   * @gwt.key sendInvitesAnonymous
   */
  String sendInvitesAnonymous();

  /**
   * Translated "You have been invited to join the burgeoning community of First Whirled! {0} has already discovered what a great experience Whirled can be, and would like you to join the fun.  ".
   * 
   * @return translated "You have been invited to join the burgeoning community of First Whirled! {0} has already discovered what a great experience Whirled can be, and would like you to join the fun.  "
   * @gwt.key inviteBody1
   */
  String inviteBody1(String arg0);

  /**
   * Translated "Click "Let''s Go" below to create your account!".
   * 
   * @return translated "Click "Let''s Go" below to create your account!"
   * @gwt.key createReady
   */
  String createReady();

  /**
   * Translated "Invitations".
   * 
   * @return translated "Invitations"
   * @gwt.key sendInvitesTitle
   */
  String sendInvitesTitle();

  /**
   * Translated "Enter your email address.".
   * 
   * @return translated "Enter your email address."
   * @gwt.key createMissingEmail
   */
  String createMissingEmail();

  /**
   * Translated "Please enter a display name of at least {0} characters in length.".
   * 
   * @return translated "Please enter a display name of at least {0} characters in length."
   * @gwt.key createNameTooShort
   */
  String createNameTooShort(String arg0);

  /**
   * Translated "You''ll use your email address to logon.".
   * 
   * @return translated "You''ll use your email address to logon."
   * @gwt.key createEmailTip
   */
  String createEmailTip();

  /**
   * Translated "Invalid email addresses found: {0}".
   * 
   * @return translated "Invalid email addresses found: {0}"
   * @gwt.key sendInvitesInvalidAddress
   */
  String sendInvitesInvalidAddress(String arg0);

  /**
   * Translated "Confirm:".
   * 
   * @return translated "Confirm:"
   * @gwt.key createConfirm
   */
  String createConfirm();

  /**
   * Translated "Too many addresses ({0}): you have {1} invitations available".
   * 
   * @return translated "Too many addresses ({0}): you have {1} invitations available"
   * @gwt.key sendInvitesTooMany
   */
  String sendInvitesTooMany(String arg0,  String arg1);

  /**
   * Translated "Come hang out with me on this crazy new website.".
   * 
   * @return translated "Come hang out with me on this crazy new website."
   * @gwt.key sendInvitesCustomDefault
   */
  String sendInvitesCustomDefault();

  /**
   * Translated "You must be at least 13 years old to play in Whirled.".
   * 
   * @return translated "You must be at least 13 years old to play in Whirled."
   * @gwt.key createDateOfBirthTip
   */
  String createDateOfBirthTip();

  /**
   * Translated "Re-enter your password to confirm it.".
   * 
   * @return translated "Re-enter your password to confirm it."
   * @gwt.key createMissingConfirm
   */
  String createMissingConfirm();

  /**
   * Translated "Send Invitation Results".
   * 
   * @return translated "Send Invitation Results"
   * @gwt.key sendInvitesResults
   */
  String sendInvitesResults();

  /**
   * Translated "Create a New Account".
   * 
   * @return translated "Create a New Account"
   * @gwt.key createTitle
   */
  String createTitle();

  /**
   * Translated "Welcome, {0}".
   * 
   * @return translated "Welcome, {0}"
   * @gwt.key inviteIntro
   */
  String inviteIntro(String arg0);

  /**
   * Translated "Please enter your date of birth.".
   * 
   * @return translated "Please enter your date of birth."
   * @gwt.key createMissingDoB
   */
  String createMissingDoB();

  /**
   * Translated "Email address:".
   * 
   * @return translated "Email address:"
   * @gwt.key createEmail
   */
  String createEmail();

  /**
   * Translated "You have been invited to join the burgeoning community of First Whirled!".
   * 
   * @return translated "You have been invited to join the burgeoning community of First Whirled!"
   * @gwt.key inviteBody1anon
   */
  String inviteBody1anon();

  /**
   * Translated "Accept Invitation".
   * 
   * @return translated "Accept Invitation"
   * @gwt.key inviteAccept
   */
  String inviteAccept();

  /**
   * Translated "Please enter at least 1 address to send an invitation".
   * 
   * @return translated "Please enter at least 1 address to send an invitation"
   * @gwt.key sendInvitesEnterAddresses
   */
  String sendInvitesEnterAddresses();

  /**
   * Translated "Send Invites".
   * 
   * @return translated "Send Invites"
   * @gwt.key sendInvitesSendEmail
   */
  String sendInvitesSendEmail();

  /**
   * Translated "Enter a password for your account.".
   * 
   * @return translated "Enter a password for your account."
   * @gwt.key createMissingPassword
   */
  String createMissingPassword();

  /**
   * Translated "Real Name:".
   * 
   * @return translated "Real Name:"
   * @gwt.key createRealName
   */
  String createRealName();

  /**
   * Translated "Enter up to {0} email address below (each on a new line), along with a custom message to invite your friends to Whirled!".
   * 
   * @return translated "Enter up to {0} email address below (each on a new line), along with a custom message to invite your friends to Whirled!"
   * @gwt.key sendInvitesSendTip
   */
  String sendInvitesSendTip(String arg0);

  /**
   * Translated "You have {0} invitations available".
   * 
   * @return translated "You have {0} invitations available"
   * @gwt.key sendInvitesSendHeader
   */
  String sendInvitesSendHeader(String arg0);

  /**
   * Translated "Your Whirled Invitation".
   * 
   * @return translated "Your Whirled Invitation"
   * @gwt.key inviteTitle
   */
  String inviteTitle();

  /**
   * Translated "Duplicate email address found: {0}".
   * 
   * @return translated "Duplicate email address found: {0}"
   * @gwt.key sendInvitesDuplicateAddress
   */
  String sendInvitesDuplicateAddress(String arg0);

  /**
   * Translated "Display name:".
   * 
   * @return translated "Display name:"
   * @gwt.key createDisplayName
   */
  String createDisplayName();

  /**
   * Translated "Your display name will be shown to other users in Whirled, and is not permanent.".
   * 
   * @return translated "Your display name will be shown to other users in Whirled, and is not permanent."
   * @gwt.key createDisplayNameTip
   */
  String createDisplayNameTip();

  /**
   * Translated "You currently have no pending invitations.".
   * 
   * @return translated "You currently have no pending invitations."
   * @gwt.key sendInvitesNoPending
   */
  String sendInvitesNoPending();

  /**
   * Translated "That invitation could not be found. Please check that the URL is exactly the same as the URL you received in your invitation email.".
   * 
   * @return translated "That invitation could not be found. Please check that the URL is exactly the same as the URL you received in your invitation email."
   * @gwt.key inviteMissing
   */
  String inviteMissing();

  /**
   * Translated "Please logon above to access account information.".
   * 
   * @return translated "Please logon above to access account information."
   * @gwt.key indexLogon
   */
  String indexLogon();

  /**
   * Translated "Email Addresses:".
   * 
   * @return translated "Email Addresses:"
   * @gwt.key sendInvitesEmailAddresses
   */
  String sendInvitesEmailAddresses();

  /**
   * Translated "Pending Invitations".
   * 
   * @return translated "Pending Invitations"
   * @gwt.key sendInvitesPendingHeader
   */
  String sendInvitesPendingHeader();

  /**
   * Translated "Birthdate:".
   * 
   * @return translated "Birthdate:"
   * @gwt.key createDateOfBirth
   */
  String createDateOfBirth();

  /**
   * Translated "Sent invite to: {0}".
   * 
   * @return translated "Sent invite to: {0}"
   * @gwt.key sendInvitesResultsSuccessful
   */
  String sendInvitesResultsSuccessful(String arg0);

  /**
   * Translated "Creating account...".
   * 
   * @return translated "Creating account..."
   * @gwt.key creatingAccount
   */
  String creatingAccount();

  /**
   * Translated "Let''s Go!".
   * 
   * @return translated "Let''s Go!"
   * @gwt.key createCreate
   */
  String createCreate();

  /**
   * Translated "The passwords you''ve entered do not match.".
   * 
   * @return translated "The passwords you''ve entered do not match."
   * @gwt.key createPasswordMismatch
   */
  String createPasswordMismatch();

  /**
   * Translated "Don''t ever tell anyone your password, not even us! Only enter your password into the box used to logon, nowhere else.".
   * 
   * @return translated "Don''t ever tell anyone your password, not even us! Only enter your password into the box used to logon, nowhere else."
   * @gwt.key createPasswordTip
   */
  String createPasswordTip();

  /**
   * Translated "You have no invitations available to send.".
   * 
   * @return translated "You have no invitations available to send."
   * @gwt.key sendInvitesNoneAvailable
   */
  String sendInvitesNoneAvailable();
}
