package client.admin;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/admin/AdminMessages.properties'.
 */
public interface AdminMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "There are no flagged items to review.".
   * 
   * @return translated "There are no flagged items to review."
   * @gwt.key reviewNoItems
   */
  String reviewNoItems();

  /**
   * Translated "MetaSOY Dashboard".
   * 
   * @return translated "MetaSOY Dashboard"
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
   * Translated "Delete All".
   * 
   * @return translated "Delete All"
   * @gwt.key reviewDeleteAll
   */
  String reviewDeleteAll();

  /**
   * Translated "Lots of useful features coming soon.".
   * 
   * @return translated "Lots of useful features coming soon."
   * @gwt.key todo
   */
  String todo();

  /**
   * Translated "The item has been marked as mature.".
   * 
   * @return translated "The item has been marked as mature."
   * @gwt.key reviewMarked
   */
  String reviewMarked();

  /**
   * Translated "Mark Mature".
   * 
   * @return translated "Mark Mature"
   * @gwt.key reviewMark
   */
  String reviewMark();

  /**
   * Translated "Delete".
   * 
   * @return translated "Delete"
   * @gwt.key reviewDelete
   */
  String reviewDelete();

  /**
   * Translated "Delist".
   * 
   * @return translated "Delist"
   * @gwt.key reviewDelist
   */
  String reviewDelist();

  /**
   * Translated "Done".
   * 
   * @return translated "Done"
   * @gwt.key reviewDone
   */
  String reviewDone();

  /**
   * Translated "Your account does not have the necessary privileges to view this page.".
   * 
   * @return translated "Your account does not have the necessary privileges to view this page."
   * @gwt.key lackPrivileges
   */
  String lackPrivileges();

  /**
   * Translated "Review flagged items".
   * 
   * @return translated "Review flagged items"
   * @gwt.key reviewButton
   */
  String reviewButton();

  /**
   * Translated "Unable to find catalog listing to delist.".
   * 
   * @return translated "Unable to find catalog listing to delist."
   * @gwt.key errListingNotFound
   */
  String errListingNotFound();

  /**
   * Translated "Log in to access admin services.".
   * 
   * @return translated "Log in to access admin services."
   * @gwt.key indexLogon
   */
  String indexLogon();
}
