
## Code for parsing information from game logs
import re, datetime, util.log

## types of events in game logs
SESSION_START = "sessionStart"
GAME_SESSION_START = "gameSessionStart"
SESSION_END = "sessionEnd"
FLOW_SCALED = "flowScaled"
GAME_ENDED = "gameEnd"
FLOW_AWARDED = "flowAwarded"
RATING_UPDATED = "ratingUpdated"

# All the event types we care about for a game log
parsers = [
    util.log.EventTypeParser(SESSION_START, "ClientManager: Session initiated"),
    util.log.EventTypeParser(GAME_SESSION_START, "Player session starting"),
    util.log.EventTypeParser(FLOW_SCALED,
        "AwardDelegate: Scaling player's awardable flow due to short game", "game"),
    util.log.EventTypeParser(SESSION_END, "ClientManager: Ending session(: .*)?"),
    util.log.EventTypeParser(GAME_ENDED, "AwardDelegate: endGameWithScores"),
    util.log.EventTypeParser(FLOW_AWARDED, "AwardDelegate: Awarding flow", "game"),
    util.log.EventTypeParser(RATING_UPDATED, "AwardDelegate: Updated rating", "where"),
]

def enumerateEvents (file):
    """Generates all known game events from the given file object."""
    return util.log.enumerateEvents(file, parsers)

