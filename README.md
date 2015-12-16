[![Circle CI](https://circleci.com/gh/amplitude/Amplitude-Android.svg?style=badge&circle-token=e01cc9eb8ea55f82890973569bf55412848b9e49)](https://circleci.com/gh/amplitude/Amplitude-Android)

Amplitude Android SDK
====================

An Android SDK for tracking events and revenue to [Amplitude](http://www.amplitude.com).

A [demo application](https://github.com/amplitude/Android-Demo) is available to show a simple integration.

# Setup #
1. If you haven't already, go to https://amplitude.com/signup and register for an account. Then, add an app. You will receive an API Key.

2. [Download the jar](https://github.com/amplitude/Amplitude-Android/raw/master/amplitude-android-2.4.0-with-dependencies.jar) and copy it into the "libs" folder in your Android project in Eclipse. If you're using an older build of Android, you may need to [add the jar file to your build path](http://stackoverflow.com/questions/3280353/how-to-import-a-jar-in-eclipse).

  Alternatively, if you are using Maven in your project, the jar is available on [Maven Central](http://search.maven.org/#artifactdetails%7Ccom.amplitude%7Candroid-sdk%7C2.4.0%7Cjar) using the following configuration in your pom.xml:

    ```
    <dependency>
      <groupId>com.amplitude</groupId>
      <artifactId>android-sdk</artifactId>
      <version>2.4.0</version>
    </dependency>
    ```

  Or if you are using gradle in your project, include in your build.gradle file:

    ```
    compile 'com.amplitude:android-sdk:2.4.0'
    ```

4.  In every file that uses analytics, import com.amplitude.api.Amplitude at the top:

    ```java
    import com.amplitude.api.Amplitude;
    ```

5. In the `onCreate()` of your main activity, initialize the SDK:

    ```java
    Amplitude.getInstance().initialize(this, "YOUR_API_KEY_HERE").enableForegroundTracking(getApplication());
    ```

6. To track an event anywhere in the app, call:

    ```java
    Amplitude.getInstance().logEvent("EVENT_IDENTIFIER_HERE");
    ```

7. If you want to use Google Advertising IDs, make sure to add [Google Play Services](https://developer.android.com/google/play-services/setup.html) to your project. _This is required for integrating with third party attribution services_

8. If you are using Proguard, add these exceptions to ```proguard.pro``` for Google Play Advertising IDs and Amplitude dependencies:

    ```yaml
        -keep class com.google.android.gms.ads.** { *; }
        -dontwarn okio.**
    ```

9. Events are saved locally. Uploads are batched to occur every 30 events and every 30 seconds. After calling `logEvent()` in your app, you will immediately see data appear on the Amplitude website.

# Tracking Events #

It's important to think about what types of events you care about as a developer. You should aim to track between 20 and 100 types of events within your app. Common event types are different screens within the app, actions a user initiates (such as pressing a button), and events you want a user to complete (such as filling out a form, completing a level, or making a payment). Contact us if you want assistance determining what would be best for you to track.

# Tracking Sessions #

A session is a period of time that a user has the app in the foreground. Events that are logged within the same session will have the same `session_id`. Sessions are handled automatically now; you no longer have to manually call `startSession()` or `endSession()`.

* For Android API level 14+, a new session is created when the app comes back into the foreground after being out of the foreground for 5 minutes or more. If the app is in the background and an event is logged, then a new session is created if more than 5 minutes has passed since the app entered the background or when the last event was logged (whichever occurred last). Otherwise the background event logged will be part of the current session. (Note you can define your own session experation time by calling `setMinTimeBetweenSessionsMillis(timeout)`, where the timeout input is in milliseconds.)

* For Android API level 13 and below, foreground tracking is not available, so a new session is automatically started when an event is logged 30 minutes or more after the last logged event. If another event is logged within 30 minutes, it will extend the current session. (Note you can define your own session expiration time by calling `setSessionTimeoutMillis(timeout)`, where the timeout input is in milliseconds. Also note, `enableForegroundTracking(getApplication)` is still safe to call for Android API level 13 and below, even though it is not available.)

Other Session Options:

1.  By default start and end session events are no longer sent. To re-enable add this line after initializing the SDK:

    ```java
    Amplitude.getInstance().trackSessionEvents(true);
    ```

2. You can also log events as out of session. Out of session events have a `session_id` of `-1` and are not considered part of the current session, meaning they do not extend the current session. This might be useful for example if you are logging events triggered by push notifications. You can log events as out of session by setting input parameter `outOfSession` to `true` when calling `logEvent()`:

    ```java
    Amplitude.getInstance().logEvent("EVENT", null, true);
    ```

# Setting Custom User IDs #

If your app has its own login system that you want to track users with, you can call `setUserId()` at any time:

```java
Amplitude.getInstance().setUserId("USER_ID_HERE");
```

A user's data will be merged on the backend so that any events up to that point on the same device will be tracked under the same user. Note: if a user logs out, or you want to log events under an anonymous user, you can also clear the user ID by calling `setUserId` with input `null`:

```java
Amplitude.getInstance().setUserId(null); // not string "null"
```

You can also add a user ID as an argument to the `initialize()` call:

```
Amplitude.getInstance().initialize(this, "YOUR_API_KEY_HERE", "USER_ID_HERE");
```

# Setting Event Properties #

You can attach additional data to any event by passing a JSONObject as the second argument to `logEvent()`:

```java
JSONObject eventProperties = new JSONObject();
try {
    eventProperties.put("KEY_GOES_HERE", "VALUE_GOES_HERE");
} catch (JSONException exception) {
}
Amplitude.getInstance().logEvent("Sent Message", eventProperties);
```

You will need to add two JSONObject imports to the code:

```java
import org.json.JSONException;
import org.json.JSONObject;
```

# User Properties and User Property Operations #

The SDK supports the operations set, setOnce, unset, and add on individual user properties. The operations are declared via a provided `Identify` interface. Multiple operations can be chained together in a single `Identify` object. The `Identify` object is then passed to the Amplitude client to send to the server. The results of the operations will be visible immediately in the dashboard, and take effect for events logged after.

To use the `Identify` interface, you will first need to import the class:
```java
import com.amplitude.api.Identify;
```

1. `set`: this sets the value of a user property.

    ```java
    Identify identify = new Identify().set('gender', 'female').set('age', 20);
    Amplitude.getInstance().identify(identify);
    ```

2. `setOnce`: this sets the value of a user property only once. Subsequent `setOnce` operations on that user property will be ignored. In the following example, `sign_up_date` will be set once to `08/24/2015`, and the following setOnce to `09/14/2015` will be ignored:

    ```java
    Identify identify1 = new Identify().setOnce('sign_up_date', '08/24/2015');
    Amplitude.getInstance().identify(identify1);

    Identify identify2 = new Identify().setOnce('sign_up_date', '09/14/2015');
    amplitude.identify(identify2);
    ```

3. `unset`: this will unset and remove a user property.

    ```java
    Identify identify = new Identify().unset('gender').unset('age');
    Amplitude.getInstance().identify(identify);
    ```

4. `add`: this will increment a user property by some numerical value. If the user property does not have a value set yet, it will be initialized to 0 before being incremented.

    ```java
    Identify identify = new Identify().add('karma', 1).add('friends', 1);
    Amplitude.getInstance().identify(identify);
    ```

5. `append`: this will append a value or values to a user property. If the user property does not have a value set yet, it will be initialized to an empty list before the new values are appended. If the user property has an existing value and it is not a list, it will be converted into a list with the new value appended.

    ```java
    Identify identify = new Identify().append("ab-tests", "new-user-test").append("some_list", new JSONArray().put(1).put("some_string"));
    Amplitude.getInstance().identify(identify);
    ```

Note: if a user property is used in multiple operations on the same `Identify` object, only the first operation will be saved, and the rest will be ignored. In this example, only the set operation will be saved, and the add and unset will be ignored:

```java
Identify identify = new Identify().set('karma', 10).add('karma', 1).unset('karma');
Amplitude.getInstance().identify(identify);
```

### Arrays in User Properties ###

The SDK supports arrays in user properties. Any of the user property operations above (with the exception of `add`) can accept a JSONArray or any Java primitive array. You can directly `set` arrays, or use `append` to generate an array.

```java
JSONArray colors = new JSONArray();
colors.put("rose").put("gold");
Identify identify = new Identify().set("colors", colors).append("ab-tests", "campaign_a").append("existing_list", new int[]{4,5});
Amplitude.getInstance().identify(identify);
```

### Setting Multiple Properties with `setUserProperties` ###

You may use `setUserProperties` shorthand to set multiple user properties at once. This method is simply a wrapper around `Identify.set` and `identify`.

```java
JSONObject userProperties = new JSONObject();
try {
    userProperties.put("KEY_GOES_HERE", "VALUE_GOES_HERE");
    userProperties.put("OTHER_KEY_GOES_HERE", "OTHER_VALUE_GOES_HERE");
} catch (JSONException exception) {
}
Amplitude.getInstance().setUserProperties(userProperties);
```

# Tracking Revenue #

To track revenue from a user, call `logRevenue()` each time a user generates revenue. For example:

```java
Amplitude.getInstance().logRevenue("com.company.productid", 1, 3.99);
```

`logRevenue()` takes a takes a string to identify the product (the product ID from Google Play), an int with the quantity of product purchased, and a double with the dollar amount of the sale. This allows us to automatically display data relevant to revenue on the Amplitude website, including average revenue per daily active user (ARPDAU), 1, 7, 14, 30, 60, and 90 day revenue, lifetime value (LTV) estimates, and revenue by advertising campaign cohort and daily/weekly/monthly cohorts.

**To enable revenue verification, copy your Google Play License Public Key into the manage section of your app on Amplitude. You must put a key for every single app in Amplitude where you want revenue verification.**

Then after a successful purchase transaction, call `logRevenue()` with the purchase data and receipt signature:

```java

// for a purchase request onActivityResult
String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

Amplitude.getInstance().logRevenue("com.company.productid", 1, 3.99, purchaseData, dataSignature);
```

See the [Google In App Billing Documentation](http://developer.android.com/google/play/billing/billing_integrate.html#Purchase) for details on how to retrieve the purchase data and receipt signature.

#### Amazon Store Revenue Verification

For purchases on the Amazon Store, you should copy your Amazon Shared Secret into the manage section of your app on Amplitude. After a successful purchase transaction, you should send the purchase token as the receipt and the user id as the receiptSignature:

```java
String purchaseToken = purchaseResponse.getReceipt();
String userId = getUserIdResponse.getUserId();

Amplitude.getInstance().logRevenue("com.company.productid", 1, 3.99, purchaseToken, userId);
```

# Fine-grained location tracking #

Amplitude access the Android location service (if possible) to add the specific coordinates (longitude and latitude)
where an event is logged.

This behaviour is enabled by default, but can be adjusted calling the following methods *after* initializing:

```java
Amplitude.getInstance().enableLocationListening();
Amplitude.getInstance().disableLocationListening();
```

Even disabling the location listening, the events will have the "country" property filled. That property
is retrieved from other sources (i.e. network or device locale).

# Allowing Users to Opt Out

To stop all event and session logging for a user, call setOptOut:

```java
Amplitude.getInstance().setOptOut(true);
```

Logging can be restarted by calling setOptOut again with enabled set to false.
No events will be logged during any period opt out is enabled.

# Advanced #

If you want to use the source files directly, you can [download them here](https://github.com/amplitude/Amplitude-Android/archive/master.zip). To include them in your project, extract the files, and then copy the five *.java files into your Android project.

If your app has multiple entry points/exit points, you should make a `Amplitude.getInstance().initialize()` at every `onCreate()` entry point.

This SDK automatically grabs useful data from the phone, including app version, phone model, operating system version, and carrier information. If your app has location permissions, the SDK will also grab the last known location of a user (this will not consume any extra battery, as it does not poll for a new location).

User IDs are automatically generated based on device specific identifiers if not specified.

By default, device IDs are a randomly generated UUID. If you would like to use Google's Advertising ID as the device ID, you can specify this by calling `Amplitude.getInstance().useAdvertisingIdForDeviceId()` prior to initializing. You can retrieve the Device ID that Amplitude uses with `Amplitude.getDeviceId()`. This method can return null if a Device ID hasn't been generated yet.

If you have your own system for tracking device IDs and would like to set a custom device ID, you can do so with `Amplitude.getInstance().setDeviceId("CUSTOM_DEVICE_ID")`. **Note: this is not recommended unless you really know what you are doing.** Make sure the device ID you set is sufficiently unique (we recommend something like a UUID - we use `UUID.randomUUID().toString()`) to prevent conflicts with other devices in our system.

The SDK includes support for SSL pinning, but it is undocumented and recommended against unless you have a specific need. Please contact Amplitude support before you ship any products with SSL pinning enabled so that we are aware and can provide documentation and implementation help.

You can disable all logging done in the SDK by calling `Amplitude.getInstance().enableLogging(false)`. By default the logging level is Log.INFO, meaning info messages, errors, and asserts are logged, but verbose and debug messages are not. You can change the logging level, for example to enable debug messages you can do `Amplitude.getInstance().setLogLevel(Log.DEBUG)`.
