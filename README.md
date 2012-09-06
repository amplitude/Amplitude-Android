# Setup #
1. If you haven't already, go to http://giraffegraph.com and register for an account. You will receive an API Key.
2. [Download the jar](http://giraffegraph.com/static/downloads/giraffegraph-android.jar).
3. Copy the jar into the "libs" folder in your Android project in Eclipse. If you're using an older version of Android build, you may need to [add the jar file to your build path](http://stackoverflow.com/questions/3280353/how-to-import-a-jar-in-eclipse).
4. In every file that uses analytics, you will need to place

        import package com.sonalight.analytics.api;

at the top.

5. In the onCreate of your main activity call:

        EventLog.initialize(this, "YOUR_API_KEY_HERE");

6. In the onDestroy of your main activity call:

        EventLog.uploadEvents();

7. To track an event anywhere in the app, call:

        EventLog.logEvent("EVENT_IDENTIFIER_HERE");

8. Events are saved locally. Uploads are batched to occur every 10 events and every 10 seconds. After calling logEvent in your app, you will immediately see data pop up on Giraffe Graph.

# Advanced #

Download the source files as a zip here: giraffegraph.com/static/downloads/giraffegraph-android.zip
Copy the four source .java files into your Android project.


Finally, in order to track sessions, include EventLog.startSession() in every onResume() in every activity and EventLog.endSession() in every onPause() in every activity in your app.