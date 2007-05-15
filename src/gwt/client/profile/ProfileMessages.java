package client.profile;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/profile/ProfileMessages.properties'.
 */
public interface ProfileMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "Find someone:".
   * 
   * @return translated "Find someone:"
   * @gwt.key search
   */
  String search();

  /**
   * Translated "Last online: {0}".
   * 
   * @return translated "Last online: {0}"
   * @gwt.key lastOnline
   */
  String lastOnline(String arg0);

  /**
   * Translated "Groups".
   * 
   * @return translated "Groups"
   * @gwt.key groupsTitle
   */
  String groupsTitle();

  /**
   * Translated "Home Page".
   * 
   * @return translated "Home Page"
   * @gwt.key homepage
   */
  String homepage();

  /**
   * Translated "Sorry parder. You don't have invite privileges for any groups.".
   * 
   * @return translated "Sorry parder. You don't have invite privileges for any groups."
   * @gwt.key haveNoGroups
   */
  String haveNoGroups();

  /**
   * Translated "You're not a member of any groups. Boo hoo.".
   * 
   * @return translated "You're not a member of any groups. Boo hoo."
   * @gwt.key notInGroupsSelf
   */
  String notInGroupsSelf();

  /**
   * Translated "Invite to Join Group".
   * 
   * @return translated "Invite to Join Group"
   * @gwt.key inviteToGroup
   */
  String inviteToGroup();

  /**
   * Translated "Permaname: {0}".
   * 
   * @return translated "Permaname: {0}"
   * @gwt.key permaName
   */
  String permaName(String arg0);

  /**
   * Translated "Your display name must be between {0} and {1} characters long.".
   * 
   * @return translated "Your display name must be between {0} and {1} characters long."
   * @gwt.key displayNameInvalid
   */
  String displayNameInvalid(String arg0,  String arg1);

  /**
   * Translated "This person is not a member of any groups.".
   * 
   * @return translated "This person is not a member of any groups."
   * @gwt.key notInGroupsOther
   */
  String notInGroupsOther();

  /**
   * Translated "Error".
   * 
   * @return translated "Error"
   * @gwt.key errorTitle
   */
  String errorTitle();

  /**
   * Translated "Profile".
   * 
   * @return translated "Profile"
   * @gwt.key profileTitle
   */
  String profileTitle();

  /**
   * Translated "Log in to view your profile.".
   * 
   * @return translated "Log in to view your profile."
   * @gwt.key indexLogon
   */
  String indexLogon();

  /**
   * Translated "Display Name".
   * 
   * @return translated "Display Name"
   * @gwt.key displayName
   */
  String displayName();

  /**
   * Translated "Headline".
   * 
   * @return translated "Headline"
   * @gwt.key headline
   */
  String headline();
}
