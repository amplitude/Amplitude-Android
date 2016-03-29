## Unreleased

## 2.6.0 (March 29, 2016)

* Update to OKHttp v3.0.1.
* Add support for prepend user property operation.
* Fix bug where merging events for upload causes array index out of bounds exception.
* Migrate shared preferences (userId and event meta data) to Sqlite db to support apps with multiple processes.

## 2.5.1 (March 14, 2016)

* Fix bug where updateServer sets the wrong batchLimit when limit is false.

## 2.5.0 (January 15, 2016)

* Add ability to clear all user properties.
* Check that SDK is initialized when user calls enableForegroundTracking, identify, setUserProperties.

## 2.4.0 (December 15, 2015)

* Add support for append user property operation.

## 2.3.0 (November 30, 2015)

* Log if Google Play Services is enabled for the application.

## 2.2.0 (October 20, 2015)

* Removed all references to Apache HTTPClient to support Android M.
* Handle exceptions when fetching last known location from LocationManager.
* Add ability to set custom deviceId.
* Handle exception when cloning JSON object.
* Maintain only one instance of OKHttpClient.
* Add AmplitudeLog helper class that supports enabling and disabling of logging as well as setting of the log level.
* Fix bug where event and identify queues are not truncated if eventMaxCount is less than eventRemoveBatchSize.

## 2.1.0 (October 04, 2015)

* Add support for user properties operations (set, setOnce, add, unset).
* Fix bug where end session event was not being sent upon app reopen.

## 2.0.4 (September 23, 2015)

* Fix bug where deviceInfo was trying to use Geocoder if none present.

## 2.0.3 (September 22, 2015)

* Fix bug where deviceId was being fetched on main thread.

## 2.0.2 (August 24, 2015)

* Fix Maven jar, fixed build file.

## 2.0.1 (August 21, 2015)

* Catch all exceptions thrown by Android TelephonyManager and NullPointerExceptions thrown by geocoder during country lookup.

## 2.0.0 (August 20, 2015)

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

## 1.6.2 (April 17, 2015)

* Change protection on AmplitudeClient to public.

## 1.6.1 (April 13, 2015)

* Fix double class inclusion in jar distribution

## 1.6.0 (April 08, 2015)

* Fix crash under aggressive proguard optimizations.
* Fix device id being lost occasionally on app update.
* Fix exception when calling logEvent with empty JSONObject.
* Log a DEBUG message on each event.

## 1.5.0 (March 24, 2015)

* Add PinnedAmplitudeClient to support SSL pinning.
* Deprecate static methods on Amplitude. Switch to using Amplitude.getInstance().
* Upgrade HTTP client to okhttp.

## 1.4.6 (March 16, 2015)

* Fix bug when initializing with user id. Api key was not set properly.

## 1.4.4 (March 11, 2015)

* Expose setUserProperties(JSONObject, boolean) as a static
* Handle null edge cases in location request
* Add user opt out support
* Merge user properties in setUserProperties by default
* Refactor Amplitude to be a singleton to support tests
* Add option to disable fine-grained location tracking
* Fix crash: ConcurrentModificationException in HashMap
* Fix crash: CursorWindowAllocationException in SQLite

## 1.4.3 (November 13, 2014)

* Update field names, split platform and os, and send library information

## 1.4.2 (November 7, 2014)

* Don't log end session event if session isn't open
* Fix creating a new session id when the previous session id is invalid or non existant

## 1.4.1 (July 16, 2014)

* Hotfix extra class file in jar.

## 1.4.0 (July 1, 2014)

* Send androidADID with events
* Use Google Play Advertising ID instead of Android ID, if set. Default / fall back on using a random UUID
* Pull country from reverse geocode, then telephony network country, then locale

## 1.3.0 (June 4, 2014)

* Add getDeviceId to unity plugin
* Add additional logRevenue methods for receipt validation
* Make device ID public
* Fix bug where first event was getting skipped from upload
* Catch SQLiteExceptions
* Catch exceptions through by Apache HTTPClient

## 1.0.0 (May 1, 2014)

* Initial packaged release
