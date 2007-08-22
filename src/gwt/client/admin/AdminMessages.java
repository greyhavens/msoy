package client.admin;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/admin/AdminMessages.properties'.
 */
public interface AdminMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated ""Active Users" is defined as all users that have logged on within the last week.".
   * 
   * @return translated ""Active Users" is defined as all users that have logged on within the last week."
   * @gwt.key activeUsersTip
   */
  String activeUsersTip();

  /**
   * Translated "Unable to find flagged items to display: {0}".
   * 
   * @return translated "Unable to find flagged items to display: {0}"
   * @gwt.key reviewErrFlaggedItems
   */
  String reviewErrFlaggedItems(String arg0);

  /**
   * Translated "Admin Console".
   * 
   * @return translated "Admin Console"
   * @gwt.key title
   */
  String title();

  /**
   * Translated "The item has been delisted.".
   * 
   * @return translated "The item has been delisted."
   * @gwt.key reviewDelisted
   */
  String reviewDelisted();

  /**
   * Translated "The item has been marked as mature.".
   * 
   * @return translated "The item has been marked as mature."
   * @gwt.key reviewMarked
   */
  String reviewMarked();

  /**
   * Translated "{0} additional invites have been granted to {1}.".
   * 
   * @return translated "{0} additional invites have been granted to {1}."
   * @gwt.key browserAddInvites
   */
  String browserAddInvites(String arg0,  String arg1);

  /**
   * Translated "Delete".
   * 
   * @return translated "Delete"
   * @gwt.key reviewDelete
   */
  String reviewDelete();

  /**
   * Translated "Invitation functions:".
   * 
   * @return translated "Invitation functions:"
   * @gwt.key inviteControls
   */
  String inviteControls();

  /**
   * Translated "Used".
   * 
   * @return translated "Used"
   * @gwt.key browserUsed
   */
  String browserUsed();

  /**
   * Translated "Reload".
   * 
   * @return translated "Reload"
   * @gwt.key reviewReload
   */
  String reviewReload();

  /**
   * Translated "Delist".
   * 
   * @return translated "Delist"
   * @gwt.key reviewDelist
   */
  String reviewDelist();

  /**
   * Translated "Issue To:".
   * 
   * @return translated "Issue To:"
   * @gwt.key invitesIssueSelection
   */
  String invitesIssueSelection();

  /**
   * Translated "Name".
   * 
   * @return translated "Name"
   * @gwt.key browserName
   */
  String browserName();

  /**
   * Translated "Done".
   * 
   * @return translated "Done"
   * @gwt.key reviewDone
   */
  String reviewDone();

  /**
   * Translated "Players invited by {0}".
   * 
   * @return translated "Players invited by {0}"
   * @gwt.key browserInvitedBy
   */
  String browserInvitedBy(String arg0);

  /**
   * Translated "Your account does not have the necessary privileges to view this page.".
   * 
   * @return translated "Your account does not have the necessary privileges to view this page."
   * @gwt.key lackPrivileges
   */
  String lackPrivileges();

  /**
   * Translated "Display Dashboard".
   * 
   * @return translated "Display Dashboard"
   * @gwt.key displayDashboard
   */
  String displayDashboard();

  /**
   * Translated "Issue Invites to Players".
   * 
   * @return translated "Issue Invites to Players"
   * @gwt.key issueInvites
   */
  String issueInvites();

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
   * Translated "Number of Invitations:".
   * 
   * @return translated "Number of Invitations:"
   * @gwt.key invitesNumber
   */
  String invitesNumber();

  /**
   * Translated "Please logon above to access Admin services.".
   * 
   * @return translated "Please logon above to access Admin services."
   * @gwt.key indexLogon
   */
  String indexLogon();

  /**
   * Translated "{0} have been issued {1} invites.".
   * 
   * @return translated "{0} have been issued {1} invites."
   * @gwt.key invitesSuccess
   */
  String invitesSuccess(String arg0,  String arg1);

  /**
   * Translated "Invitations".
   * 
   * @return translated "Invitations"
   * @gwt.key browserInvites
   */
  String browserInvites();

  /**
   * Translated "To delete this item, enter a message to be sent to the item''s creator:".
   * 
   * @return translated "To delete this item, enter a message to be sent to the item''s creator:"
   * @gwt.key reviewDeletionPrompt
   */
  String reviewDeletionPrompt();

  /**
   * Translated "Active Users".
   * 
   * @return translated "Active Users"
   * @gwt.key invitesToActive
   */
  String invitesToActive();

  /**
   * Translated "Successfully deleted {0} item(s) and notified owner(s).".
   * 
   * @return translated "Successfully deleted {0} item(s) and notified owner(s)."
   * @gwt.key reviewDeletionSuccess
   */
  String reviewDeletionSuccess(String arg0);

  /**
   * Translated "The item "{0}" has been deleted by the game administrators: {1}".
   * 
   * @return translated "The item "{0}" has been deleted by the game administrators: {1}"
   * @gwt.key reviewDeletionMailMessage
   */
  String reviewDeletionMailMessage(String arg0,  String arg1);

  /**
   * Translated "There are no flagged items to review.".
   * 
   * @return translated "There are no flagged items to review."
   * @gwt.key reviewNoItems
   */
  String reviewNoItems();

  /**
   * Translated "Available".
   * 
   * @return translated "Available"
   * @gwt.key browserAvailable
   */
  String browserAvailable();

  /**
   * Translated "Delete All".
   * 
   * @return translated "Delete All"
   * @gwt.key reviewDeleteAll
   */
  String reviewDeleteAll();

  /**
   * Translated "All Users".
   * 
   * @return translated "All Users"
   * @gwt.key invitesToAll
   */
  String invitesToAll();

  /**
   * Translated "Flagged Items".
   * 
   * @return translated "Flagged Items"
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
   * Translated "Total".
   * 
   * @return translated "Total"
   * @gwt.key browserTotal
   */
  String browserTotal();

  /**
   * Translated "Failed to delete messages and/or notify owners: {0}".
   * 
   * @return translated "Failed to delete messages and/or notify owners: {0}"
   * @gwt.key reviewErrDeletionFailed
   */
  String reviewErrDeletionFailed(String arg0);

  /**
   * Translated "Player Browser".
   * 
   * @return translated "Player Browser"
   * @gwt.key browserTitle
   */
  String browserTitle();

  /**
   * Translated "Admin functions:".
   * 
   * @return translated "Admin functions:"
   * @gwt.key adminControls
   */
  String adminControls();

  /**
   * Translated "Issue Invitations".
   * 
   * @return translated "Issue Invitations"
   * @gwt.key invitesIssueButton
   */
  String invitesIssueButton();

  /**
   * Translated "Players with no inviter".
   * 
   * @return translated "Players with no inviter"
   * @gwt.key browserNoInviter
   */
  String browserNoInviter();

  /**
   * Translated "Cancel".
   * 
   * @return translated "Cancel"
   * @gwt.key reviewDeletionDont
   */
  String reviewDeletionDont();

  /**
   * Translated "Issue Invitations".
   * 
   * @return translated "Issue Invitations"
   * @gwt.key invitesTitle
   */
  String invitesTitle();

  /**
   * Translated "Edit".
   * 
   * @return translated "Edit"
   * @gwt.key itemPopupEdit
   */
  String itemPopupEdit();

  /**
   * Translated "Delete".
   * 
   * @return translated "Delete"
   * @gwt.key reviewDeletionDo
   */
  String reviewDeletionDo();

  /**
   * Translated "Player Browser".
   * 
   * @return translated "Player Browser"
   * @gwt.key browserPlayers
   */
  String browserPlayers();
}
