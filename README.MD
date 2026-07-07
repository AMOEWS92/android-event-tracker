\# Android Event Tracker



A native Android application for creating, viewing, and deleting locally stored events.



Users can enter an event name, date, and location, then view saved events in the app. The project also includes optional SMS reminder settings with Android runtime permission handling.



\## Built With



\- Java

\- Android Studio

\- SQLite

\- Gradle



\## Features



\- Create event records with a name, date, and location

\- View saved events in a table-style list

\- Delete events from the list

\- Store event data locally with SQLite

\- Use repository classes to separate database logic from screen logic

\- Handle optional SMS permission requests

\- Enable a test SMS reminder workflow when permission is granted



\## Java and Android Skills Demonstrated



\- Object-oriented Java classes and Android activities

\- SQLite database operations for creating, reading, and deleting records

\- Repository pattern for organized data access

\- User input validation and error messages

\- Android intents for moving between screens

\- Runtime permission handling for SMS features



\## Running the Project



1\. Open the project folder in Android Studio.

2\. Allow Gradle to sync project dependencies.

3\. Run the app on an Android emulator or compatible device.



\## Notes



The SMS settings screen requests permission only when the user chooses to enable the feature. The current SMS workflow sends a test message to an emulator-friendly placeholder number rather than scheduling automatic reminders for each event.

