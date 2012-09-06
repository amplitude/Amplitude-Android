# Setup #
1. If you haven't already, go to http://giraffegraph.com and register for an account. You will receive an API Key.
2. [Download the jar](http://giraffegraph.com/static/downloads/giraffegraph-android.jar).
3. Copy the jar into the "libs" folder in your Android project in Eclipse. If you're using an older version of Android build, you may need to [add the jar file to your build path](http://stackoverflow.com/questions/3280353/how-to-import-a-jar-in-eclipse).
4. In every file that uses analytics, you will need to place the following at the top:

        import package com.giraffegraph.api;

5. In the onCreate of your main activity you need to initialize the SDK:

        EventLog.initialize(this, "YOUR_API_KEY_HERE");

6. In the onDestroy of your main activity you need to upload events before the app is destroyed:

        EventLog.uploadEvents();

7. To track an event anywhere in the app, call:

        EventLog.logEvent("EVENT_IDENTIFIER_HERE");

8. Events are saved locally. Uploads are batched to occur every 10 events and every 10 seconds. After calling logEvent in your app, you will immediately see data appear on Giraffe Graph.

# Tracking Events #

It's important to think about what types of events you care about as a developer. You should aim to track at least 5 and no more than 50 types of events within your app. Common event types are different screens within the app, actions the user initiates (such as pressing a button), and events you want the user to complete (such as filling out a form, completing a level, or making a payment). Shoot me an email if you want assistance determining what would be best for you to track.

# Tracking Sessions #

A session is a period of time that a user has the app in the foreground. Sessions within 10 seconds of each other are merged into a single session. To track sessions, add the following to each onResume() in every activity in your app:

    EventLog.startSession() 

and the following to each onPause() in every activity in your app:

    EventLog.endSession()

# Settings Custom User IDs #

If your app has its own login system that you want to track users with, you can call the following at any time:

    EventLog.setUserId("USER_ID_HERE");

You can also add the user ID as an argument to the initialize call:

    EventLog.initialize(this, "YOUR_API_KEY_HERE", "USER_ID_HERE");

Users data will be merged on the backend so that any events up to that point will be tracked under the same user.

# Setting Custom Properties #

You can attach additional data to any event by passing a JSONObject as the second argument to logEvent():

    JSONObject customProperties = new JSONObject();
    try {
      customProperties.put("KEY_GOES_HERE", "VALUE_GOES_HERE");
    } catch (JSONException exception) {
    }
    EventLog.logEvent(Action.OPEN.toString(), customProperties);

You will need to add the following imports to the code:

    import org.json.JSONException;
    import org.json.JSONObject;

To add properties that are tracked in every event, you can set global properties for a user:

    JSONObject globalProperties = new JSONObject();
    try {
      globalProperties.put("KEY_GOES_HERE", "VALUE_GOES_HERE");
    } catch (JSONException exception) {
    }
    EventLog.setGlobalUserProperties(globalProperties);

# Advanced #

If you want to use the source files directly, you can [download them here](giraffegraph.com/static/downloads/giraffegraph-android.zip). To include them in your project, extract the files, and then copy the four *.java files into your Android project.

If your app has multiple entry points/exit points, you should make a `EventLog.initialize()` at every onCreate() entry point and a `EventLog.uploadEvents()` at every onDestroy() exit point.

This SDK automatically grabs useful data from the phone, including app version, phone model, operating system version, and carrier information. If your app has location permissions, the SDK will also grab the last known location of the user (this will not consume any extra battery, as it does not poll for a new location).

User IDs are automatically generated based on device specific identifiers if not specified.
