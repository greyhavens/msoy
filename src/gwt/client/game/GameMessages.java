package client.game;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/game/GameMessages.properties'.
 */
public interface GameMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "Earned on {0}".
   * 
   * @return translated "Earned on {0}"
   * @gwt.key gameTrophyEarnedOn
   */
  String gameTrophyEarnedOn(String arg0);

  /**
   * Translated "Note: this is the in-development version of this game. Trophies awarded during this game will not be saved nor will ratings be updated.".
   * 
   * @return translated "Note: this is the in-development version of this game. Trophies awarded during this game will not be saved nor will ratings be updated."
   * @gwt.key gdpDevVersion
   */
  String gdpDevVersion();

  /**
   * Translated "#{0}".
   * 
   * @return translated "#{0}"
   * @gwt.key gameRank
   */
  String gameRank(String arg0);

  /**
   * Translated "Last abuse recalc:".
   * 
   * @return translated "Last abuse recalc:"
   * @gwt.key gdpLastRecalc
   */
  String gdpLastRecalc();

  /**
   * Translated "Comments".
   * 
   * @return translated "Comments"
   * @gwt.key tabComments
   */
  String tabComments();

  /**
   * Translated "Average time:".
   * 
   * @return translated "Average time:"
   * @gwt.key gdpAvgDuration
   */
  String gdpAvgDuration();

  /**
   * Translated "No trophies.".
   * 
   * @return translated "No trophies."
   * @gwt.key caseEmpty
   */
  String caseEmpty();

  /**
   * Translated "This game awards no trophies.".
   * 
   * @return translated "This game awards no trophies."
   * @gwt.key gameTrophyNoTrophies
   */
  String gameTrophyNoTrophies();

  /**
   * Translated "Player minutes:".
   * 
   * @return translated "Player minutes:"
   * @gwt.key gdpPlayerMinutes
   */
  String gdpPlayerMinutes();

  /**
   * Translated "Trophies".
   * 
   * @return translated "Trophies"
   * @gwt.key tabTrophies
   */
  String tabTrophies();

  /**
   * Translated "Games played:".
   * 
   * @return translated "Games played:"
   * @gwt.key gdpGamesPlayed
   */
  String gdpGamesPlayed();

  /**
   * Translated "Zoiks! Uknown game type {0}.".
   * 
   * @return translated "Zoiks! Uknown game type {0}."
   * @gwt.key errUnknownGameType
   */
  String errUnknownGameType(String arg0);

  /**
   * Translated "Players:".
   * 
   * @return translated "Players:"
   * @gwt.key gdpPlayers
   */
  String gdpPlayers();

  /**
   * Translated "Loading trophies...".
   * 
   * @return translated "Loading trophies..."
   * @gwt.key caseLoading
   */
  String caseLoading();

  /**
   * Translated "My Rankings".
   * 
   * @return translated "My Rankings"
   * @gwt.key tabMyRankings
   */
  String tabMyRankings();

  /**
   * Translated "{0} to {1}".
   * 
   * @return translated "{0} to {1}"
   * @gwt.key gdpPlayersFixed
   */
  String gdpPlayersFixed(String arg0,  String arg1);

  /**
   * Translated "Game Detail".
   * 
   * @return translated "Game Detail"
   * @gwt.key gdpTitle
   */
  String gdpTitle();

  /**
   * Translated "Multiplayer".
   * 
   * @return translated "Multiplayer"
   * @gwt.key gdpMultiplayer
   */
  String gdpMultiplayer();

  /**
   * Translated "Trophies".
   * 
   * @return translated "Trophies"
   * @gwt.key caseTitle
   */
  String caseTitle();

  /**
   * Translated "{0}+".
   * 
   * @return translated "{0}+"
   * @gwt.key gdpPlayersParty
   */
  String gdpPlayersParty(String arg0);

  /**
   * Translated "or play with friends:".
   * 
   * @return translated "or play with friends:"
   * @gwt.key gdpOrWithFriends
   */
  String gdpOrWithFriends();

  /**
   * Translated "Loading trophies.".
   * 
   * @return translated "Loading trophies."
   * @gwt.key gameTrophyLoading
   */
  String gameTrophyLoading();

  /**
   * Translated "Loading game details...".
   * 
   * @return translated "Loading game details..."
   * @gwt.key gdpLoading
   */
  String gdpLoading();

  /**
   * Translated "Abuse factor:".
   * 
   * @return translated "Abuse factor:"
   * @gwt.key gdpAbuseFactor
   */
  String gdpAbuseFactor();

  /**
   * Translated "Top Rankings".
   * 
   * @return translated "Top Rankings"
   * @gwt.key tabTopRankings
   */
  String tabTopRankings();

  /**
   * Translated "You must earn this trophy in the game to see its description.".
   * 
   * @return translated "You must earn this trophy in the game to see its description."
   * @gwt.key gameTrophySecret
   */
  String gameTrophySecret();
}
