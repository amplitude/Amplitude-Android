## Unreleased

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
