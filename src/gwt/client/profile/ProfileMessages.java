package client.profile;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/profile/ProfileMessages.properties'.
 */
public interface ProfileMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "Headline".
   * 
   * @return translated "Headline"
   * @gwt.key headline
   */
  String headline();

  /**
   * Translated "Game Trophies".
   * 
   * @return translated "Game Trophies"
   * @gwt.key trophiesTitle
   */
  String trophiesTitle();

  /**
   * Translated "Permaname: {0}".
   * 
   * @return translated "Permaname: {0}"
   * @gwt.key permaName
   */
  String permaName(String arg0);

  /**
   * Translated "That person could not be found.".
   * 
   * @return translated "That person could not be found."
   * @gwt.key friendsNoSuchMember
   */
  String friendsNoSuchMember();

  /**
   * Translated "Real Name".
   * 
   * @return translated "Real Name"
   * @gwt.key searchRadioName
   */
  String searchRadioName();

  /**
   * Translated "Email".
   * 
   * @return translated "Email"
   * @gwt.key searchRadioEmail
   */
  String searchRadioEmail();

  /**
   * Translated "Let''s be buddies!".
   * 
   * @return translated "Let''s be buddies!"
   * @gwt.key inviteBody
   */
  String inviteBody();

  /**
   * Translated "Search Type:".
   * 
   * @return translated "Search Type:"
   * @gwt.key searchType
   */
  String searchType();

  /**
   * Translated "No profiles were found matching your search criteria.".
   * 
   * @return translated "No profiles were found matching your search criteria."
   * @gwt.key gridNoProfiles
   */
  String gridNoProfiles();

  /**
   * Translated "This person has no friends. How sad.".
   * 
   * @return translated "This person has no friends. How sad."
   * @gwt.key noFriendsOther
   */
  String noFriendsOther();

  /**
   * Translated "Send Mail".
   * 
   * @return translated "Send Mail"
   * @gwt.key sendMail
   */
  String sendMail();

  /**
   * Translated ""{0}" has been removed from your friends list.".
   * 
   * @return translated ""{0}" has been removed from your friends list."
   * @gwt.key friendsRemoved
   */
  String friendsRemoved(String arg0);

  /**
   * Translated "You''re not a member of any groups. Boo hoo.".
   * 
   * @return translated "You''re not a member of any groups. Boo hoo."
   * @gwt.key notInGroupsSelf
   */
  String notInGroupsSelf();

  /**
   * Translated "Sorry pardner. You don''t have invite privileges for any groups.".
   * 
   * @return translated "Sorry pardner. You don''t have invite privileges for any groups."
   * @gwt.key haveNoGroups
   */
  String haveNoGroups();

  /**
   * Translated "Show Homepage".
   * 
   * @return translated "Show Homepage"
   * @gwt.key showHomepage
   */
  String showHomepage();

  /**
   * Translated "Profile Search".
   * 
   * @return translated "Profile Search"
   * @gwt.key profileSearchTitle
   */
  String profileSearchTitle();

  /**
   * Translated "Find someone:".
   * 
   * @return translated "Find someone:"
   * @gwt.key search
   */
  String search();

  /**
   * Translated "Error".
   * 
   * @return translated "Error"
   * @gwt.key errorTitle
   */
  String errorTitle();

  /**
   * Translated "Be my Friend".
   * 
   * @return translated "Be my Friend"
   * @gwt.key inviteTitle
   */
  String inviteTitle();

  /**
   * Translated "Friends".
   * 
   * @return translated "Friends"
   * @gwt.key friendsTitle
   */
  String friendsTitle();

  /**
   * Translated "Invite To Be Your Friend".
   * 
   * @return translated "Invite To Be Your Friend"
   * @gwt.key inviteFriend
   */
  String inviteFriend();

  /**
   * Translated "Search".
   * 
   * @return translated "Search"
   * @gwt.key searchGo
   */
  String searchGo();

  /**
   * Translated "Loading friends...".
   * 
   * @return translated "Loading friends..."
   * @gwt.key friendsLoading
   */
  String friendsLoading();

  /**
   * Translated "Visit Home".
   * 
   * @return translated "Visit Home"
   * @gwt.key visitHome
   */
  String visitHome();

  /**
   * Translated "Missing profile details.".
   * 
   * @return translated "Missing profile details."
   * @gwt.key profileLoadFailed
   */
  String profileLoadFailed();

  /**
   * Translated "Your display name must be between {0} and {1} characters long.".
   * 
   * @return translated "Your display name must be between {0} and {1} characters long."
   * @gwt.key displayNameInvalid
   */
  String displayNameInvalid(String arg0,  String arg1);

  /**
   * Translated "Please log in to view your profile.".
   * 
   * @return translated "Please log in to view your profile."
   * @gwt.key profileLogin
   */
  String profileLogin();

  /**
   * Translated "Admin Info".
   * 
   * @return translated "Admin Info"
   * @gwt.key adminBrowse
   */
  String adminBrowse();

  /**
   * Translated "Log in to view your profile.".
   * 
   * @return translated "Log in to view your profile."
   * @gwt.key indexLogon
   */
  String indexLogon();

  /**
   * Translated "Invite to Join Group".
   * 
   * @return translated "Invite to Join Group"
   * @gwt.key inviteToGroup
   */
  String inviteToGroup();

  /**
   * Translated "Find Friends".
   * 
   * @return translated "Find Friends"
   * @gwt.key findFriends
   */
  String findFriends();

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
   * Translated "(single player)".
   * 
   * @return translated "(single player)"
   * @gwt.key ratingsSingle
   */
  String ratingsSingle();

  /**
   * Translated "About Me".
   * 
   * @return translated "About Me"
   * @gwt.key profileTitle
   */
  String profileTitle();

  /**
   * Translated "Are you sure you want to remove "{0}" from your friends list?".
   * 
   * @return translated "Are you sure you want to remove "{0}" from your friends list?"
   * @gwt.key friendsRemoveConfirm
   */
  String friendsRemoveConfirm(String arg0);

  /**
   * Translated "Last online: {0}".
   * 
   * @return translated "Last online: {0}"
   * @gwt.key lastOnline
   */
  String lastOnline(String arg0);

  /**
   * Translated "[remove]".
   * 
   * @return translated "[remove]"
   * @gwt.key friendsRemove
   */
  String friendsRemove();

  /**
   * Translated "See All".
   * 
   * @return translated "See All"
   * @gwt.key seeAll
   */
  String seeAll();

  /**
   * Translated "This person is not a member of any groups.".
   * 
   * @return translated "This person is not a member of any groups."
   * @gwt.key notInGroupsOther
   */
  String notInGroupsOther();

  /**
   * Translated "Display Name".
   * 
   * @return translated "Display Name"
   * @gwt.key displayName
   */
  String displayName();

  /**
   * Translated "You have no friends. Boo hoo.".
   * 
   * @return translated "You have no friends. Boo hoo."
   * @gwt.key noFriendsSelf
   */
  String noFriendsSelf();

  /**
   * Translated "Game Ratings".
   * 
   * @return translated "Game Ratings"
   * @gwt.key ratingsTitle
   */
  String ratingsTitle();

  /**
   * Translated "Display Name".
   * 
   * @return translated "Display Name"
   * @gwt.key searchRadioDisplayName
   */
  String searchRadioDisplayName();
}
