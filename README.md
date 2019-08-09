# monic

Desktop utility that dynamically shows if your internet connexion is up or down

## How to build

Make sure you have Java 8+ and Maven installed then cd in the projet folder and run
```
mvn package
```

## Run with a desktop

```
java -jar target/monic-1.0.0-SNAPSHOT.jar [host (defaults to 'www.google.com')] [port (defaults to 80)]
```
If you need to configure a proxy to connect to the host you will have to specify it on the command line
```
java -Dhttp.proxyHost=<your proxy host> -jar target/monic-1.0.0-SNAPSHOT.jar [host] [port]
```

The application will immediately open a socket to the host. Upon failure the application will retry every minute. Upon succes the application will not try again.

A red/green LED icon should show up in your system tray and reflect the connection status of the application.

You can right-clic the icon to close the application.

## Run without a desktop

It is also possible to run the application when no system tray is available. The application is logging using java.util.logging. You may use the following command line
```
java -Djava.util.logging.config.file=logging.properties -jar target/monic-1.0.0-SNAPSHOT.jar
```
Where logging.properties if a file in the working directory with following content :
```
monic.Main.level=ALL
monic.Main.handlers=java.util.logging.ConsoleHandler
java.util.logging.ConsoleHandler.level=ALL
```
