package client.admin;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/admin/AdminMessages.properties'.
 */
public interface AdminMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "Unable to find flagged items to display: {0}".
   * 
   * @return translated "Unable to find flagged items to display: {0}"
   * @gwt.key reviewErrFlaggedItems
   */
  String reviewErrFlaggedItems(String arg0);

  /**
   * Translated "The item has been delisted.".
   * 
   * @return translated "The item has been delisted."
   * @gwt.key reviewDelisted
   */
  String reviewDelisted();

  /**
   * Translated "Admin Console".
   * 
   * @return translated "Admin Console"
   * @gwt.key title
   */
  String title();

  /**
   * Translated "The item has been marked as mature.".
   * 
   * @return translated "The item has been marked as mature."
   * @gwt.key reviewMarked
   */
  String reviewMarked();

  /**
   * Translated "Delete".
   * 
   * @return translated "Delete"
   * @gwt.key reviewDelete
   */
  String reviewDelete();

  /**
   * Translated "Enter the email addresses you would like to invite. Accounts will be created for these addresses and an invitation email will be sent.".
   * 
   * @return translated "Enter the email addresses you would like to invite. Accounts will be created for these addresses and an invitation email will be sent."
   * @gwt.key inviteTip
   */
  String inviteTip();

  /**
   * Translated "Delist".
   * 
   * @return translated "Delist"
   * @gwt.key reviewDelist
   */
  String reviewDelist();

  /**
   * Translated "Reload".
   * 
   * @return translated "Reload"
   * @gwt.key reviewReload
   */
  String reviewReload();

  /**
   * Translated "Sent!".
   * 
   * @return translated "Sent!"
   * @gwt.key inviteSent
   */
  String inviteSent();

  /**
   * Translated "Done".
   * 
   * @return translated "Done"
   * @gwt.key reviewDone
   */
  String reviewDone();

  /**
   * Translated "Invite Players".
   * 
   * @return translated "Invite Players"
   * @gwt.key invitePlayers
   */
  String invitePlayers();

  /**
   * Translated "Your account does not have the necessary privileges to view this page.".
   * 
   * @return translated "Your account does not have the necessary privileges to view this page."
   * @gwt.key lackPrivileges
   */
  String lackPrivileges();

  /**
   * Translated "Invite Players".
   * 
   * @return translated "Invite Players"
   * @gwt.key inviteTitle
   */
  String inviteTitle();

  /**
   * Translated "Display Dashboard".
   * 
   * @return translated "Display Dashboard"
   * @gwt.key displayDashboard
   */
  String displayDashboard();

  /**
   * Translated "Review Flagged Items".
   * 
   * @return translated "Review Flagged Items"
   * @gwt.key reviewButton
   */
  String reviewButton();

  /**
   * Translated "Item Deleted".
   * 
   * @return translated "Item Deleted"
   * @gwt.key reviewDeletionMailHeader
   */
  String reviewDeletionMailHeader();

  /**
   * Translated "Log in to access admin services.".
   * 
   * @return translated "Log in to access admin services."
   * @gwt.key indexLogon
   */
  String indexLogon();

  /**
   * Translated "Admin functions:".
   * 
   * @return translated "Admin functions:"
   * @gwt.key controls
   */
  String controls();

  /**
   * Translated "To delete this item, enter a message to be sent to the item''s creator:".
   * 
   * @return translated "To delete this item, enter a message to be sent to the item''s creator:"
   * @gwt.key reviewDeletionPrompt
   */
  String reviewDeletionPrompt();

  /**
   * Translated "Successfully deleted {0} item(s) and notified owner(s).".
   * 
   * @return translated "Successfully deleted {0} item(s) and notified owner(s)."
   * @gwt.key reviewDeletionSuccess
   */
  String reviewDeletionSuccess(String arg0);

  /**
   * Translated "There are no flagged items to review.".
   * 
   * @return translated "There are no flagged items to review."
   * @gwt.key reviewNoItems
   */
  String reviewNoItems();

  /**
   * Translated "The item "{0}" has been deleted by the game administrators: {1}".
   * 
   * @return translated "The item "{0}" has been deleted by the game administrators: {1}"
   * @gwt.key reviewDeletionMailMessage
   */
  String reviewDeletionMailMessage(String arg0,  String arg1);

  /**
   * Translated "Delete All".
   * 
   * @return translated "Delete All"
   * @gwt.key reviewDeleteAll
   */
  String reviewDeleteAll();

  /**
   * Translated "Review Flagged Items".
   * 
   * @return translated "Review Flagged Items"
   * @gwt.key reviewTitle
   */
  String reviewTitle();

  /**
   * Translated "Mark Mature".
   * 
   * @return translated "Mark Mature"
   * @gwt.key reviewMark
   */
  String reviewMark();

  /**
   * Translated "Failed to delete messages and/or notify owners: {0}".
   * 
   * @return translated "Failed to delete messages and/or notify owners: {0}"
   * @gwt.key reviewErrDeletionFailed
   */
  String reviewErrDeletionFailed(String arg0);

  /**
   * Translated "Dismiss".
   * 
   * @return translated "Dismiss"
   * @gwt.key reviewDismiss
   */
  String reviewDismiss();

  /**
   * Translated "Cancel".
   * 
   * @return translated "Cancel"
   * @gwt.key reviewDeletionDont
   */
  String reviewDeletionDont();

  /**
   * Translated "Edit".
   * 
   * @return translated "Edit"
   * @gwt.key itemPopupEdit
   */
  String itemPopupEdit();

  /**
   * Translated "Send ''em!".
   * 
   * @return translated "Send ''em!"
   * @gwt.key inviteSubmit
   */
  String inviteSubmit();

  /**
   * Translated "Delete".
   * 
   * @return translated "Delete"
   * @gwt.key reviewDeletionDo
   */
  String reviewDeletionDo();
}
