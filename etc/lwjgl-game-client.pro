#
# $Id$
#
# Proguard configuration file for the Java game client that includes LWJGL

-include base-game-client.pro

-injars ../dist/lib/lwjgl-applet.jar(!META-INF/*)
-outjars ../dist/lwjgl-game-client.jar

-keep public class org.lwjgl.** {
    public protected *;
}
