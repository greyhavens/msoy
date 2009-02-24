These classes are here because we include these resources in both the main
world-client as well as the small flex-free media players. Since the media
players are basically part of the world-client, they are included in two places.
Surprise surprise, the flex compiler is too stupid to realize that a resource
is being included twice and only generate one class, so we do it manually.
