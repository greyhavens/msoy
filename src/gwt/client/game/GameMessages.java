package client.game;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/game/GameMessages.properties'.
 */
public interface GameMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "This game awards no trophies.".
   * 
   * @return translated "This game awards no trophies."
   * @gwt.key gameTrophyNoTrophies
   */
  String gameTrophyNoTrophies();

  /**
   * Translated "Earned on {0}".
   * 
   * @return translated "Earned on {0}"
   * @gwt.key gameTrophyEarnedOn
   */
  String gameTrophyEarnedOn(String arg0);

  /**
   * Translated "Zoiks! Uknown game type {0}.".
   * 
   * @return translated "Zoiks! Uknown game type {0}."
   * @gwt.key errUnknownGameType
   */
  String errUnknownGameType(String arg0);

  /**
   * Translated "You must earn this trophy in the game to see its description.".
   * 
   * @return translated "You must earn this trophy in the game to see its description."
   * @gwt.key gameTrophySecret
   */
  String gameTrophySecret();

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
   * Translated "Loading trophies.".
   * 
   * @return translated "Loading trophies."
   * @gwt.key gameTrophyLoading
   */
  String gameTrophyLoading();

  /**
   * Translated "Loading trophies...".
   * 
   * @return translated "Loading trophies..."
   * @gwt.key caseLoading
   */
  String caseLoading();
}
