package com.amplitude.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 *  <h1>Identify</h1>
 *  Identify objects are a wrapper for user property operations. Each method adds a user
 *  property operation to the Identify object, and returns the same Identify object, allowing
 *  you to chain multiple method calls together, for example:
 *  {@code Identify identify = new Identify().set("color", "green").add("karma", 1);}
 *  <br><br>
 *  <b>Note:</b> if the same user property is used
 *  in multiple operations on a single Identify object, only the first operation on that
 *  property will be saved, and the rest will be ignored.
 *  <br><br>
 *  After creating an Identify object and setting the desired operations, send it to Amplitude
 *  servers by calling {@code Amplitude.getInstance().identify(identify);} and pass in the object.
 *
 *  @see <a href="https://github.com/amplitude/Amplitude-Android#user-properties-and-user-property-operations">
 *      Android SDK README</a> for more information on the Identify API and user property operations.
 */
public class Identify {

    /**
     * The class identifier tag used in logging. TAG = {@code "com.amplitude.api.Identify";}
     */
    private static final String TAG = Identify.class.getName();

    /**
     * Internal {@code JSONObject} to hold all of the user property operations.
     */
    protected JSONObject userPropertiesOperations = new JSONObject();
    /**
     * Internal set to keep track of user property keys and test for duplicates.
     */
    protected Set<String> userProperties = new HashSet<String>();

