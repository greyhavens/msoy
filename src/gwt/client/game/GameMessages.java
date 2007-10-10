package client.game;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/game/GameMessages.properties'.
 */
public interface GameMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "Zoiks! Uknown game type {0}.".
   * 
   * @return translated "Zoiks! Uknown game type {0}."
   * @gwt.key errUnknownGameType
   */
  String errUnknownGameType(String arg0);

  /**
   * Translated "No trophies.".
   * 
   * @return translated "No trophies."
   * @gwt.key caseEmpty
   */
  String caseEmpty();

  /**
   * Translated "{0}''s Trophies".
   * 
   * @return translated "{0}''s Trophies"
   * @gwt.key caseTitle
   */
  String caseTitle(String arg0);

  /**
   * Translated "Loading trophies...".
   * 
   * @return translated "Loading trophies..."
   * @gwt.key caseLoading
   */
  String caseLoading();
}
