package client.shell;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/shell/ShellMessages.properties'.
 */
public interface ShellMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "Flag".
   * 
   * @return translated "Flag"
   * @gwt.key tagFlag
   */
  String tagFlag();

  /**
   * Translated "Welcome to Whirled! Register here to get your very own house, to join groups and to keep track of your friends.".
   * 
   * @return translated "Welcome to Whirled! Register here to get your very own house, to join groups and to keep track of your friends."
   * @gwt.key createIntro
   */
  String createIntro();

  /**
   * Translated "Display name:".
   * 
   * @return translated "Display name:"
   * @gwt.key createDisplayName
   */
  String createDisplayName();

  /**
   * Translated "Don''t ever tell anyone your password, not even us! Only enter your password into the box used to logon, nowhere else.".
   * 
   * @return translated "Don''t ever tell anyone your password, not even us! Only enter your password into the box used to logon, nowhere else."
   * @gwt.key createPasswordTip
   */
  String createPasswordTip();

  /**
   * Translated "Please enter a display name.".
   * 
   * @return translated "Please enter a display name."
   * @gwt.key createMissingName
   */
  String createMissingName();

  /**
   * Translated "Your Permaname has been configured. Note: you must use your Permaname to log into the Forums and the Wiki. We plan to fix this in the future.".
   * 
   * @return translated "Your Permaname has been configured. Note: you must use your Permaname to log into the Forums and the Wiki. We plan to fix this in the future."
   * @gwt.key permaNameConfigured
   */
  String permaNameConfigured();

  /**
   * Translated "Games".
   * 
   * @return translated "Games"
   * @gwt.key menuGames
   */
  String menuGames();

  /**
   * Translated "Re-enter your password to confirm it.".
   * 
   * @return translated "Re-enter your password to confirm it."
   * @gwt.key createMissingConfirm
   */
  String createMissingConfirm();

  /**
   * Translated "Too many addresses ({0}): you have {1} invitations available".
   * 
   * @return translated "Too many addresses ({0}): you have {1} invitations available"
   * @gwt.key sendInvitesTooMany
   */
  String sendInvitesTooMany(String arg0,  String arg1);

  /**
   * Translated "Copyright Violation".
   * 
   * @return translated "Copyright Violation"
   * @gwt.key tagCopyrightFlag
   */
  String tagCopyrightFlag();

  /**
   * Translated "Reenter your password to confirm it.".
   * 
   * @return translated "Reenter your password to confirm it."
   * @gwt.key editMissingConfirm
   */
  String editMissingConfirm();

  /**
   * Translated "People".
   * 
   * @return translated "People"
   * @gwt.key menuPeople
   */
  String menuPeople();

  /**
   * Translated "Here you can update your account information.".
   * 
   * @return translated "Here you can update your account information."
   * @gwt.key editTip
   */
  String editTip();

  /**
   * Translated "You have no invitations available to send.".
   * 
   * @return translated "You have no invitations available to send."
   * @gwt.key sendInvitesNoneAvailable
   */
  String sendInvitesNoneAvailable();

  /**
   * Translated "{0} addresses already invited by you: ".
   * 
   * @return translated "{0} addresses already invited by you: "
   * @gwt.key sendInvitesResultsAlreadyInvited
   */
  String sendInvitesResultsAlreadyInvited(String arg0);

  /**
   * Translated "Duplicate email address found: {0}".
   * 
   * @return translated "Duplicate email address found: {0}"
   * @gwt.key sendInvitesDuplicateAddress
   */
  String sendInvitesDuplicateAddress(String arg0);

  /**
   * Translated "The passwords you''ve entered do not yet match.".
   * 
   * @return translated "The passwords you''ve entered do not yet match."
   * @gwt.key editPasswordMismatch
   */
  String editPasswordMismatch();

  /**
   * Translated "Email address:".
   * 
   * @return translated "Email address:"
   * @gwt.key createEmail
   */
  String createEmail();

  /**
   * Translated "Please logon above to access the Whirled.".
   * 
   * @return translated "Please logon above to access the Whirled."
   * @gwt.key noGuests
   */
  String noGuests();

  /**
   * Translated "Send Invitations - You have {0} invitations available".
   * 
   * @return translated "Send Invitations - You have {0} invitations available"
   * @gwt.key sendInvitesSendHeader
   */
  String sendInvitesSendHeader(String arg0);

  /**
   * Translated "Cancel".
   * 
   * @return translated "Cancel"
   * @gwt.key cancel
   */
  String cancel();

  /**
   * Translated "The passwords you''ve entered do not match.".
   * 
   * @return translated "The passwords you''ve entered do not match."
   * @gwt.key createPasswordMismatch
   */
  String createPasswordMismatch();

  /**
   * Translated "Please enter at least 1 address to send an invitation".
   * 
   * @return translated "Please enter at least 1 address to send an invitation"
   * @gwt.key sendInvitesEnterAddresses
   */
  String sendInvitesEnterAddresses();

  /**
   * Translated "Permaname:".
   * 
   * @return translated "Permaname:"
   * @gwt.key editPermaName
   */
  String editPermaName();

  /**
   * Translated "Enter a password for your account.".
   * 
   * @return translated "Enter a password for your account."
   * @gwt.key createMissingPassword
   */
  String createMissingPassword();

  /**
   * Translated "Your Permaname cannot be more than twelve characters long.".
   * 
   * @return translated "Your Permaname cannot be more than twelve characters long."
   * @gwt.key editPermaLong
   */
  String editPermaLong();

  /**
   * Translated "<b>Note:</b> your Permaname can never be changed once it is set.<br> You must create a Permaname to log into the Wiki and the Forums.".
   * 
   * @return translated "<b>Note:</b> your Permaname can never be changed once it is set.<br> You must create a Permaname to log into the Wiki and the Forums."
   * @gwt.key editPermaNameTip
   */
  String editPermaNameTip();

  /**
   * Translated "Your Permaname can only contain letters, numbers and underscore and must start with a letter.".
   * 
   * @return translated "Your Permaname can only contain letters, numbers and underscore and must start with a letter."
   * @gwt.key editPermaInvalid
   */
  String editPermaInvalid();

  /**
   * Translated "Stuff".
   * 
   * @return translated "Stuff"
   * @gwt.key menuStuff
   */
  String menuStuff();

  /**
   * Translated "Enter your email address.".
   * 
   * @return translated "Enter your email address."
   * @gwt.key createMissingEmail
   */
  String createMissingEmail();

  /**
   * Translated "Dismiss".
   * 
   * @return translated "Dismiss"
   * @gwt.key dismiss
   */
  String dismiss();

  /**
   * Translated "Edit Account Information".
   * 
   * @return translated "Edit Account Information"
   * @gwt.key editTitle
   */
  String editTitle();

  /**
   * Translated "Whirled - {0}".
   * 
   * @return translated "Whirled - {0}"
   * @gwt.key windowTitle
   */
  String windowTitle(String arg0);

  /**
   * Translated "Update Email Address".
   * 
   * @return translated "Update Email Address"
   * @gwt.key editEmailHeader
   */
  String editEmailHeader();

  /**
   * Translated "Are you sure you would like to remove the tag {0}?".
   * 
   * @return translated "Are you sure you would like to remove the tag {0}?"
   * @gwt.key tagRemoveConfirm
   */
  String tagRemoveConfirm(String arg0);

  /**
   * Translated "{0} invitations sent successfully: ".
   * 
   * @return translated "{0} invitations sent successfully: "
   * @gwt.key sendInvitesResultsSuccessful
   */
  String sendInvitesResultsSuccessful(String arg0);

  /**
   * Translated "Email address:".
   * 
   * @return translated "Email address:"
   * @gwt.key editEmail
   */
  String editEmail();

  /**
   * Translated "New password:".
   * 
   * @return translated "New password:"
   * @gwt.key editPassword
   */
  String editPassword();

  /**
   * Translated "Create a New Account".
   * 
   * @return translated "Create a New Account"
   * @gwt.key createTitle
   */
  String createTitle();

  /**
   * Translated "Quick add:".
   * 
   * @return translated "Quick add:"
   * @gwt.key tagQuickAdd
   */
  String tagQuickAdd();

  /**
   * Translated "Click "Let''s Go" below to create your account!".
   * 
   * @return translated "Click "Let''s Go" below to create your account!"
   * @gwt.key createReady
   */
  String createReady();

  /**
   * Translated "Greetings!
   * 
   * You are cordially invited to participate in the closed alpha test of Whirled,
   * Three Rings' exciting new online social world for chat, games and
   * player-created content.".
   * 
   * @return translated "Greetings!
   * 
   * You are cordially invited to participate in the closed alpha test of Whirled,
   * Three Rings' exciting new online social world for chat, games and
   * player-created content."
   * @gwt.key sendInvitesCustomDefault
   */
  String sendInvitesCustomDefault();

  /**
   * Translated "Logging on...".
   * 
   * @return translated "Logging on..."
   * @gwt.key loggingOn
   */
  String loggingOn();

  /**
   * Translated "Let''s Go!".
   * 
   * @return translated "Let''s Go!"
   * @gwt.key createCreate
   */
  String createCreate();

  /**
   * Translated "You currently have no pending invitations.".
   * 
   * @return translated "You currently have no pending invitations."
   * @gwt.key sendInvitesNoPending
   */
  String sendInvitesNoPending();

  /**
   * Translated "Refresh".
   * 
   * @return translated "Refresh"
   * @gwt.key refresh
   */
  String refresh();

  /**
   * Translated "Update".
   * 
   * @return translated "Update"
   * @gwt.key update
   */
  String update();

  /**
   * Translated "Select one...".
   * 
   * @return translated "Select one..."
   * @gwt.key tagSelectOne
   */
  String tagSelectOne();

  /**
   * Translated "Remove Tag".
   * 
   * @return translated "Remove Tag"
   * @gwt.key tagRemove
   */
  String tagRemove();

  /**
   * Translated "Pending Invitations".
   * 
   * @return translated "Pending Invitations"
   * @gwt.key sendInvitesPendingHeader
   */
  String sendInvitesPendingHeader();

  /**
   * Translated "{0} addresses already registered: ".
   * 
   * @return translated "{0} addresses already registered: "
   * @gwt.key sendInvitesResultsAlreadyRegistered
   */
  String sendInvitesResultsAlreadyRegistered(String arg0);

  /**
   * Translated "Email Addresses:".
   * 
   * @return translated "Email Addresses:"
   * @gwt.key sendInvitesEmailAddresses
   */
  String sendInvitesEmailAddresses();

  /**
   * Translated "Flag".
   * 
   * @return translated "Flag"
   * @gwt.key tagFlagFlagButton
   */
  String tagFlagFlagButton();

  /**
   * Translated "Add Tag:".
   * 
   * @return translated "Add Tag:"
   * @gwt.key tagAddTag
   */
  String tagAddTag();

  /**
   * Translated "Confirm:".
   * 
   * @return translated "Confirm:"
   * @gwt.key createConfirm
   */
  String createConfirm();

  /**
   * Translated "Mature".
   * 
   * @return translated "Mature"
   * @gwt.key tagMatureFlag
   */
  String tagMatureFlag();

  /**
   * Translated "Send Invitations to Whirled".
   * 
   * @return translated "Send Invitations to Whirled"
   * @gwt.key sendInvitesTitle
   */
  String sendInvitesTitle();

  /**
   * Translated "Click ''Submit'' to configure your Permaname.".
   * 
   * @return translated "Click ''Submit'' to configure your Permaname."
   * @gwt.key editPermaReady
   */
  String editPermaReady();

  /**
   * Translated "Your Permaname must be at least four characters long.".
   * 
   * @return translated "Your Permaname must be at least four characters long."
   * @gwt.key editPermaShort
   */
  String editPermaShort();

  /**
   * Translated "Update Password".
   * 
   * @return translated "Update Password"
   * @gwt.key editPasswordHeader
   */
  String editPasswordHeader();

  /**
   * Translated "You''ll use your email address to logon.".
   * 
   * @return translated "You''ll use your email address to logon."
   * @gwt.key createEmailTip
   */
  String createEmailTip();

  /**
   * Translated "Email Address:".
   * 
   * @return translated "Email Address:"
   * @gwt.key logonEmail
   */
  String logonEmail();

  /**
   * Translated "Choose a Photo from your Inventory:".
   * 
   * @return translated "Choose a Photo from your Inventory:"
   * @gwt.key pickImage
   */
  String pickImage();

  /**
   * Translated "Invalid Tag: can''t be more than 24 characters.".
   * 
   * @return translated "Invalid Tag: can''t be more than 24 characters."
   * @gwt.key errTagTooLong
   */
  String errTagTooLong();

  /**
   * Translated "Submit".
   * 
   * @return translated "Submit"
   * @gwt.key submit
   */
  String submit();

  /**
   * Translated "Click ''Update'' to update your email address.".
   * 
   * @return translated "Click ''Update'' to update your email address."
   * @gwt.key editEmailReady
   */
  String editEmailReady();

  /**
   * Translated "Email address updated. Remember, this new address must be used next time you log into Whirled.".
   * 
   * @return translated "Email address updated. Remember, this new address must be used next time you log into Whirled."
   * @gwt.key emailUpdated
   */
  String emailUpdated();

  /**
   * Translated "You have already sent invitations to these email addresses.".
   * 
   * @return translated "You have already sent invitations to these email addresses."
   * @gwt.key sendInvitesPendingTip
   */
  String sendInvitesPendingTip();

  /**
   * Translated "Enter up to {0} email address below (each on a new line), along with a custom message to invite your friends to Whirled!".
   * 
   * @return translated "Enter up to {0} email address below (each on a new line), along with a custom message to invite your friends to Whirled!"
   * @gwt.key sendInvitesSendTip
   */
  String sendInvitesSendTip(String arg0);

  /**
   * Translated "{0}...".
   * 
   * @return translated "{0}..."
   * @gwt.key truncName
   */
  String truncName(String arg0);

  /**
   * Translated "No description provided for this item.".
   * 
   * @return translated "No description provided for this item."
   * @gwt.key noDescrip
   */
  String noDescrip();

  /**
   * Translated "{0} addresses are invalid: ".
   * 
   * @return translated "{0} addresses are invalid: "
   * @gwt.key sendInvitesResultsInvalid
   */
  String sendInvitesResultsInvalid(String arg0);

  /**
   * Translated "Password:".
   * 
   * @return translated "Password:"
   * @gwt.key createPassword
   */
  String createPassword();

  /**
   * Translated "Your password has been updated.".
   * 
   * @return translated "Your password has been updated."
   * @gwt.key passwordUpdated
   */
  String passwordUpdated();

  /**
   * Translated "Confirm:".
   * 
   * @return translated "Confirm:"
   * @gwt.key editConfirm
   */
  String editConfirm();

  /**
   * Translated "Logon".
   * 
   * @return translated "Logon"
   * @gwt.key menuLogon
   */
  String menuLogon();

  /**
   * Translated "Send Invites".
   * 
   * @return translated "Send Invites"
   * @gwt.key sendInvitesSendEmail
   */
  String sendInvitesSendEmail();

  /**
   * Translated "{0} invitations failed to send: ".
   * 
   * @return translated "{0} invitations failed to send: "
   * @gwt.key sendInvitesResultsFailed
   */
  String sendInvitesResultsFailed(String arg0);

  /**
   * Translated "Invalid email addresses found: {0}".
   * 
   * @return translated "Invalid email addresses found: {0}"
   * @gwt.key sendInvitesInvalidAddress
   */
  String sendInvitesInvalidAddress(String arg0);

  /**
   * Translated "Custom Message:".
   * 
   * @return translated "Custom Message:"
   * @gwt.key sendInvitesCustomMessage
   */
  String sendInvitesCustomMessage();

  /**
   * Translated "Pick Your Permaname".
   * 
   * @return translated "Pick Your Permaname"
   * @gwt.key editPickPermaNameHeader
   */
  String editPickPermaNameHeader();

  /**
   * Translated "Password:".
   * 
   * @return translated "Password:"
   * @gwt.key logonPassword
   */
  String logonPassword();

  /**
   * Translated "Play!".
   * 
   * @return translated "Play!"
   * @gwt.key detailPlay
   */
  String detailPlay();

  /**
   * Translated "Creating account...".
   * 
   * @return translated "Creating account..."
   * @gwt.key creatingAccount
   */
  String creatingAccount();

  /**
   * Translated "Send Invitation Results".
   * 
   * @return translated "Send Invitation Results"
   * @gwt.key sendInvitesResults
   */
  String sendInvitesResults();

  /**
   * Translated "Invalid tag: use letters, numbers, and underscore.".
   * 
   * @return translated "Invalid tag: use letters, numbers, and underscore."
   * @gwt.key errTagInvalidCharacters
   */
  String errTagInvalidCharacters();

  /**
   * Translated "<no name>".
   * 
   * @return translated "<no name>"
   * @gwt.key noName
   */
  String noName();

  /**
   * Translated "Click ''Update'' to update your password.".
   * 
   * @return translated "Click ''Update'' to update your password."
   * @gwt.key editPasswordReady
   */
  String editPasswordReady();

  /**
   * Translated "Places".
   * 
   * @return translated "Places"
   * @gwt.key menuPlaces
   */
  String menuPlaces();

  /**
   * Translated "You have no Photos in your Inventory. Select the ''Stuff'' -> ''Inventory'' from the menu at the top of the page and upload some Photos!".
   * 
   * @return translated "You have no Photos in your Inventory. Select the ''Stuff'' -> ''Inventory'' from the menu at the top of the page and upload some Photos!"
   * @gwt.key haveNoImages
   */
  String haveNoImages();

  /**
   * Translated "Your display name will be shown to other users in Whirled.".
   * 
   * @return translated "Your display name will be shown to other users in Whirled."
   * @gwt.key createDisplayNameTip
   */
  String createDisplayNameTip();

  /**
   * Translated "Me".
   * 
   * @return translated "Me"
   * @gwt.key menuMe
   */
  String menuMe();

  /**
   * Translated "Cancel".
   * 
   * @return translated "Cancel"
   * @gwt.key tagFlagCancelButton
   */
  String tagFlagCancelButton();

  /**
   * Translated "Flag item as {0}?".
   * 
   * @return translated "Flag item as {0}?"
   * @gwt.key tagFlagPrompt
   */
  String tagFlagPrompt(String arg0);

  /**
   * Translated "Loading...".
   * 
   * @return translated "Loading..."
   * @gwt.key tagLoading
   */
  String tagLoading();
}
