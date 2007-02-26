package client.item;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/item/ItemMessages.properties'.
 */
public interface ItemMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "Search".
   * 
   * @return translated "Search"
   * @gwt.key searchSearch
   */
  String searchSearch();

  /**
   * Translated "No description provided for this item.".
   * 
   * @return translated "No description provided for this item."
   * @gwt.key noDescrip
   */
  String noDescrip();

  /**
   * Translated "(clear)".
   * 
   * @return translated "(clear)"
   * @gwt.key clearCurrentTag
   */
  String clearCurrentTag();

  /**
   * Translated "Current tag:".
   * 
   * @return translated "Current tag:"
   * @gwt.key currentTag
   */
  String currentTag();

  /**
   * Translated "<no name>".
   * 
   * @return translated "<no name>"
   * @gwt.key noName
   */
  String noName();

  /**
   * Translated "Sort by:".
   * 
   * @return translated "Sort by:"
   * @gwt.key searchSortBy
   */
  String searchSortBy();

  /**
   * Translated "Common tags:".
   * 
   * @return translated "Common tags:"
   * @gwt.key cloudCommonTags
   */
  String cloudCommonTags();

  /**
   * Translated "by".
   * 
   * @return translated "by"
   * @gwt.key creatorBy
   */
  String creatorBy();

  /**
   * Translated "No tags in use.".
   * 
   * @return translated "No tags in use."
   * @gwt.key msgNoTags
   */
  String msgNoTags();

  /**
   * Translated "{0}...".
   * 
   * @return translated "{0}..."
   * @gwt.key truncName
   */
  String truncName(String arg0);
}
