package client.game;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/game/GameMessages.properties'.
 */
public interface GameMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "Multiplayer".
   * 
   * @return translated "Multiplayer"
   * @gwt.key gdpMultiplayer
   */
  String gdpMultiplayer();

  /**
   * Translated "{0}+".
   * 
   * @return translated "{0}+"
   * @gwt.key gdpPlayersParty
   */
  String gdpPlayersParty(String arg0);

  /**
   * Translated "You must earn this trophy in the game to see its description.".
   * 
   * @return translated "You must earn this trophy in the game to see its description."
   * @gwt.key gameTrophySecret
   */
  String gameTrophySecret();

  /**
   * Translated "Note: this is the in-development version of this game. Trophies awarded during this game will not be saved nor will ratings be updated.".
   * 
   * @return translated "Note: this is the in-development version of this game. Trophies awarded during this game will not be saved nor will ratings be updated."
   * @gwt.key gdpDevVersion
   */
  String gdpDevVersion();

  /**
   * Translated "No trophies.".
   * 
   * @return translated "No trophies."
   * @gwt.key caseEmpty
   */
  String caseEmpty();

  /**
   * Translated "Loading game details...".
   * 
   * @return translated "Loading game details..."
   * @gwt.key gdpLoading
   */
  String gdpLoading();

  /**
   * Translated "Games played:".
   * 
   * @return translated "Games played:"
   * @gwt.key gdpGamesPlayed
   */
  String gdpGamesPlayed();

  /**
   * Translated "Loading trophies...".
   * 
   * @return translated "Loading trophies..."
   * @gwt.key caseLoading
   */
  String caseLoading();

  /**
   * Translated "This game awards no trophies.".
   * 
   * @return translated "This game awards no trophies."
   * @gwt.key gameTrophyNoTrophies
   */
  String gameTrophyNoTrophies();

  /**
   * Translated "or play with friends:".
   * 
   * @return translated "or play with friends:"
   * @gwt.key gdpOrWithFriends
   */
  String gdpOrWithFriends();

  /**
   * Translated "Earned on {0}".
   * 
   * @return translated "Earned on {0}"
   * @gwt.key gameTrophyEarnedOn
   */
  String gameTrophyEarnedOn(String arg0);

  /**
   * Translated "Players:".
   * 
   * @return translated "Players:"
   * @gwt.key gdpPlayers
   */
  String gdpPlayers();

  /**
   * Translated "Player minutes:".
   * 
   * @return translated "Player minutes:"
   * @gwt.key gdpPlayerMinutes
   */
  String gdpPlayerMinutes();

  /**
   * Translated "Last abuse recalc:".
   * 
   * @return translated "Last abuse recalc:"
   * @gwt.key gdpLastRecalc
   */
  String gdpLastRecalc();

  /**
   * Translated "Zoiks! Uknown game type {0}.".
   * 
   * @return translated "Zoiks! Uknown game type {0}."
   * @gwt.key errUnknownGameType
   */
  String errUnknownGameType(String arg0);

  /**
   * Translated "{0} to {1}".
   * 
   * @return translated "{0} to {1}"
   * @gwt.key gdpPlayersFixed
   */
  String gdpPlayersFixed(String arg0,  String arg1);

  /**
   * Translated "Comments".
   * 
   * @return translated "Comments"
   * @gwt.key tabComments
   */
  String tabComments();

  /**
   * Translated "{0}''s Trophies".
   * 
   * @return translated "{0}''s Trophies"
   * @gwt.key caseTitle
   */
  String caseTitle(String arg0);

  /**
   * Translated "Average duration:".
   * 
   * @return translated "Average duration:"
   * @gwt.key gdpAvgDuration
   */
  String gdpAvgDuration();

  /**
   * Translated "Abuse factor:".
   * 
   * @return translated "Abuse factor:"
   * @gwt.key gdpAbuseFactor
   */
  String gdpAbuseFactor();

  /**
   * Translated "Loading trophies.".
   * 
   * @return translated "Loading trophies."
   * @gwt.key gameTrophyLoading
   */
  String gameTrophyLoading();

  /**
   * Translated "Trophies".
   * 
   * @return translated "Trophies"
   * @gwt.key tabTrophies
   */
  String tabTrophies();
}
