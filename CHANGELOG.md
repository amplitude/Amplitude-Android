## [2.40.3](https://github.com/amplitude/Amplitude-Android/compare/v2.40.2...v2.40.3) (2025-04-29)


### Bug Fixes

* default location listening to false ([#407](https://github.com/amplitude/Amplitude-Android/issues/407)) ([27d3758](https://github.com/amplitude/Amplitude-Android/commit/27d3758fd8427d44e15813356ada2e3234ad62b8))

## [2.40.2](https://github.com/amplitude/Amplitude-Android/compare/v2.40.1...v2.40.2) (2024-06-12)


### Bug Fixes

* catch all exceptions when trying to intercept identifies ([#402](https://github.com/amplitude/Amplitude-Android/issues/402)) ([f0350bf](https://github.com/amplitude/Amplitude-Android/commit/f0350bf5dff450fe34c1a9095b16082f2d6c5353))

## [2.40.1](https://github.com/amplitude/Amplitude-Android/compare/v2.40.0...v2.40.1) (2024-04-17)


### Bug Fixes

* always run middleware flush on updateServer ([#400](https://github.com/amplitude/Amplitude-Android/issues/400)) ([fbee357](https://github.com/amplitude/Amplitude-Android/commit/fbee357d6ba9eb540101cf09393f7eebafdfd49d))

# [2.40.0](https://github.com/amplitude/Amplitude-Android/compare/v2.39.9...v2.40.0) (2024-04-17)


### Features

* middleware session replay integration ([#399](https://github.com/amplitude/Amplitude-Android/issues/399)) ([28bbbe6](https://github.com/amplitude/Amplitude-Android/commit/28bbbe6ad2d0a0fe6424645e6105ecb8c2be7a4f))

## [2.39.9](https://github.com/amplitude/Amplitude-Android/compare/v2.39.8...v2.39.9) (2024-02-27)


### Bug Fixes

* move identifyInterceptor before eventBridge receiver is set ([#392](https://github.com/amplitude/Amplitude-Android/issues/392)) ([a6d4c9d](https://github.com/amplitude/Amplitude-Android/commit/a6d4c9d1ad8cfc99420e92ed39164feaf3e5eac1))

## [2.39.8](https://github.com/amplitude/Amplitude-Android/compare/v2.39.7...v2.39.8) (2023-07-21)


### Bug Fixes

* try to correctly handle session change for race condition ([#382](https://github.com/amplitude/Amplitude-Android/issues/382)) ([b0f4fea](https://github.com/amplitude/Amplitude-Android/commit/b0f4fea58d808129c0bd46a7624f8a1e541bc6c6))

## [2.39.7](https://github.com/amplitude/Amplitude-Android/compare/v2.39.6...v2.39.7) (2023-07-07)


### Bug Fixes

* try to reduce sqlite cursor memory usage ([#375](https://github.com/amplitude/Amplitude-Android/issues/375)) ([77e508d](https://github.com/amplitude/Amplitude-Android/commit/77e508d9b8cbaf36328f3176e89c04f502c606c6))

## [2.39.6](https://github.com/amplitude/Amplitude-Android/compare/v2.39.5...v2.39.6) (2023-07-06)


### Bug Fixes

* remove md5 usage ([#372](https://github.com/amplitude/Amplitude-Android/issues/372)) ([c849590](https://github.com/amplitude/Amplitude-Android/commit/c84959086f9a645f87a3175caaec3022154ee6bb))

## [2.39.5](https://github.com/amplitude/Amplitude-Android/compare/v2.39.4...v2.39.5) (2023-06-13)


### Bug Fixes

* do not fetch advertising Id if adid tracking is disabled ([#366](https://github.com/amplitude/Amplitude-Android/issues/366)) ([a1f8cc8](https://github.com/amplitude/Amplitude-Android/commit/a1f8cc80af0bbaf2b7edc3b47b87c3f518ea8433))

## [2.39.4](https://github.com/amplitude/Amplitude-Android/compare/v2.39.3...v2.39.4) (2023-06-01)


### Bug Fixes

* move inForeground reads/writes to main thread ([#362](https://github.com/amplitude/Amplitude-Android/issues/362)) ([15b4b35](https://github.com/amplitude/Amplitude-Android/commit/15b4b35187da132adeeb35e6726e5b76bc48f75f))

## [2.39.3](https://github.com/amplitude/Amplitude-Android/compare/v2.39.2...v2.39.3) (2023-04-29)


### Bug Fixes

* filter null value in identify user properties ([#356](https://github.com/amplitude/Amplitude-Android/issues/356)) ([779b6b3](https://github.com/amplitude/Amplitude-Android/commit/779b6b3246e01b0c408ecae7437b7f40c7067f22))
* update identify interceptor to identify only ([#357](https://github.com/amplitude/Amplitude-Android/issues/357)) ([afd3251](https://github.com/amplitude/Amplitude-Android/commit/afd3251f75c30b130a42805e49b31673916cedf1))

### As of September 21, 2020 CHANGELOG.md is no longer manually updated. 
Please check the [releases page](https://github.com/amplitude/Amplitude-Android/releases) for up to date changes.

## 2.28.2 (Sep 13, 2020)
* Add `setMinTimeBetweenSessionsMillis` in plugin for Unity Plugin to use.

## 2.28.1 (Aug 26, 2020)
* Add `setOffline` in plugin for Unity Plugin to use.

## 2.28.0 (Aug 10, 2020)

* Introducing useDynamicConfig flag!! Turning this flag on will find the best server url automatically based on users' geo location.
* Note 1. If you have your own proxy server and use setServerUrl API, please leave this OFF.
* Note 2. If you have users in China Mainland, we suggest you turn this on.
* Note 3. By default, this feature is OFF. So you need to explicitly set it to ON to use it.

## 2.27.0 (Jul 14, 2020)

* Added setServerUrl to `AmplitudePlugin` to enable it for Unity SDK too.
* Fix an issue during location fetching.

## 2.26.1 (Jun 15, 2020)

* Fix the incorrect behavior of `disableLocationListening`. If you want to disable location listening over LocationManager. Please call called before initialization.

## 2.26.0 (Jun 2, 2020)

* Remove ComodoRSA certificate for SSL pinning.

## 2.25.2 (May 13, 2020)

* Add 3 APIs to `AmplitudePlugin` (`uploadEvents`, `useAdvertisingIdForDeviceId`, `setDeviceId`)

## 2.25.1 (Apr 3, 2020)

* Remove the declaration of location related permissions in manifest file.

## 2.25.0 (Mar 17, 2020)

* Added APIs to `AmplitudeClient` to let users set library name and version. This should be only used when you develop your own library which wraps Amplitude Android SDK.

## 2.24.2 (Feb 5, 2020)

* Now you can set auth token! Use `AmplitudeClient#setBearerToken(String token)` please!

## 2.24.1 (Jan 28, 2020)

* Fix the issue that `version` property shows old version.

## 2.24.0 (Jan 28, 2020)

* Now you can enable or disable COPPA (Children's Online Privacy Protection Act) restrictions on ADID, city, IP address and location tracking. `AmplitudeClient#enableCoppaControl()` and `AmplitudeClient#disableCoppaControl()`

## 2.23.2 (Aug 05, 2019)

* Catch exceptions when fetching most recent location.

## 2.23.1 (Jul 19, 2019)

* Handle SQLite database crashes caused by fetching events that exceed 2MB (max size of cursor window).

## 2.23.0 (Apr 22, 2019)

* Make `startNewSessionIfNeeded` a public method. Only call this if you know what you are doing. This may trigger a new session to start.

## 2.22.1 (Mar 21, 2019)

* Store deviceId in SharedPreferences as backup in case SQLite database fails or becomes corrupted.

## 2.22.0 (Jan 18, 2019)

* Add ability to set a custom server URL for uploading events using `setServerUrl`.

## 2.21.0 (Dec 05, 2018)

* Update SDK to better handle when the SQLite database file gets corrupted between interactions.
* Add optional diagnostic logging that tracks exceptions thrown in the SDK and sends to Amplitude.

## 2.20.0 (Oct 15, 2018)

* Add ability to set group properties via a new `groupIdentify` method that takes in an `Identify` object as well as a group type and group name.

## 2.19.1 (Aug 14, 2018)

* Update SDK to better handle SQLite Exceptions.

## 2.19.0 (Jul 24, 2018)

* Add `TrackingOptions` interface to customize the automatic tracking of user properties in the SDK (such as language, ip_address, platform, etc). See [Help Center Documentation](https://amplitude.zendesk.com/hc/en-us/articles/115002935588#disable-automatic-tracking-of-properties) for instructions on setting up this configuration.

## 2.18.2 (Jul 24, 2018)

* Use randomly generated device id if user has limitAdTracking enabled.

## 2.18.1 (May 07, 2018)

* Updating to [OkHttp 3.10.0](https://github.com/square/okhttp/blob/master/CHANGELOG.md#version-3100)
* Lowering event upload max batch size from 100 to 50. This should help to avoid out of memory issues on Android devices with low memory.

## 2.18.0 (Apr 19, 2018)

* Added a `setUserId` method with optional boolean argument `startNewSession`, which when `true` starts a new session after changing the userId.

## 2.17.0 (Feb 05, 2018)

* Add ability to specify a custom `platform` value during initialization as an input argument. If the value is `null` or an empty string then `platform` will default to `Android`.

## 2.16.0 (Nov 27, 2017)

* Expose a public `getUserPropertiesOperations` method on the `Identify` class.
* Handle exceptions when the LocationManager is not available for fetching location.

## 2.15.0 (Oct 04, 2017)

* Updating to latest version of OkHttp3 ([3.9.0](https://github.com/square/okhttp/blob/master/CHANGELOG.md#version-390))

## 2.14.1 (Jul 27, 2017)

* Switch to an internal implementation of `isEmptyString` instead of Android TextUtils.

## 2.14.0 (Jul 05, 2017)

* Add support for logging events to multiple Amplitude apps. See our [Help Center Documentation](https://amplitude.zendesk.com/hc/en-us/articles/115002935588#logging-events-to-multiple-projects) for details.

## 2.13.4 (May 09, 2017)

* Handle exceptions when fetching device carrier information. Thanks to @fkam-tt for the pull request.
* Copy userProperties on main thread in `setUserProperties` to prevent ConcurrentModificationExceptions.
* Migrating setup instructions and SDK documentation in the README file to Zendesk articles.

## 2.13.3 (Mar 13, 2017)

* Handle exceptions when reading from database. Only affects certain Fairphone and LG devices.
* Handle exceptions when building request to upload event data. Only affects certain Lenovo devices.

## 2.13.2 (Dec 22, 2016)

* Fix crash when pulling null unsent event strings during upload.
* Fix bug where unserializable events were being saved to unsent events table.
* Added more logging around JSON serialization errors when logging events.

## 2.13.1 (Dec 15, 2016)

* Fix bug where `regenerateDeviceId` was not being run on background thread. DeviceInfo.generateUUID() should be a static method.

## 2.13.0 (Dec 05, 2016)

* Add helper method to regenerate a new random deviceId. This can be used in conjunction with `setUserId(null)` to anonymize a user after they log out. Note this is not recommended unless you know what you are doing. See [Readme](https://github.com/amplitude/Amplitude-Android#logging-out-and-anonymous-users) for more information.

## 2.12.0 (Nov 07, 2016)

* Allow `logEvent` with a custom timestamp (milliseconds since epoch). See [documentation](https://rawgit.com/amplitude/Amplitude-Android/master/javadoc/com/amplitude/api/AmplitudeClient.html#logEvent-java.lang.String-org.json.JSONObject-org.json.JSONObject-org.json.JSONObject-org.json.JSONObject-long-boolean-) for more details.

## 2.11.0 (Oct 26, 2016)

* Add ability to log identify events outOfSession, this is useful for updating user properties without triggering session-handling logic. See [Readme](https://github.com/amplitude/Amplitude-Android#tracking-sessions) for more information.

## 2.10.0 (Oct 12, 2016)

* Catch and handle `CursorWindowAllocationException` thrown when the SDK is querying from the SQLite DB when app memory is low. If the exception is caught during `initialize`, then it is treated as if `initialize` was never called. If the exception is caught during the uploading of unsent events, then the upload is deferred to a later time.
* Block event property and user property dictionaries that have more than 1000 items. This is to block properties that are set unintentionally (for example in a loop). A single call to `logEvent` should not have more than 1000 event properties. Similarly a single call to `setUserProperties` should not have more than 1000 user properties.
* Handle IllegalArgumentException thrown by Android Geocoder for bad lat / lon values.

## 2.9.2 (Jul 14, 2016)

* Fix bug where `enableLocationListening` and `disableLocationListening` were not being run on background thread. Thanks to @elevenfive for PR.
* Update `Revenue` class to expose public `equals` and `hashCode` methods.

## 2.9.1 (Jul 11, 2016)

* Fix bug where `setOptOut` was not being run on background thread.
* `productId` is no longer a required field for `Revenue` logged via `logRevenueV2`.
* Fix bug where receipt and receiptSignature were being truncated if they were too long (exceeded 1024 characters).

## 2.9.0 (Jul 07, 2016)

* Add automatic flushing of unsent events on app close/minimize (through the Activity Lifecycle `onPause` callback). This only works if you call `Amplitude.getInstance().enableForegroundTracking(getApplication());`, which is recommended in the README by default for Setup. To disable you can call `Amplitude.getInstance().setFlushEventsOnClose(false);`

## 2.8.0 (Jun 29, 2016)

* Run the `initialize` logic on the background thread so that the SQLite database operations do not delay the main thread.
* Add support for Amazon Advertising ID (use in place of Google Advertising ID on Amazon devices). Thanks to @jcomo for the pull request.

## 2.7.2 (May 24, 2016)

* Add documentation for SDK functions. You can take a look [here](https://rawgit.com/amplitude/Amplitude-Android/master/javadoc/index.html). A link has also been added to the Readme.
* Fix bug where fetching the user's location on select devices throws a SecurityException, causing a crash.

## 2.7.1 (Apr 19, 2016)

* RevenueProperties is a confusing name and should actually be eventProperties. Deprecating Revenue.setRevenueProperties and replacing it with Revenue.setEventProperties, and clarified in Readme.

## 2.7.0 (Apr 19, 2016)

* Add support setting groups for users and events. See [Readme](https://github.com/amplitude/Amplitude-Android#setting-groups) for more information.
* Add helper method `getSessionId` to expose the current sessionId value.
* Add `logRevenueV2` and new `Revenue` class to support logging revenue events with properties, revenue type, and verified. See [Readme](https://github.com/amplitude/Amplitude-Android#tracking-revenue) for more info.
* Fix crash when trying to enableForegroundTracking with the PinnedAmplitudeClient. AmplitudeClient methods should be using `this` instead of static `instance` variable.

## 2.6.0 (Mar 29, 2016)

* Update to OKHttp v3.0.1.
* Add support for prepend user property operation.
* Fix bug where merging events for upload causes array index out of bounds exception.
* Migrate shared preferences (userId and event meta data) to Sqlite db to support apps with multiple processes.

## 2.5.1 (Mar 14, 2016)

* Fix bug where updateServer sets the wrong batchLimit when limit is false.

## 2.5.0 (Jan 15, 2016)

* Add ability to clear all user properties.
* Check that SDK is initialized when user calls enableForegroundTracking, identify, setUserProperties.

## 2.4.0 (Dec 15, 2015)

* Add support for append user property operation.

## 2.3.0 (Nov 30, 2015)

* Log if Google Play Services is enabled for the application.

## 2.2.0 (Oct 20, 2015)

* Removed all references to Apache HTTPClient to support Android M.
* Handle exceptions when fetching last known location from LocationManager.
* Add ability to set custom deviceId.
* Handle exception when cloning JSON object.
* Maintain only one instance of OKHttpClient.
* Add AmplitudeLog helper class that supports enabling and disabling of logging as well as setting of the log level.
* Fix bug where event and identify queues are not truncated if eventMaxCount is less than eventRemoveBatchSize.

## 2.1.0 (Oct 04, 2015)

* Add support for user properties operations (set, setOnce, add, unset).
* Fix bug where end session event was not being sent upon app reopen.

## 2.0.4 (Sep 23, 2015)

* Fix bug where deviceInfo was trying to use Geocoder if none present.

## 2.0.3 (Sep 22, 2015)

* Fix bug where deviceId was being fetched on main thread.

## 2.0.2 (Aug 24, 2015)

* Fix Maven jar, fixed build file.

## 2.0.1 (Aug 21, 2015)

* Catch all exceptions thrown by Android TelephonyManager and NullPointerExceptions thrown by geocoder during country lookup.

## 2.0.0 (Aug 20, 2015)

* Expose user ID with getUserId.
* Simplified session tracking. No longer need to call startSession and endSession. No longer send start/end session events by default. Added foreground tracking for sessions that uses Android activity lifecycles.
* The minimum supported API level is 9. API level 14 is required for foreground tracking.
* Always track Android advertising ID (ADID) regardless of limit ad tracking enabled.
* Track if limit ad tracking enabled as an API property for each logged event.
* Database upgraded to version 2: added a new store table for key value pairs.
* Device ID is now saved to and reloaded from the SQLite database (instead of SharedPrefs because SharedPrefs currently does not support multiple processes).
* MessageDigest.getInstance(String) is not threadsafe (known Android issue). Replaced with alternate MD5 implementation from http://org.rodage.com/pub/java/security/MD5.java.
* Create a copy of input userProperties JSONObject in setUserProperties to try and prevent ConcurrentModificationException.

## 1.7.0 (May 29, 2015)

* Enable configuration of eventUploadThreshold, eventMaxCount,
  eventUploadMaxBatchSize, eventUploadPeriodSeconds, minTimeBetweenSessionsMillis,
  and sessionTimeoutMillis.

## 1.6.3 (May 06, 2015)

* Add offline mode to turn off server uploading for a time.
* Add synchronous logging. Logs events to the DB synchronously to guarantee event persistence.

## 1.6.2 (Apr 17, 2015)

* Change protection on AmplitudeClient to public.

## 1.6.1 (Apr 13, 2015)

* Fix double class inclusion in jar distribution

## 1.6.0 (Apr 08, 2015)

* Fix crash under aggressive proguard optimizations.
* Fix device id being lost occasionally on app update.
* Fix exception when calling logEvent with empty JSONObject.
* Log a DEBUG message on each event.

## 1.5.0 (Mar 24, 2015)

* Add PinnedAmplitudeClient to support SSL pinning.
* Deprecate static methods on Amplitude. Switch to using Amplitude.getInstance().
* Upgrade HTTP client to okhttp.

## 1.4.6 (Mar 16, 2015)

* Fix bug when initializing with user id. Api key was not set properly.

## 1.4.4 (Mar 11, 2015)

* Expose setUserProperties(JSONObject, boolean) as a static
* Handle null edge cases in location request
* Add user opt out support
* Merge user properties in setUserProperties by default
* Refactor Amplitude to be a singleton to support tests
* Add option to disable fine-grained location tracking
* Fix crash: ConcurrentModificationException in HashMap
* Fix crash: CursorWindowAllocationException in SQLite

## 1.4.3 (Nov 13, 2014)

* Update field names, split platform and os, and send library information

## 1.4.2 (Nov 7, 2014)

* Don't log end session event if session isn't open
* Fix creating a new session id when the previous session id is invalid or non existant

## 1.4.1 (Jul 16, 2014)

* Hotfix extra class file in jar.

## 1.4.0 (Jul 1, 2014)

* Send androidADID with events
* Use Google Play Advertising ID instead of Android ID, if set. Default / fall back on using a random UUID
* Pull country from reverse geocode, then telephony network country, then locale

## 1.3.0 (Jun 4, 2014)

* Add getDeviceId to unity plugin
* Add additional logRevenue methods for receipt validation
* Make device ID public
* Fix bug where first event was getting skipped from upload
* Catch SQLiteExceptions
* Catch exceptions through by Apache HTTPClient

## 1.0.0 (May 1, 2014)

* Initial packaged release
