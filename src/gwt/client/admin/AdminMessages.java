package client.admin;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/admin/AdminMessages.properties'.
 */
public interface AdminMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "Your account does not have the necessary privileges to view this page.".
   * 
   * @return translated "Your account does not have the necessary privileges to view this page."
   * @gwt.key lackPrivileges
   */
  String lackPrivileges();

  /**
   * Translated "MetaSOY Dashboard".
   * 
   * @return translated "MetaSOY Dashboard"
   * @gwt.key title
   */
  String title();

  /**
   * Translated "Lots of useful features coming soon.".
   * 
   * @return translated "Lots of useful features coming soon."
   * @gwt.key todo
   */
  String todo();

  /**
   * Translated "Log in to access admin services.".
   * 
   * @return translated "Log in to access admin services."
   * @gwt.key indexLogon
   */
  String indexLogon();
}
