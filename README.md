# Google Calendar Events Generator

A Java 23 standalone CLI application that will create recurring weekly Google Calendar events for a specified list of recurring Zoom meetings.

## Prerequisites

1. Java 23 JDK
2. Gradle
3. A Google Cloud project with Calendar API enabled
4. OAuth 2.0 client credentials (type “Desktop app”) — download `credentials.json` and place it in `src/main/resources/credentials.json`

## Build JAR

```bash
$ ./gradlew clean build shadowJar
$ java -Dapp.env=local|prod -jar build/libs/calendar-events-generator-1.0.0-SNAPSHOT.jar
```


