[![Circle CI](https://circleci.com/gh/amplitude/Amplitude-Android.svg?style=badge&circle-token=e01cc9eb8ea55f82890973569bf55412848b9e49)](https://circleci.com/gh/amplitude/Amplitude-Android)

# Setup #
1. If you haven't already, go to https://amplitude.com/signup and register for an account. Then, add an app. You will receive an API Key.

2. [Download the jar](https://github.com/amplitude/Amplitude-Android/raw/master/amplitude-android-1.4.3.jar) and copy it into the "libs" folder in your Android project in Eclipse. If you're using an older build of Android, you may need to [add the jar file to your build path](http://stackoverflow.com/questions/3280353/how-to-import-a-jar-in-eclipse).

3. Alternatively, if you are using Maven in your project, the jar is available on [Maven Central](http://search.maven.org/#artifactdetails%7Ccom.amplitude%7Candroid-sdk%7C1.4.3%7Cjar) using the following configuration in your pom.xml:

    ```
    <dependency>
      <groupId>com.amplitude</groupId>
      <artifactId>android-sdk</artifactId>
      <version>1.4.3</version>
    </dependency>
    ```

4. In every file that uses analytics, import com.amplitude.api.Amplitude at the top:

    ```java
    import com.amplitude.api.Amplitude;
    ```

5. In the `onCreate()` of your main activity, initialize the SDK:

    ```java
    Amplitude.initialize(this, "YOUR_API_KEY_HERE");
    ```

6. Add a `startSession()` call to each `onResume()` in every activity in your app:

    ```java
    Amplitude.startSession();
    ```

7. Add an `endSession()` call to each `onPause()` in every activity in your app. This call also ensures data is uploaded before the app closes:

    ```java
    Amplitude.endSession();
    ```

8. To track an event anywhere in the app, call:

    ```java
    Amplitude.logEvent("EVENT_IDENTIFIER_HERE");
    ```

9. Events are saved locally. Uploads are batched to occur every 30 events and every 30 seconds. After calling `logEvent()` in your app, you will immediately see data appear on the Amplitude website.

# Tracking Events #

It's important to think about what types of events you care about as a developer. You should aim to track between 20 and 100 types of events within your app. Common event types are different screens within the app, actions a user initiates (such as pressing a button), and events you want a user to complete (such as filling out a form, completing a level, or making a payment). Contact us if you want assistance determining what would be best for you to track.

# Tracking Sessions #

A session is a period of time that a user has the app in the foreground. Calls to `startSession()` and `endSession()` track the duration of a session. Sessions within 10 seconds of each other are merged into a single session when they are reported in Amplitude.

Calling `startSession()` in `onResume()` will generate a start session event every time the app regains focus or comes out of a locked screen. Calling `endSession()` in `onPause()` will generate an end session event every time the foreground activity loses focus or the screen becomes locked. If you'd prefer to only log session starts and ends when the app is no longer visible, instead of no longer in focus, you can place the `startSession()` and `endSession()` calls in `onStart()` and `onStop()`, respectively. Note that `onStart()` and `onStop()` are not called when a user unlocks and locks the screen.

# Setting Custom User IDs #

If your app has its own login system that you want to track users with, you can call `setUserId()` at any time:

```java
Amplitude.setUserId("USER_ID_HERE");
```

A user's data will be merged on the backend so that any events up to that point on the same device will be tracked under the same user.

You can also add a user ID as an argument to the `initialize()` call:

```
Amplitude.initialize(this, "YOUR_API_KEY_HERE", "USER_ID_HERE");
```

# Setting Event Properties #

You can attach additional data to any event by passing a JSONObject as the second argument to `logEvent()`:

```java
JSONObject eventProperties = new JSONObject();
try {
    eventProperties.put("KEY_GOES_HERE", "VALUE_GOES_HERE");
} catch (JSONException exception) {
}
Amplitude.logEvent("Sent Message", eventProperties);
```

You will need to add two JSONObject imports to the code:

```java
import org.json.JSONException;
import org.json.JSONObject;
```

# Setting User Properties #

To add properties that are associated with a user, you can set user properties:

```java
JSONObject userProperties = new JSONObject();
try {
    userProperties.put("KEY_GOES_HERE", "VALUE_GOES_HERE");
} catch (JSONException exception) {
}
Amplitude.setUserProperties(userProperties);
```

# Tracking Revenue #

To track revenue from a user, call `logRevenue()` each time a user generates revenue. For example:

```java
Amplitude.logRevenue("com.company.productid", 1, 3.99);
```

`logRevenue()` takes a takes a string to identify the product (the product ID from Google Play), an int with the quantity of product purchased, and a double with the dollar amount of the sale. This allows us to automatically display data relevant to revenue on the Amplitude website, including average revenue per daily active user (ARPDAU), 1, 7, 14, 30, 60, and 90 day revenue, lifetime value (LTV) estimates, and revenue by advertising campaign cohort and daily/weekly/monthly cohorts.

**To enable revenue verification, copy your Google Play License Public Key into the manage section of your app on Amplitude. You must put a key for every single app in Amplitude where you want revenue verification.**

Then after a successful purchase transaction, call `logRevenue()` with the purchase data and receipt signature:

```java

// for a purchase request onActivityResult
String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

Amplitude.logRevenue("com.company.productid", 1, 3.99, purchaseData, receiptSignature);
```

See the [Google In App Billing Documentation](http://developer.android.com/google/play/billing/billing_integrate.html#Purchase) for details on how to retrieve the purchase data and receipt signature.

# Fine-grained location tracking #

Amplitude access the Android location service (if possible) to add the specific coordinates (longitude and latitude)
where an event is logged.

This behaviour is enabled by default, but can be adjusted calling the following methods *after* initializing:

```java
Amplitude.enableLocationListening();
Amplitude.disableLocationListening();
```

Even disabling the location listening, the events will have the "country" property filled. That property
is retrieved from other sources (i.e. network or device locale).


# Advanced #

If you want to use the source files directly, you can [download them here](https://github.com/amplitude/Amplitude-Android/archive/master.zip). To include them in your project, extract the files, and then copy the five *.java files into your Android project.

If your app has multiple entry points/exit points, you should make a `Amplitude.initialize()` at every `onCreate()` entry point.

This SDK automatically grabs useful data from the phone, including app version, phone model, operating system version, and carrier information. If your app has location permissions, the SDK will also grab the last known location of a user (this will not consume any extra battery, as it does not poll for a new location).

User IDs are automatically generated based on device specific identifiers if not specified.

By default, device IDs are a randomly generated UUID. If you would like to use Google's Advertising ID as the device ID, you can specify this by calling `Amplitude.useAdvertisingIdForDeviceId()` prior to initializing. You can retrieve the Device ID that Amplitude uses with `Amplitude.getDeviceId()`. This method can return null if a Device ID hasn't been generated yet.
