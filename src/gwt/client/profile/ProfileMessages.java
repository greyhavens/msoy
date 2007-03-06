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
   * Translated "(pending their acceptance)".
   * 
   * @return translated "(pending their acceptance)"
   * @gwt.key pendingThem
   */
  String pendingThem();

  /**
   * Translated "Last online: {0}".
   * 
   * @return translated "Last online: {0}"
   * @gwt.key lastOnline
   */
  String lastOnline(String arg0);

  /**
   * Translated "Your display name must be between {0} and {1} characters long.".
   * 
   * @return translated "Your display name must be between {0} and {1} characters long."
   * @gwt.key displayNameInvalid
   */
  String displayNameInvalid(String arg0,  String arg1);

  /**
   * Translated "Home Page".
   * 
   * @return translated "Home Page"
   * @gwt.key homepage
   */
  String homepage();

  /**
   * Translated "(pending your acceptance)".
   * 
   * @return translated "(pending your acceptance)"
   * @gwt.key pendingYou
   */
  String pendingYou();

  /**
   * Translated "Log in to view your profile.".
   * 
   * @return translated "Log in to view your profile."
   * @gwt.key indexLogon
   */
  String indexLogon();

  /**
   * Translated "Headline".
   * 
   * @return translated "Headline"
   * @gwt.key headline
   */
  String headline();

  /**
   * Translated "Display Name".
   * 
   * @return translated "Display Name"
   * @gwt.key displayName
   */
  String displayName();
}
