This is the source code to the [Whirled] online virtual world platform.

## Abandon hope all ye who enter here

If you've come here hoping to hack on the Whirled server and improve it for fun and profit, then
beware that Whirled is a many tentacled beast with more moving parts than a stadium-sized Rube
Goldberg machine. Moreover, it was unceremoniously yanked from its crufty ancestral home to rescue
it from destruction.

It would take many tens of thousands of dollars to pay someone with the sufficient level of
expertise to clean it up and make it easy for normal humans to hack on and improve. We don't have
the time or money to do that, so you're stuck with what you see here. A complex, tempermental
project that uses half a dozen major technologies to do hundreds of barely related things.

If you're not **very** proficient in [Java], [JDBC] and [servlets], reasonably proficient in
[ActionScript] and [Flex], familiar with [GWT], and familiar with either [Postgres] or [MySQL],
then you're going to have a long hard road in front of you.

Good luck!

## Building

The code is built with the [Ant] build tool. The main build target is `distall`, invoked like so:

```
ant distall
```

There are a number of intermediate build targets that are useful when iterating on individual parts
of the project, those can be seen by running `ant -p`:

```
% ant -p
Buildfile: .../msoy/build.xml

Main targets:

 asclient      Builds the Flash world client.
 clean         Cleans out compiled code.
 compile       Builds java class files.
 distall       Builds entire system (does not package).
 distcleanall  Fully cleans out the application and all subprojects.
 flashapps     Builds all Flash clients and applets.
 gclients      Builds all GWT clients (use -Dpages to choose which).
 genasync      Regenerates GWT Async interfaces.
 package       Builds entire system and generates dpkg files.
 tests         Runs unit tests.
 thane-client  Builds the thane game client.
 viewer        Build the viewer for the SDK.
Default target: compile
```

## Running a test instance

It is possible to run a test instance of the Whirled server on a developer machine. A variety of
configuration files must be copied into place and configured. The following files:

```
etc/build_settings.properties.dist
etc/burl-server.conf.dist
etc/burl-server.properties.dist
etc/msoy-server.conf.dist
etc/msoy-server.properties.dist
```

must be copied to:

```
etc/test/build_settings.properties
etc/test/burl-server.conf
etc/test/burl-server.properties
etc/test/msoy-server.conf
etc/test/msoy-server.properties
```

and edited to reflect the configuration of your local instance. The `.dist` config files contain
comments explaining each of the configurations. The most important configuration is for a local
database server (either MySQL or Postgres) which the Whirled server will use to store data.

Assuming you have successfully run `ant distall` to build the server and client, and you have
put the configuration files in place, and your database server is properly configured and running,
you can run the local Whirled instance like so:

```
./bin/msoyserver
```

This will report log messages to the console indicating the status of the running server. If you
see something like the following, then you're in good shape:

```
2015-09-02 09:36:40,546 INFO Logger: No AMQP messaging server configured.
2015-09-02 09:36:44,545 INFO Logger: Starting up [server=MsoyServer, os=Mac OS X (10.11-x86_64), jvm=1.8.0_45, Oracle Corporation]
2015-09-02 09:36:44,702 INFO Logger: Running thane bureaus locally
2015-09-02 09:36:44,719 INFO Slf4jLog: Logging to org.slf4j.impl.Log4jLoggerAdapter(org.mortbay.log) via org.mortbay.log.Slf4jLog
2015-09-02 09:36:44,757 INFO Logger: Running in cluster mode as node 'msoy1'.
2015-09-02 09:36:44,765 INFO Logger: Scheduling reboot on Thu Sep 03 01:00:00 PDT 2015 for automatic.
2015-09-02 09:36:44,810 INFO Slf4jLog: jetty-6.1.25
2015-09-02 09:36:44,825 INFO Logger: Underwire servlet initialized.
2015-09-02 09:36:44,872 INFO Slf4jLog: Started MsoyHttpServer$MsoyChannelConnector@0.0.0.0:8080
2015-09-02 09:36:44,901 INFO Logger: Policy server listening on port 47623.
2015-09-02 09:36:44,903 INFO Logger: Msoy server initialized.
2015-09-02 09:36:44,903 INFO Logger: DOMGR running.
2015-09-02 09:36:44,923 INFO Logger: Server listening on 0.0.0.0/0.0.0.0:4010.
```

You can then navigate to http://localhost:8080/ to access your local Whirled instance.

## License

The Whirled code is released under the BSD License. See the [LICENSE] file for details.

[ActionScript]: http://www.adobe.com/devnet/actionscript.html
[Ant]: http://ant.apache.org/
[Flex]: http://www.adobe.com/devnet/flex.html
[GWT]: http://www.gwtproject.org/
[JDBC]: http://docs.oracle.com/javase/7/docs/technotes/guides/jdbc/
[Java]: http://docs.oracle.com/javase/7/docs/
[LICENSE]: https://github.com/greyhavens/msoy/blob/master/LICENSE
[MySQL]: https://www.mysql.com/
[Postgres]: http://www.postgresql.org/
[Whirled]: http://whirled.com/
[servlets]: http://www.oracle.com/technetwork/java/index-jsp-135475.html
