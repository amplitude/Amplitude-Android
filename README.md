# Setup #
1. If you haven't already, go to https://amplitude.com and register for an account. You will receive an API Key.
2. [Download the jar](https://github.com/amplitude/Amplitude-Android/raw/master/amplitude.jar).
3. Copy the jar into the "libs" folder in your Android project in Eclipse. If you're using an older build of Android, you may need to [add the jar file to your build path](http://stackoverflow.com/questions/3280353/how-to-import-a-jar-in-eclipse).
4. In every file that uses analytics, import com.amplitude.api at the top:

        import package com.amplitude.api;

5. In the `onCreate()` of your main activity, initialize the SDK:

        Amplitude.initialize(this, "YOUR_API_KEY_HERE");

6. Add a `startSession()` call to each `onResume()` in every activity in your app:

        Amplitude.startSession();

7. Add an `endSession()` call to each `onPause()` in every activity in your app. This call also ensures data is uploaded before the app closes:

        Amplitude.endSession();

8. To track an event anywhere in the app, call:

        Amplitude.logEvent("EVENT_IDENTIFIER_HERE");

9. Events are saved locally. Uploads are batched to occur every 30 events and every 30 seconds. After calling `logEvent()` in your app, you will immediately see data appear on the Amplitude website.

# Tracking Events #

It's important to think about what types of events you care about as a developer. You should aim to track between 5 and 50 types of events within your app. Common event types are different screens within the app, actions a user initiates (such as pressing a button), and events you want a user to complete (such as filling out a form, completing a level, or making a payment). Contact us if you want assistance determining what would be best for you to track.

# Tracking Sessions #

A session is a period of time that a user has the app in the foreground. Calls to `startSession()` and `endSession()` track the duration of a session. Sessions within 10 seconds of each other are merged into a single session when they are reported in Amplitude.

Calling `startSession()` in `onResume()` will generate a start session event every time the app regains focus or comes out of a locked screen. Calling `endSession()` in `onPause()` will generate an end session event every time the foreground activity loses focus or the screen becomes locked. If you'd prefer to only log session starts and ends when the app is no longer visible, instead of no longer in focus, you can place the `startSession()` and `endSession()` calls in `onStart()` and `onStop()`, respectively. Note that `onStart()` and `onStop()` are not called when a user unlocks and locks the screen.

# Setting Custom User IDs #

If your app has its own login system that you want to track users with, you can call `setUserId()` at any time:

    Amplitude.setUserId("USER_ID_HERE");

A user's data will be merged on the backend so that any events up to that point on the same device will be tracked under the same user.

You can also add a user ID as an argument to the `initialize()` call:

    Amplitude.initialize(this, "YOUR_API_KEY_HERE", "USER_ID_HERE");

# Setting Event Properties #

You can attach additional data to any event by passing a JSONObject as the second argument to `logEvent()`:

    JSONObject eventProperties = new JSONObject();
    try {
      eventProperties.put("KEY_GOES_HERE", "VALUE_GOES_HERE");
    } catch (JSONException exception) {
    }
    Amplitude.logEvent("Sent Message", eventProperties);

You will need to add two JSONObject imports to the code:

    import org.json.JSONException;
    import org.json.JSONObject;

To add properties that are associated with a user, you can set user properties:

    JSONObject userProperties = new JSONObject();
    try {
      userProperties.put("KEY_GOES_HERE", "VALUE_GOES_HERE");
    } catch (JSONException exception) {
    }
    Amplitude.setUserProperties(userProperties);

# Tracking Revenue #

To track revenue from a user, call `logRevenue()` each time a user generates revenue. For example:

    Amplitude.logRevenue(3.99);
    
`logRevenue()` takes a double with the dollar amount of the sale as the only argument. This allows us to automatically display data relevant to revenue on the Amplitude website, including average revenue per daily active user (ARPDAU), 1, 7, 14, 30, 60, and 90 day revenue, lifetime value (LTV) estimates, and revenue by advertising campaign cohort and daily/weekly/monthly cohorts.

# Advanced #

If you want to use the source files directly, you can [download them here](https://github.com/amplitude/Amplitude-Android/archive/master.zip). To include them in your project, extract the files, and then copy the five *.java files into your Android project.

If your app has multiple entry points/exit points, you should make a `Amplitude.initialize()` at every `onCreate()` entry point.

This SDK automatically grabs useful data from the phone, including app version, phone model, operating system version, and carrier information. If your app has location permissions, the SDK will also grab the last known location of a user (this will not consume any extra battery, as it does not poll for a new location).

User IDs are automatically generated based on device specific identifiers if not specified.
