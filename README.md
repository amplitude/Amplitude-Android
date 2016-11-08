[![Circle CI](https://circleci.com/gh/amplitude/Amplitude-Android.svg?style=badge&circle-token=e01cc9eb8ea55f82890973569bf55412848b9e49)](https://circleci.com/gh/amplitude/Amplitude-Android)

Amplitude Android SDK
====================

An Android SDK for tracking events and revenue to [Amplitude](http://www.amplitude.com).

A [demo application](https://github.com/amplitude/Android-Demo) is available to show a simple integration.

See our [SDK documentation](https://rawgit.com/amplitude/Amplitude-Android/master/javadoc/index.html) for a description of all available SDK methods.

# Setup #
1. If you haven't already, go to https://amplitude.com/signup and register for an account. Then, add an app. You will receive an API Key.

2. [Download the jar](https://github.com/amplitude/Amplitude-Android/raw/master/amplitude-android-2.12.0-with-dependencies.jar) and copy it into the "libs" folder in your Android project in Android Studio.

  Alternatively, if you are using Maven in your project, the jar is available on [Maven Central](http://search.maven.org/#artifactdetails%7Ccom.amplitude%7Candroid-sdk%7C2.12.0%7Cjar) using the following configuration in your pom.xml:

    ```
    <dependency>
      <groupId>com.amplitude</groupId>
      <artifactId>android-sdk</artifactId>
      <version>2.12.0</version>
    </dependency>
    ```

  Or if you are using gradle in your project, include in your build.gradle file:

    ```
    compile 'com.amplitude:android-sdk:2.12.0'
    ```

3. If you haven't already, add the [INTERNET](https://developer.android.com/reference/android/Manifest.permission.html#INTERNET) permission to your manifest file:

    ``` xml
    <uses-permission android:name="android.permission.INTERNET" />
    ```

4.  In every file that uses analytics, import com.amplitude.api.Amplitude at the top:

    ```java
    import com.amplitude.api.Amplitude;
    ```

5. In the `onCreate()` of your main activity, initialize the SDK:

    ```java
    Amplitude.getInstance().initialize(this, "YOUR_API_KEY_HERE").enableForegroundTracking(getApplication());
    ```

    Note: if your app has multiple entry points/exit points, you should make a `Amplitude.getInstance().initialize()` at every `onCreate()` entry point.

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

9. Events are saved locally. Uploads are batched to occur every 30 events and every 30 seconds. After calling `logEvent()` in your app, you will immediately see data appear on the Amplitude website. *NOTE:* by default unsent events are uploaded when your app is minimized or closed via the Activity Lifecycle `onPause` callback. If you wish to disable this behavior you can call `Amplitude.getInstance().setFlushEventsOnClose(false);`

# Tracking Events #

It's important to think about what types of events you care about as a developer. You should aim to track between 20 and 200 types of events on your site. Common event types are actions the user initiates (such as pressing a button) and events you want the user to complete (such as filling out a form, completing a level, or making a payment).

Here are some resources to help you with your instrumentation planning:
  * [Event Tracking Quick Start Guide](https://amplitude.zendesk.com/hc/en-us/articles/207108137).
  * [Event Taxonomy and Best Practices](https://amplitude.zendesk.com/hc/en-us/articles/211988918).

Having large amounts of distinct event types, event properties and user properties, however, can make visualizing and searching of the data very confusing. By default we only show the first:
  * 1000 distinct event types
  * 2000 distinct event properties
  * 1000 distinct user properties

Anything past the above thresholds will not be visualized. **Note that the raw data is not impacted by this in any way, meaning you can still see the values in the raw data, but they will not be visualized on the platform.**

A single call to `logEvent` should not have more than 1000 event properties. Likewise a single call to `setUserProperties` should not have more than 1000 user properties. If the 1000 item limit is exceeded then the properties will be dropped and a warning will be logged. We have put in very conservative estimates for the event and property caps which we don’t expect to be exceeded in any practical use case. If you feel that your use case will go above those limits please reach out to support@amplitude.com.

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

3. You can also log Identify events as out of session by setting input parameter `outOfSession` to `true` when calling `identify()`:

    ```java
    Identify identify = new Identify().set("key", "value);
    Amplitude.getInstance().identify(identify, true);
    ```

### Getting the Session Id ###

You can use the helper method `getSessionId` to get the value of the current sessionId:
```java
long sessionId = Amplitude.getInstance().getSessionId();
```

# Setting Custom User IDs #

If your app has its own login system that you want to track users with, you can call `setUserId()` at any time:

```java
Amplitude.getInstance().setUserId("USER_ID_HERE");
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

6. `prepend`: this will prepend a value or values to a user property. Prepend means inserting the value(s) at the front of a given list. If the user property does not have a value set yet, it will be initialized to an empty list before the new values are prepended. If the user property has an existing value and it is not a list, it will be converted into a list with the new value prepended.

    ```java
    Identify identify = new Identify().prepend("ab-tests", "new-user-test").prepend("some_list", new JSONArray().put(1).put("some_string"));
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

### Clearing User Properties with `clearUserProperties` ###

You may use `clearUserProperties` to clear all user properties at once. Note: the result is irreversible!

```java
Amplitude.getInstance().clearUserProperties();
```

# Tracking Revenue #

The preferred method of tracking revenue for a user now is to use `logRevenueV2()` in conjunction with the provided `Revenue` interface. `Revenue` instances will store each revenue transaction and allow you to define several special revenue properties (such as revenueType, productId, etc) that are used in Amplitude dashboard's Revenue tab. You can now also add event properties to the revenue event, via the eventProperties field. These `Revenue` instance objects are then passed into `logRevenueV2` to send as revenue events to Amplitude servers. This allows us to automatically display data relevant to revenue on the Amplitude website, including average revenue per daily active user (ARPDAU), 1, 7, 14, 30, 60, and 90 day revenue, lifetime value (LTV) estimates, and revenue by advertising campaign cohort and daily/weekly/monthly cohorts.

**Important Note**: Amplitude currently does not support currency conversion. All revenue data should be normalized to your currency of choice, before being sent to Amplitude.

To use the `Revenue` interface, you will first need to import the class:
```java
import com.amplitude.api.Revenue;
```

Each time a user generates revenue, you create a `Revenue` object and fill out the revenue properties:
```java
Revenue revenue = new Revenue().setProductId("com.company.productId").setPrice(3.99).setQuantity(3);
Amplitude.getInstance().logRevenueV2(revenue);
```

`price` is a required field. `quantity` defaults to 1 if unspecified. `productId`, `receipt` and `receiptSignature` are required if you want to verify the revenue event. Each field has a corresponding `set` method (for example `setProductId`, `setQuantity`, etc). This table describes the different fields available:

| Name               | Type       | Description                                                                                              | default |
|--------------------|------------|----------------------------------------------------------------------------------------------------------|---------|
| productId          | String     | Optional: an identifier for the product (we recommend something like the Google Play Store product Id)   | null    |
| quantity           | int        | Required: the quantity of products purchased. Defaults to 1 if not specified. Revenue = quantity * price | 1       |
| price              | Double     | Required: the price of the products purchased (can be negative). Revenue = quantity * price              | null    |
| revenueType        | String     | Optional: the type of revenue (ex: tax, refund, income)                                                  | null    |
| receipt            | String     | Optional: required if you want to verify the revenue event                                               | null    |
| receiptSignature   | String     | Optional: required if you want to verify the revenue event                                               | null    |
| eventProperties    | JSONObject | Optional: a JSONObject of event properties to include in the revenue event                               | null    |

Note: the price can be negative, which might be useful for tracking revenue lost, for example refunds or costs. Also note, you can set event properties on the revenue event just as you would with logEvent by passing in a JSONObject of string key value pairs. These event properties, however, will only appear in the Event Segmentation tab, not in the Revenue tab.

### Revenue Verification ###

By default Revenue events recorded on the Android SDK appear in Amplitude dashboards as unverified revenue events. **To enable revenue verification, copy your Google Play License Public Key into the manage section of your app on Amplitude. You must put a key for every single app in Amplitude where you want revenue verification.**

Then after a successful purchase transaction, add the purchase data and receipt signature to the `Revenue` object:

```java
// for a purchase request onActivityResult
String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

Revenue revenue = new Revenue().setProductId("com.company.productId").setQuantity(1);
revenue.setPrice(3.99).setReceipt(purchaseData, dataSignature);

Amplitude.getInstance().logRevenueV2(revenue);
```

See the [Google In App Billing Documentation](http://developer.android.com/google/play/billing/billing_integrate.html#Purchase) for details on how to retrieve the purchase data and receipt signature.

#### Amazon Store Revenue Verification

For purchases on the Amazon Store, you should copy your Amazon Shared Secret into the manage section of your app on Amplitude. After a successful purchase transaction, you should send the purchase token as the receipt and the user id as the receiptSignature:

```java
// for a purchase request onActivityResult
String purchaseToken = purchaseResponse.getReceipt();
String userId = getUserIdResponse.getUserId();

Revenue revenue = new Revenue().setProductId("com.company.productId").setQuantity(1);
revenue.setPrice(3.99).setReceipt(purchaseToken, userId);

Amplitude.getInstance().logRevenueV2(revenue);
```

### Backwards compatibility ###

The existing `logRevenue` methods still work but are deprecated. Fields such as `revenueType` will be missing from events logged with the old methods, so Revenue segmentation on those events will be limited in Amplitude dashboards.


# Fine-grained location tracking #

Amplitude can access the Android location service (if possible) to add the specific coordinates (longitude and latitude) where an event is logged. This behaviour is enabled by default, but can be adjusted calling the following methods *after* initializing:

```java
Amplitude.getInstance().enableLocationListening();
Amplitude.getInstance().disableLocationListening();
```

Even disabling the location listening, the events will have the "country" property filled. That property is retrieved from other sources (i.e. network or device locale).

# Allowing Users to Opt Out

To stop all event and session logging for a user, call setOptOut:

```java
Amplitude.getInstance().setOptOut(true);
```

Logging can be restarted by calling setOptOut again with enabled set to false.
No events will be logged during any period opt out is enabled.

# Advanced #
If you want to use the source files directly, you can [download them here](https://github.com/amplitude/Amplitude-Android/archive/master.zip). To include them in your project, extract the files, and then copy the five *.java files into your Android project.

This SDK automatically grabs useful data from the phone, including app version, phone model, operating system version, and carrier information. If your app has location permissions, the SDK will also grab the last known location of a user (this will not consume any extra battery, as it does not poll for a new location).

### Setting Groups ###

Amplitude supports assigning users to groups, and performing queries such as Count by Distinct on those groups. An example would be if you want to group your users based on what organization they are in by using an orgId. You can designate Joe to be in orgId 10, while Sue is in orgId 15. When performing an event segmentation query, you can then select Count by Distinct orgIds to query the number of different orgIds that have performed a specific event. As long as at least one member of that group has performed the specific event, that group will be included in the count. See our help article on [Count By Distinct](https://amplitude.zendesk.com/hc/en-us/articles/218824237) for more information.

When setting groups you need to define a `groupType` and `groupName`(s). In the above example, 'orgId' is a `groupType`, and the value 10 or 15 is the `groupName`. Another example of a `groupType` could be 'sport' with `groupNames` like 'tennis', 'baseball', etc.

You can use `setGroup(groupType, groupName)` to designate which groups a user belongs to. Note: this will also set the `groupType`: `groupName` as a user property. **This will overwrite any existing groupName value set for that user's groupType, as well as the corresponding user property value.** `groupType` is a string, and `groupName` can be either a string or an *JSONArray* of strings to indicate a user being in multiple groups (for example Joe is in orgId 10 and 16, so the `groupName` would be [10, 16]). It is important that you use JSONArrays to set multiple groups since primitive arrays do not serialize properly.

You can also call `setGroup` multiple times with different groupTypes to track multiple types of groups. **You are allowed to track up to 5 different groupTypes per app.** For example Sue is in orgId: 15, and she also plays sport: soccer. Now when querying, you can Count by Distinct on both orgId and sport (although as separate queries). Any additional groupTypes after the limit will be ignored from the Count By Distinct query UI, although they will still be saved as user properties.

```java
Amplitude.getInstance().setGroup("orgId", "15");
Amplitude.getInstance().setGroup("sport", new JSONArray().put("tennis").put("soccer"));  // list values
```

You can also use `logEvent` with the groups argument to set event-level groups, meaning the group designation only applies for the specific event being logged and does not persist on the user (unless you explicitly set it with `setGroup`).

```java
JSONObject eventProperties = new JSONObject().put("key", "value");
JSONObject groups = new JSONObject().put("orgId", 10);

Amplitude.getInstance().logEvent("initialize_game", eventProperties, groups);
```

### Custom Device Ids ###
By default, device IDs are a randomly generated UUID. If you would like to use Google's Advertising ID as the device ID, you can specify this by calling `Amplitude.getInstance().useAdvertisingIdForDeviceId()` prior to initializing. You can retrieve the Device ID that Amplitude uses with `Amplitude.getInstance().getDeviceId()`. This method can return null if a Device ID hasn't been generated yet.

If you have your own system for tracking device IDs and would like to set a custom device ID, you can do so with `Amplitude.getInstance().setDeviceId("CUSTOM_DEVICE_ID")`. **Note: this is not recommended unless you really know what you are doing.** Make sure the device ID you set is sufficiently unique (we recommend something like a UUID - we use `UUID.randomUUID().toString()`) to prevent conflicts with other devices in our system.

### SSL Pinning ###
The SDK includes support for SSL pinning, but it is undocumented and recommended against unless you have a specific need. Please contact Amplitude support before you ship any products with SSL pinning enabled so that we are aware and can provide documentation and implementation help.

### SDK Logging ###
You can disable all logging done in the SDK by calling `Amplitude.getInstance().enableLogging(false)`. By default the logging level is Log.INFO, meaning info messages, errors, and asserts are logged, but verbose and debug messages are not. You can change the logging level, for example to enable debug messages you can do `Amplitude.getInstance().setLogLevel(Log.DEBUG)`.