    /**
     * Sets a user property value only once. Subsequent @{code setOnce} operations on that user
     * property will be ignored.
     *
     * @param property the user property to setOnce
     * @param value    the value of the user property
     * @return the same Identify object
     */
    public Identify setOnce(String property, boolean value) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, value);
        return this;
    }

    /**
     * Sets a user property value only once. Subsequent @{code setOnce} operations on that user
     * property will be ignored.
     *
     * @param property the user property to setOnce
     * @param value    the value of the user property
     * @return the same Identify object
     */
    public Identify setOnce(String property, double value) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, value);
        return this;
    }

    /**
     * Sets a user property value only once. Subsequent @{code setOnce} operations on that user
     * property will be ignored.
     *
     * @param property the user property to setOnce
     * @param value    the value of the user property
     * @return the same Identify object
     */
    public Identify setOnce(String property, float value) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, value);
        return this;
    }

    /**
     * Sets a user property value only once. Subsequent @{code setOnce} operations on that user
     * property will be ignored.
     *
     * @param property the user property to setOnce
     * @param value    the value of the user property
     * @return the same Identify object
     */
    public Identify setOnce(String property, int value) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, value);
        return this;
    }

    /**
     * Sets a user property value only once. Subsequent @{code setOnce} operations on that user
     * property will be ignored.
     *
     * @param property the user property to setOnce
     * @param value    the value of the user property
     * @return the same Identify object
     */
    public Identify setOnce(String property, long value) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, value);
        return this;
    }

    /**
     * Sets a user property value only once. Subsequent @{code setOnce} operations on that user
     * property will be ignored.
     *
     * @param property the user property to setOnce
     * @param value    the value of the user property
     * @return the same Identify object
     */
    public Identify setOnce(String property, String value) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, value);
        return this;
    }

    /**
     * Sets a user property value only once. Subsequent @{code setOnce} operations on that user
     * property will be ignored.
     *
     * @param property the user property to setOnce
     * @param values    the value of the user property
     * @return the same Identify object
     */
    public Identify setOnce(String property, JSONArray values) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, values);
        return this;
    }

    /**
     * Sets a user property value only once. Subsequent @{code setOnce} operations on that user
     * property will be ignored.
     *
     * @param property the user property to setOnce
     * @param values    the value of the user property
     * @return the same Identify object
     */
    public Identify setOnce(String property, JSONObject values) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, values);
        return this;
    }

    /**
     * Sets a user property value only once. Subsequent @{code setOnce} operations on that user
     * property will be ignored.
     *
     * @param property the user property to setOnce
     * @param values    the value of the user property
     * @return the same Identify object
     */
    public Identify setOnce(String property, boolean[] values) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, booleanArrayToJSONArray(values));
        return this;
    }

    /**
     * Sets a user property value only once. Subsequent @{code setOnce} operations on that user
     * property will be ignored.
     *
     * @param property the user property to setOnce
     * @param values    the value of the user property
     * @return the same Identify object
     */
    public Identify setOnce(String property, double[] values) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, doubleArrayToJSONArray(values));
        return this;
    }

    /**
     * Sets a user property value only once. Subsequent @{code setOnce} operations on that user
     * property will be ignored.
     *
     * @param property the user property to setOnce
     * @param values    the value of the user property
     * @return the same Identify object
     */
    public Identify setOnce(String property, float[] values) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, floatArrayToJSONArray(values));
        return this;
    }

    /**
     * Sets a user property value only once. Subsequent @{code setOnce} operations on that user
     * property will be ignored.
     *
     * @param property the user property to setOnce
     * @param values    the value of the user property
     * @return the same Identify object
     */
    public Identify setOnce(String property, int[] values) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, intArrayToJSONArray(values));
        return this;
    }

    /**
     * Sets a user property value only once. Subsequent @{code setOnce} operations on that user
     * property will be ignored.
     *
     * @param property the user property to setOnce
     * @param values    the value of the user property
     * @return the same Identify object
     */
    public Identify setOnce(String property, long[] values) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, longArrayToJSONArray(values));
        return this;
    }

    /**
     * Sets a user property value only once. Subsequent @{code setOnce} operations on that user
     * property will be ignored.
     *
     * @param property the user property to setOnce
     * @param values    the value of the user property
     * @return the same Identify object
     */
    public Identify setOnce(String property, String[] values) {
        addToUserProperties(Constants.AMP_OP_SET_ONCE, property, stringArrayToJSONArray(values));
        return this;
    }


    /**
     * Sets a user property value. Existing values for that user property will be overwritten.
     *
     * @param property the user property to set
     * @param value    the value of the user property
     * @return the same Identify object
     */
    public Identify set(String property, boolean value) {
        addToUserProperties(Constants.AMP_OP_SET, property, value);
        return this;
    }

    /**
     * Sets a user property value. Existing values for that user property will be overwritten.
     *
     * @param property the user property to set
     * @param value    the value of the user property
     * @return the same Identify object
     */
    public Identify set(String property, double value) {
        addToUserProperties(Constants.AMP_OP_SET, property, value);
        return this;
    }

    /**
     * Sets a user property value. Existing values for that user property will be overwritten.
     *
     * @param property the user property to set
     * @param value    the value of the user property
     * @return the same Identify object
     */
    public Identify set(String property, float value) {
        addToUserProperties(Constants.AMP_OP_SET, property, value);
        return this;
    }

    /**
     * Sets a user property value. Existing values for that user property will be overwritten.
     *
     * @param property the user property to set
     * @param value    the value of the user property
     * @return the same Identify object
     */
    public Identify set(String property, int value) {
        addToUserProperties(Constants.AMP_OP_SET, property, value);
        return this;
    }

    /**
     * Sets a user property value. Existing values for that user property will be overwritten.
     *
     * @param property the user property to set
     * @param value    the value of the user property
     * @return the same Identify object
     */
    public Identify set(String property, long value) {
        addToUserProperties(Constants.AMP_OP_SET, property, value);
        return this;
    }

    /**
     * Sets a user property value. Existing values for that user property will be overwritten.
     *
     * @param property the user property to set
     * @param value    the value of the user property
     * @return the same Identify object
     */
    public Identify set(String property, String value) {
        addToUserProperties(Constants.AMP_OP_SET, property, value);
        return this;
    }

    /**
     * Sets a user property value. Existing values for that user property will be overwritten.
     *
     * @param property the user property to set
     * @param values    the value of the user property
     * @return the same Identify object
     */
    public Identify set(String property, JSONObject values) {
        addToUserProperties(Constants.AMP_OP_SET, property, values);
        return this;
    }

    /**
     * Sets a user property value. Existing values for that user property will be overwritten.
     *
     * @param property the user property to set
     * @param values    the value of the user property
     * @return the same Identify object
     */
    public Identify set(String property, JSONArray values) {
        addToUserProperties(Constants.AMP_OP_SET, property, values);
        return this;
    }

    /**
     * Sets a user property value. Existing values for that user property will be overwritten.
     *
     * @param property the user property to set
     * @param values    the value of the user property
     * @return the same Identify object
     */
    public Identify set(String property, boolean[] values) {
        addToUserProperties(Constants.AMP_OP_SET, property, booleanArrayToJSONArray(values));
        return this;
    }

    /**
     * Sets a user property value. Existing values for that user property will be overwritten.
     *
     * @param property the user property to set
     * @param values    the value of the user property
     * @return the same Identify object
     */
    public Identify set(String property, double[] values) {
        addToUserProperties(Constants.AMP_OP_SET, property, doubleArrayToJSONArray(values));
        return this;
    }

    /**
     * Sets a user property value. Existing values for that user property will be overwritten.
     *
     * @param property the user property to set
     * @param values    the value of the user property
     * @return the same Identify object
     */
    public Identify set(String property, float[] values) {
        addToUserProperties(Constants.AMP_OP_SET, property, floatArrayToJSONArray(values));
        return this;
    }

    /**
     * Sets a user property value. Existing values for that user property will be overwritten.
     *
     * @param property the user property to set
     * @param values    the value of the user property
     * @return the same Identify object
     */
    public Identify set(String property, int[] values) {
        addToUserProperties(Constants.AMP_OP_SET, property, intArrayToJSONArray(values));
        return this;
    }

    /**
     * Sets a user property value. Existing values for that user property will be overwritten.
     *
     * @param property the user property to set
     * @param values    the value of the user property
     * @return the same Identify object
     */
    public Identify set(String property, long[] values) {
        addToUserProperties(Constants.AMP_OP_SET, property, longArrayToJSONArray(values));
        return this;
    }

    /**
     * Sets a user property value. Existing values for that user property will be overwritten.
     *
     * @param property the user property to set
     * @param values    the value of the user property
     * @return the same Identify object
     */
    public Identify set(String property, String[] values) {
        addToUserProperties(Constants.AMP_OP_SET, property, stringArrayToJSONArray(values));
        return this;
    }


    /**
     * Increment a user property by some numerical value. If the user property does not have
     * a value set, it will be initialized to 0 before being incremented. Value can be
     * negative to decrement a user property value.
     *
     * @param property the user property to increment
     * @param value    the value (can be negative) to increment
     * @return the same Identify object
     */
    public Identify add(String property, double value) {
        addToUserProperties(Constants.AMP_OP_ADD, property, value);
        return this;
    }

    /**
     * Increment a user property by some numerical value. If the user property does not have
     * a value set, it will be initialized to 0 before being incremented. Value can be
     * negative to decrement a user property value.
     *
     * @param property the user property to increment
     * @param value    the value (can be negative) to increment
     * @return the same Identify object
     */
    public Identify add(String property, float value) {
        addToUserProperties(Constants.AMP_OP_ADD, property, value);
        return this;
    }

    /**
     * Increment a user property by some numerical value. If the user property does not have
     * a value set, it will be initialized to 0 before being incremented. Value can be
     * negative to decrement a user property value.
     *
     * @param property the user property to increment
     * @param value    the value (can be negative) to increment
     * @return the same Identify object
     */
    public Identify add(String property, int value) {
        addToUserProperties(Constants.AMP_OP_ADD, property, value);
        return this;
    }

    /**
     * Increment a user property by some numerical value. If the user property does not have
     * a value set, it will be initialized to 0 before being incremented. Value can be
     * negative to decrement a user property value.
     *
     * @param property the user property to increment
     * @param value    the value (can be negative) to increment
     * @return the same Identify object
     */
    public Identify add(String property, long value) {
        addToUserProperties(Constants.AMP_OP_ADD, property, value);
        return this;
    }

    /**
     * Increment a user property by some numerical value. If the user property does not have
     * a value set, it will be initialized to 0 before being incremented. Value can be
     * negative to decrement a user property value.
     *
     * @param property the user property to increment
     * @param value    the value (can be negative) to increment. Server-side we convert
     *                 the string into a number if possible.
     * @return the same Identify object
     */
    public Identify add(String property, String value) {
        addToUserProperties(Constants.AMP_OP_ADD, property, value);
        return this;
    }

    /**
     * Increment a user property by some numerical value. If the user property does not have
     * a value set, it will be initialized to 0 before being incremented. Value can be
     * negative to decrement a user property value.
     *
     * @param property the user property to increment
     * @param values    the value (can be negative) to increment. Server-side we flatten
     *                 dictionaries and apply add to each flattened property value.
     * @return the same Identify object
     */
    public Identify add(String property, JSONObject values) {
        addToUserProperties(Constants.AMP_OP_ADD, property, values);
        return this;
    }


    /**
     * Append a value or values to a user property. If the user property does not have a value
     * set, it will be initialized to an empty list before the new values are appended. If
     * the user property has an existing value and it is not a list, it will be converted into
     * a list with the new value(s) appended.
     *
     * @param property the user property property to which to append
     * @param value    the value being appended
     * @return the same Identify object
     */
    public Identify append(String property, boolean value) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, value);
        return this;
    }

    /**
     * Append a value or values to a user property. If the user property does not have a value
     * set, it will be initialized to an empty list before the new values are appended. If
     * the user property has an existing value and it is not a list, it will be converted into
     * a list with the new value(s) appended.
     *
     * @param property the user property property to which to append
     * @param value    the value being appended
     * @return the same Identify object
     */
    public Identify append(String property, double value) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, value);
        return this;
    }

    /**
     * Append a value or values to a user property. If the user property does not have a value
     * set, it will be initialized to an empty list before the new values are appended. If
     * the user property has an existing value and it is not a list, it will be converted into
     * a list with the new value(s) appended.
     *
     * @param property the user property property to which to append
     * @param value    the value being appended
     * @return the same Identify object
     */
    public Identify append(String property, float value) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, value);
        return this;
    }

    /**
     * Append a value or values to a user property. If the user property does not have a value
     * set, it will be initialized to an empty list before the new values are appended. If
     * the user property has an existing value and it is not a list, it will be converted into
     * a list with the new value(s) appended.
     *
     * @param property the user property property to which to append
     * @param value    the value being appended
     * @return the same Identify object
     */
    public Identify append(String property, int value) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, value);
        return this;
    }

    /**
     * Append a value or values to a user property. If the user property does not have a value
     * set, it will be initialized to an empty list before the new values are appended. If
     * the user property has an existing value and it is not a list, it will be converted into
     * a list with the new value(s) appended.
     *
     * @param property the user property property to which to append
     * @param value    the value being appended
     * @return the same Identify object
     */
    public Identify append(String property, long value) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, value);
        return this;
    }

    /**
     * Append a value or values to a user property. If the user property does not have a value
     * set, it will be initialized to an empty list before the new values are appended. If
     * the user property has an existing value and it is not a list, it will be converted into
     * a list with the new value(s) appended.
     *
     * @param property the user property property to which to append
     * @param value    the value being appended
     * @return the same Identify object
     */
    public Identify append(String property, String value) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, value);
        return this;
    }

    /**
     * Append a value or values to a user property. If the user property does not have a value
     * set, it will be initialized to an empty list before the new values are appended. If
     * the user property has an existing value and it is not a list, it will be converted into
     * a list with the new value(s) appended.
     *
     * @param property the user property property to which to append
     * @param values    the values being appended
     * @return the same Identify object
     */
    public Identify append(String property, JSONArray values) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, values);
        return this;
    }

    /**
     * Append a value or values to a user property. If the user property does not have a value
     * set, it will be initialized to an empty list before the new values are appended. If
     * the user property has an existing value and it is not a list, it will be converted into
     * a list with the new value(s) appended.
     *
     * @param property the user property property to which to append
     * @param values    the values being appended. Server-side we flatten dictionaries and apply
     *                  append to each flattened property.
     * @return the same Identify object
     */
    public Identify append(String property, JSONObject values) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, values);
        return this;
    }

    /**
     * Append a value or values to a user property. If the user property does not have a value
     * set, it will be initialized to an empty list before the new values are appended. If
     * the user property has an existing value and it is not a list, it will be converted into
     * a list with the new value(s) appended.
     *
     * @param property the user property property to which to append
     * @param values    the values being appended
     * @return the same Identify object
     */
    public Identify append(String property, boolean[] values) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, booleanArrayToJSONArray(values));
        return this;
    }

    /**
     * Append a value or values to a user property. If the user property does not have a value
     * set, it will be initialized to an empty list before the new values are appended. If
     * the user property has an existing value and it is not a list, it will be converted into
     * a list with the new value(s) appended.
     *
     * @param property the user property property to which to append
     * @param values    the values being appended
     * @return the same Identify object
     */
    public Identify append(String property, double[] values) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, doubleArrayToJSONArray(values));
        return this;
    }

    /**
     * Append a value or values to a user property. If the user property does not have a value
     * set, it will be initialized to an empty list before the new values are appended. If
     * the user property has an existing value and it is not a list, it will be converted into
     * a list with the new value(s) appended.
     *
     * @param property the user property property to which to append
     * @param values    the values being appended
     * @return the same Identify object
     */
    public Identify append(String property, float[] values) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, floatArrayToJSONArray(values));
        return this;
    }

    /**
     * Append a value or values to a user property. If the user property does not have a value
     * set, it will be initialized to an empty list before the new values are appended. If
     * the user property has an existing value and it is not a list, it will be converted into
     * a list with the new value(s) appended.
     *
     * @param property the user property property to which to append
     * @param values    the values being appended
     * @return the same Identify object
     */
    public Identify append(String property, int[] values) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, intArrayToJSONArray(values));
        return this;
    }

    /**
     * Append a value or values to a user property. If the user property does not have a value
     * set, it will be initialized to an empty list before the new values are appended. If
     * the user property has an existing value and it is not a list, it will be converted into
     * a list with the new value(s) appended.
     *
     * @param property the user property property to which to append
     * @param values    the values being appended
     * @return the same Identify object
     */
    public Identify append(String property, long[] values) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, longArrayToJSONArray(values));
        return this;
    }

    /**
     * Append a value or values to a user property. If the user property does not have a value
     * set, it will be initialized to an empty list before the new values are appended. If
     * the user property has an existing value and it is not a list, it will be converted into
     * a list with the new value(s) appended.
     *
     * @param property the user property property to which to append
     * @param values    the values being appended
     * @return the same Identify object
     */
    public Identify append(String property, String[] values) {
        addToUserProperties(Constants.AMP_OP_APPEND, property, stringArrayToJSONArray(values));
        return this;
    }


    /**
     * Prepend a value or values to a user property. Prepend means inserting the value(s) at the
     * front of a given list. if the user property does not have a value set, it will be
     * initialized to an empty list before the new values are prepended. If the user property
     * has an existing value and it is not a list, it will be converted into a list with the
     * new value(s) prepended.
     *
     * @param property the user property to which to append
     * @param value    the value being prepended
     * @return the same Identify object
     */
    public Identify prepend(String property, boolean value) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, value);
        return this;
    }

    /**
     * Prepend a value or values to a user property. Prepend means inserting the value(s) at the
     * front of a given list. if the user property does not have a value set, it will be
     * initialized to an empty list before the new values are prepended. If the user property
     * has an existing value and it is not a list, it will be converted into a list with the
     * new value(s) prepended.
     *
     * @param property the user property to which to append
     * @param value    the value being prepended
     * @return the same Identify object
     */
    public Identify prepend(String property, double value) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, value);
        return this;
    }

    /**
     * Prepend a value or values to a user property. Prepend means inserting the value(s) at the
     * front of a given list. if the user property does not have a value set, it will be
     * initialized to an empty list before the new values are prepended. If the user property
     * has an existing value and it is not a list, it will be converted into a list with the
     * new value(s) prepended.
     *
     * @param property the user property to which to append
     * @param value    the value being prepended
     * @return the same Identify object
     */
    public Identify prepend(String property, float value) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, value);
        return this;
    }

    /**
     * Prepend a value or values to a user property. Prepend means inserting the value(s) at the
     * front of a given list. if the user property does not have a value set, it will be
     * initialized to an empty list before the new values are prepended. If the user property
     * has an existing value and it is not a list, it will be converted into a list with the
     * new value(s) prepended.
     *
     * @param property the user property to which to append
     * @param value    the value being prepended
     * @return the same Identify object
     */
    public Identify prepend(String property, int value) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, value);
        return this;
    }

    /**
     * Prepend a value or values to a user property. Prepend means inserting the value(s) at the
     * front of a given list. if the user property does not have a value set, it will be
     * initialized to an empty list before the new values are prepended. If the user property
     * has an existing value and it is not a list, it will be converted into a list with the
     * new value(s) prepended.
     *
     * @param property the user property to which to append
     * @param value    the value being prepended
     * @return the same Identify object
     */
    public Identify prepend(String property, long value) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, value);
        return this;
    }

    /**
     * Prepend a value or values to a user property. Prepend means inserting the value(s) at the
     * front of a given list. if the user property does not have a value set, it will be
     * initialized to an empty list before the new values are prepended. If the user property
     * has an existing value and it is not a list, it will be converted into a list with the
     * new value(s) prepended.
     *
     * @param property the user property to which to append
     * @param value    the value being prepended
     * @return the same Identify object
     */
    public Identify prepend(String property, String value) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, value);
        return this;
    }

    /**
     * Prepend a value or values to a user property. Prepend means inserting the value(s) at the
     * front of a given list. if the user property does not have a value set, it will be
     * initialized to an empty list before the new values are prepended. If the user property
     * has an existing value and it is not a list, it will be converted into a list with the
     * new value(s) prepended.
     *
     * @param property the user property to which to append
     * @param values    the value being prepended
     * @return the same Identify object
     */
    public Identify prepend(String property, JSONArray values) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, values);
        return this;
    }

    /**
     * Prepend a value or values to a user property. Prepend means inserting the value(s) at the
     * front of a given list. if the user property does not have a value set, it will be
     * initialized to an empty list before the new values are prepended. If the user property
     * has an existing value and it is not a list, it will be converted into a list with the
     * new value(s) prepended.
     *
     * @param property the user property to which to append
     * @param values    the values being prepended. Server-side we flatten dictionaries and apply
     *                  prepend to each flattened property.
     * @return the same Identify object
     */
    public Identify prepend(String property, JSONObject values) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, values);
        return this;
    }

    /**
     * Prepend a value or values to a user property. Prepend means inserting the value(s) at the
     * front of a given list. if the user property does not have a value set, it will be
     * initialized to an empty list before the new values are prepended. If the user property
     * has an existing value and it is not a list, it will be converted into a list with the
     * new value(s) prepended.
     *
     * @param property the user property to which to append
     * @param values    the values being prepended
     * @return the same Identify object
     */
    public Identify prepend(String property, boolean[] values) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, booleanArrayToJSONArray(values));
        return this;
    }

    /**
     * Prepend a value or values to a user property. Prepend means inserting the value(s) at the
     * front of a given list. if the user property does not have a value set, it will be
     * initialized to an empty list before the new values are prepended. If the user property
     * has an existing value and it is not a list, it will be converted into a list with the
     * new value(s) prepended.
     *
     * @param property the user property to which to append
     * @param values    the values being prepended
     * @return the same Identify object
     */
    public Identify prepend(String property, double[] values) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, doubleArrayToJSONArray(values));
        return this;
    }

    /**
     * Prepend a value or values to a user property. Prepend means inserting the value(s) at the
     * front of a given list. if the user property does not have a value set, it will be
     * initialized to an empty list before the new values are prepended. If the user property
     * has an existing value and it is not a list, it will be converted into a list with the
     * new value(s) prepended.
     *
     * @param property the user property to which to append
     * @param values    the values being prepended
     * @return the same Identify object
     */
    public Identify prepend(String property, float[] values) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, floatArrayToJSONArray(values));
        return this;
    }

    /**
     * Prepend a value or values to a user property. Prepend means inserting the value(s) at the
     * front of a given list. if the user property does not have a value set, it will be
     * initialized to an empty list before the new values are prepended. If the user property
     * has an existing value and it is not a list, it will be converted into a list with the
     * new value(s) prepended.
     *
     * @param property the user property to which to append
     * @param values    the values being prepended
     * @return the same Identify object
     */
    public Identify prepend(String property, int[] values) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, intArrayToJSONArray(values));
        return this;
    }

    /**
     * Prepend a value or values to a user property. Prepend means inserting the value(s) at the
     * front of a given list. if the user property does not have a value set, it will be
     * initialized to an empty list before the new values are prepended. If the user property
     * has an existing value and it is not a list, it will be converted into a list with the
     * new value(s) prepended.
     *
     * @param property the user property to which to append
     * @param values    the values being prepended
     * @return the same Identify object
     */
    public Identify prepend(String property, long[] values) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, longArrayToJSONArray(values));
        return this;
    }

    /**
     * Prepend a value or values to a user property. Prepend means inserting the value(s) at the
     * front of a given list. if the user property does not have a value set, it will be
     * initialized to an empty list before the new values are prepended. If the user property
     * has an existing value and it is not a list, it will be converted into a list with the
     * new value(s) prepended.
     *
     * @param property the user property to which to append
     * @param values    the values being prepended
     * @return the same Identify object
     */
    public Identify prepend(String property, String[] values) {
        addToUserProperties(Constants.AMP_OP_PREPEND, property, stringArrayToJSONArray(values));
        return this;
    }


    /**
     * Unset and remove a user property.
     *
     * @param property the user property to unset and remove.
     * @return the same Identify object
     */
    public Identify unset(String property) {
        addToUserProperties(Constants.AMP_OP_UNSET, property, "-");
        return this;
    }


    /**
     * Clear all user properties. <b>Note:</b> the result is irreversible! <b>Also Note:</b>
     * clearAll needs to be be sent on its own Identify object without any other operations.
     *
     * @return the same Identify object.
     */
    public Identify clearAll() {
        if (userPropertiesOperations.length() > 0) {
            if (!userProperties.contains(Constants.AMP_OP_CLEAR_ALL)) {
                AmplitudeLog.getLogger().w(TAG, String.format(
                   "Need to send $clearAll on its own Identify object without any other " +
                   "operations, ignoring $clearAll"
                ));
            }
            return this;
        }

        try {
            userPropertiesOperations.put(Constants.AMP_OP_CLEAR_ALL, "-");
        } catch (JSONException e) {
            AmplitudeLog.getLogger().e(TAG, e.toString());
        }
        return this;
    }


    private void addToUserProperties(String operation, String property, Object value) {
        if (Utils.isEmptyString(property)) {
            AmplitudeLog.getLogger().w(TAG, String.format(
               "Attempting to perform operation %s with a null or empty string property, ignoring",
                operation
            ));
            return;
        }

        if (value == null) {
            AmplitudeLog.getLogger().w(TAG, String.format(
                "Attempting to perform operation %s with null value for property %s, ignoring",
                operation, property
            ));
            return;
        }

        // check that clearAll wasn't already used in this Identify
        if (userPropertiesOperations.has(Constants.AMP_OP_CLEAR_ALL)) {
            AmplitudeLog.getLogger().w(TAG, String.format(
                "This Identify already contains a $clearAll operation, ignoring operation %s",
                operation
            ));
            return;
        }

        // check if property already used in previous operation
        if (userProperties.contains(property)) {
            AmplitudeLog.getLogger().w(TAG, String.format(
                "Already used property %s in previous operation, ignoring operation %s",
                property, operation
            ));
            return;
        }

        try {
            if (!userPropertiesOperations.has(operation)) {
                userPropertiesOperations.put(operation, new JSONObject());
            }
            userPropertiesOperations.getJSONObject(operation).put(property, value);
            userProperties.add(property);
        } catch (JSONException e) {
            AmplitudeLog.getLogger().e(TAG, e.toString());
        }
    }

    private JSONArray booleanArrayToJSONArray(boolean[] values) {
        JSONArray array = new JSONArray();
        for (boolean value : values) array.put(value);
        return array;
    }

    private JSONArray floatArrayToJSONArray(float[] values) {
        JSONArray array = new JSONArray();
        for (float value : values) {
            try {
                array.put(value);
            } catch (JSONException e) {
                AmplitudeLog.getLogger().e(TAG, String.format(
                    "Error converting float %f to JSON: %s", value, e.toString()
                ));
            }
        }
        return array;
    }

    private JSONArray doubleArrayToJSONArray(double[] values) {
        JSONArray array = new JSONArray();
        for (double value : values) {
            try {
                array.put(value);
            } catch (JSONException e) {
                AmplitudeLog.getLogger().e(TAG, String.format(
                    "Error converting double %d to JSON: %s", value, e.toString()
                ));
            }
        }
        return array;
    }

    private JSONArray intArrayToJSONArray(int[] values) {
        JSONArray array = new JSONArray();
        for (int value : values) array.put(value);
        return array;
    }

    private JSONArray longArrayToJSONArray(long[] values) {
        JSONArray array = new JSONArray();
        for (long value : values) array.put(value);
        return array;
    }

    private JSONArray stringArrayToJSONArray(String[] values) {
        JSONArray array = new JSONArray();
        for (String value : values) array.put(value);
        return array;
    }

    /**
     * Sets user property.
     *
     * @param property the property
     * @param value    the value
     * @return the user property
     */
    Identify setUserProperty(String property, Object value) {
        addToUserProperties(Constants.AMP_OP_SET, property, value);
        return this;
    }

    /**
     * Sets a user property value only once. Subsequent @{code setOnce} operations on that user
     * property will be ignored. <b>Note:</b> this method has been deprecated. Please use one
     * with a different signature.
     *
     * @param property the user property to setOnce
     * @param value    the value of the user property
     * @return the same Identify object
     * @deprecated
     */
    public Identify setOnce(String property, Object value) {
        AmplitudeLog.getLogger().w(
            TAG,
            "This version of setOnce is deprecated. Please use one with a different signature."
        );
        return this;
    }

    /**
     * Sets a user property value. Existing values for that user property will be overwritten.
     * <b>Note:</b> this method has been deprecated. Please use one with a different signature.
     *
     * @param property the user property to set
     * @param value    the value of the user property
     * @return the same Identify object
     * @deprecated
     */
    public Identify set(String property, Object value) {
        AmplitudeLog.getLogger().w(
            TAG,
            "This version of set is deprecated. Please use one with a different signature."
        );
        return this;
    }

    /**
     * Public method that exposes the user property operations JSON blob.
      * @return a copy of the User Property Operations JSONObject. If copying fails, returns
     *      an empty JSONObject
     */
    public JSONObject getUserPropertiesOperations() {
        try {
            return new JSONObject(userPropertiesOperations.toString());
        } catch (JSONException e) {
            AmplitudeLog.getLogger().e(TAG, e.toString());
        }
        return new JSONObject();
    }
}
