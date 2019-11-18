package com.amplitude.api;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@Config(manifest = Config.NONE)
public class IdentifyTest extends BaseTest {

    @Before
    public void setUp() throws Exception { setUp(false); }

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testUnsetProperty() throws JSONException {
        String property1 = "testProperty1";
        String property2 = "testProperty2";
        Identify identify = new Identify().unset(property1).unset(property2).unset(property1);

        JSONObject expected = new JSONObject();
        JSONObject expectedOperations = new JSONObject().put(property1, "-").put(property2, "-");
        expected.put(Constants.AMP_OP_UNSET, expectedOperations);
        assertTrue(Utils.compareJSONObjects(expected, identify.userPropertiesOperations));
    }

    @Test
    public void testSetProperty() throws JSONException {
        String property1 = "string value";
        String value1 = "testValue";

        String property2 = "double value";
        double value2 = 0.123;

        String property3 = "boolean value";
        boolean value3 = true;

        String property4 = "json value";
        JSONObject value4 = new JSONObject();

        String property5 = "boolean array";
        boolean[] value5 = new boolean[]{true, true, false};
        JSONArray value5Expected = new JSONArray();
        for (boolean value : value5) value5Expected.put(value);

        Identify identify = new Identify().set(property1, value1).set(property2, value2);
        identify.set(property3, value3).set(property4, value4).set(property5, value5);

        // identify should ignore this since duplicate key
        identify.set(property1, value3);

        JSONObject expected = new JSONObject();
        JSONObject expectedOperations = new JSONObject().put(property1, value1);
        expectedOperations.put(property2, value2).put(property3, value3).put(property4, value4);
        expectedOperations.put(property5, value5Expected);
        expected.put(Constants.AMP_OP_SET, expectedOperations);
        assertTrue(Utils.compareJSONObjects(expected, identify.userPropertiesOperations));
    }

    @Test
    public void testSetOnceProperty() throws JSONException {
        String property1 = "string value";
        String value1 = "testValue";

        String property2 = "double value";
        double value2 = 0.123;

        String property3 = "boolean value";
        boolean value3 = true;

        String property4 = "json value";
        JSONObject value4 = new JSONObject();

        String property5 = "double array";
        double[] value5 = new double[]{1.2, 2.3, 3.4};
        JSONArray value5Expected = new JSONArray();
        for (double value : value5) value5Expected.put(value);

        Identify identify = new Identify().setOnce(property1, value1).setOnce(property2, value2);
        identify.setOnce(property3, value3).setOnce(property4, value4).setOnce(property5, value5);

        // identify should ignore this since duplicate key
        identify.setOnce(property1, value3);

        JSONObject expected = new JSONObject();
        JSONObject expectedOperations = new JSONObject().put(property1, value1);
        expectedOperations.put(property2, value2).put(property3, value3).put(property4, value4);
        expectedOperations.put(property5, value5Expected);
        expected.put(Constants.AMP_OP_SET_ONCE, expectedOperations);
        assertTrue(Utils.compareJSONObjects(expected, identify.userPropertiesOperations));
    }

    @Test
    public void testAddProperty() throws JSONException {
        String property1 = "int value";
        int value1 = 5;

        String property2 = "double value";
        double value2 = 0.123;

        String property3 = "float value";
        double value3 = 0.625; // floats are actually promoted to long in JSONObject

        String property4 = "long value";
        long value4 = 18l;

        String property5 = "string value";
        String value5 = "19";

        Identify identify = new Identify().add(property1, value1).add(property2, value2);
        identify.add(property3, value3).add(property4, value4).add(property5, value5);

        // identify should ignore this since duplicate key
        identify.add(property1, value3);

        JSONObject expected = new JSONObject();
        JSONObject expectedOperations = new JSONObject().put(property1, value1);
        expectedOperations.put(property2, value2).put(property3, value3).put(property4, value4);
        expectedOperations.put(property5, value5);
        expected.put(Constants.AMP_OP_ADD, expectedOperations);
        assertTrue(Utils.compareJSONObjects(expected, identify.userPropertiesOperations));
    }

    @Test
    public void testAppendProperty() throws JSONException {
        String property1 = "int value";
        int value1 = 5;

        String property2 = "double value";
        double value2 = 0.123;

        String property3 = "float value";
        double value3 = 0.625; // floats are actually promoted to long in JSONObject

        String property4 = "long value";
        long value4 = 18l;

        String property5 = "array value";
        JSONArray value5 = new JSONArray();
        value5.put(1);
        value5.put(2);
        value5.put(3);

        String property6 = "float array";
        float[] value6 = new float[]{(float)1.2, (float)2.3, (float)3.4, (float)4.5};
        JSONArray value6Expected = new JSONArray();
        for (float value : value6) value6Expected.put(value);

        String property7 = "int array";
        int[] value7 = new int[]{10, 12, 14, 17};
        JSONArray value7Expected = new JSONArray();
        for (int value : value7) value7Expected.put(value);

        String property8 = "long array";
        long[] value8 = new long[]{20, 22, 24, 27};
        JSONArray value8Expected = new JSONArray();
        for (long value : value8) value8Expected.put(value);

        String property9 = "string array";
        String[] value9 = new String[]{"test1", "test2", "test3"};
        JSONArray value9Expected = new JSONArray();
        for (String value : value9) value9Expected.put(value);

        Identify identify = new Identify().append(property1, value1).append(property2, value2);
        identify.append(property3, value3).append(property4, value4).append(property5, value5);
        identify.append(property6, value6).append(property7, value7).append(property8, value8);
        identify.append(property9, value9);

        // identify should ignore this since duplicate key
        identify.add(property1, value3);

        JSONObject expected = new JSONObject();
        JSONObject expectedOperations = new JSONObject().put(property1, value1);
        expectedOperations.put(property2, value2).put(property3, value3).put(property4, value4);
        expectedOperations.put(property5, value5).put(property6, value6Expected);
        expectedOperations.put(property7, value7Expected).put(property8, value8Expected);
        expectedOperations.put(property9, value9Expected);
        expected.put(Constants.AMP_OP_APPEND, expectedOperations);
        assertTrue(Utils.compareJSONObjects(expected, identify.userPropertiesOperations));
    }

    @Test
    public void testPrependProperty() throws JSONException {
        String property1 = "int value";
        int value1 = 5;

        String property2 = "double value";
        double value2 = 0.123;

        String property3 = "float value";
        double value3 = 0.625; // floats are actually promoted to long in JSONObject

        String property4 = "long value";
        long value4 = 18l;

        String property5 = "array value";
        JSONArray value5 = new JSONArray();
        value5.put(1);
        value5.put(2);
        value5.put(3);

        String property6 = "float array";
        float[] value6 = new float[]{(float)1.2, (float)2.3, (float)3.4, (float)4.5};
        JSONArray value6Expected = new JSONArray();
        for (float value : value6) value6Expected.put(value);

        String property7 = "int array";
        int[] value7 = new int[]{10, 12, 14, 17};
        JSONArray value7Expected = new JSONArray();
        for (int value : value7) value7Expected.put(value);

        String property8 = "long array";
        long[] value8 = new long[]{20, 22, 24, 27};
        JSONArray value8Expected = new JSONArray();
        for (long value : value8) value8Expected.put(value);

        String property9 = "string array";
        String[] value9 = new String[]{"test1", "test2", "test3"};
        JSONArray value9Expected = new JSONArray();
        for (String value : value9) value9Expected.put(value);

        Identify identify = new Identify().prepend(property1, value1).prepend(property2, value2);
        identify.prepend(property3, value3).prepend(property4, value4).prepend(property5, value5);
        identify.prepend(property6, value6).prepend(property7, value7).prepend(property8, value8);
        identify.prepend(property9, value9);

        // identify should ignore this since duplicate key
        identify.add(property1, value3);

        JSONObject expected = new JSONObject();
        JSONObject expectedOperations = new JSONObject().put(property1, value1);
        expectedOperations.put(property2, value2).put(property3, value3).put(property4, value4);
        expectedOperations.put(property5, value5).put(property6, value6Expected);
        expectedOperations.put(property7, value7Expected).put(property8, value8Expected);
        expectedOperations.put(property9, value9Expected);
        expected.put(Constants.AMP_OP_PREPEND, expectedOperations);
        assertTrue(Utils.compareJSONObjects(expected, identify.userPropertiesOperations));
    }

    @Test
    public void testMultipleOperations() throws JSONException {
        String property1 = "string value";
        String value1 = "testValue";

        String property2 = "double value";
        double value2 = 0.123;

        String property3 = "boolean value";
        boolean value3 = true;

        String property4 = "json value";

        String property5 = "array value";
        JSONArray value5 = new JSONArray();
        value5.put(15);
        value5.put(25);

        String property6 = "int value";
        int value6 = 100;

        Identify identify = new Identify().setOnce(property1, value1).add(property2, value2);
        identify.set(property3, value3).unset(property4).append(property5, value5);
        identify.prepend(property6, value6);

        // identify should ignore this since duplicate key
        identify.set(property4, value3);

        JSONObject expected = new JSONObject();
        expected.put(Constants.AMP_OP_SET_ONCE, new JSONObject().put(property1, value1));
        expected.put(Constants.AMP_OP_ADD, new JSONObject().put(property2, value2));
        expected.put(Constants.AMP_OP_SET, new JSONObject().put(property3, value3));
        expected.put(Constants.AMP_OP_UNSET, new JSONObject().put(property4, "-"));
        expected.put(Constants.AMP_OP_APPEND, new JSONObject().put(property5, value5));
        expected.put(Constants.AMP_OP_PREPEND, new JSONObject().put(property6, value6));
        assertTrue(Utils.compareJSONObjects(expected, identify.userPropertiesOperations));
    }

    @Test
    public void testDisallowDuplicateProperties() throws JSONException {
        String property = "testProperty";
        String value1 = "testValue";
        double value2 = 0.123;
        boolean value3 = true;

        Identify identify = new Identify().setOnce(property, value1).add(property, value2);
        identify.set(property, value3).unset(property);

        JSONObject expected = new JSONObject();
        expected.put(Constants.AMP_OP_SET_ONCE, new JSONObject().put(property, value1));
        assertTrue(Utils.compareJSONObjects(expected, identify.userPropertiesOperations));
    }

    @Test
    public void testDisallowOtherOperationsOnClearAllIdentify() throws JSONException {
        String property = "testProperty";
        String value1 = "testValue";
        double value2 = 0.123;
        boolean value3 = true;

        Identify identify = new Identify().clearAll().setOnce(property, value1);
        identify.add(property, value2).set(property, value3).unset(property);

        JSONObject expected = new JSONObject();
        expected.put(Constants.AMP_OP_CLEAR_ALL, "-");
        assertTrue(Utils.compareJSONObjects(expected, identify.userPropertiesOperations));
    }

    @Test
    public void testDisallowClearAllOnIdentifysWithOtherOperations() throws JSONException {
        String property = "testProperty";
        String value1 = "testValue";
        double value2 = 0.123;
        boolean value3 = true;

        Identify identify = new Identify().setOnce(property, value1).add(property, value2);
        identify.set(property, value3).unset(property).clearAll();

        JSONObject expected = new JSONObject();
        expected.put(Constants.AMP_OP_SET_ONCE, new JSONObject().put(property, value1));
        assertTrue(Utils.compareJSONObjects(expected, identify.userPropertiesOperations));
    }

    @Test
    public void testGetUserPropertyOperations() throws JSONException {
        String property1 = "string value";
        String value1 = "testValue";

        String property2 = "double value";
        double value2 = 0.123;

        String property3 = "boolean value";
        boolean value3 = true;

        String property4 = "json value";

        String property5 = "array value";
        JSONArray value5 = new JSONArray();
        value5.put(15);
        value5.put(25);

        String property6 = "int value";
        int value6 = 100;

        Identify identify = new Identify().setOnce(property1, value1).add(property2, value2);
        identify.set(property3, value3).unset(property4).append(property5, value5);
        identify.prepend(property6, value6);

        // identify should ignore this since duplicate key
        identify.set(property4, value3);

        JSONObject expected = new JSONObject();
        expected.put(Constants.AMP_OP_SET_ONCE, new JSONObject().put(property1, value1));
        expected.put(Constants.AMP_OP_ADD, new JSONObject().put(property2, value2));
        expected.put(Constants.AMP_OP_SET, new JSONObject().put(property3, value3));
        expected.put(Constants.AMP_OP_UNSET, new JSONObject().put(property4, "-"));
        expected.put(Constants.AMP_OP_APPEND, new JSONObject().put(property5, value5));
        expected.put(Constants.AMP_OP_PREPEND, new JSONObject().put(property6, value6));
        assertTrue(Utils.compareJSONObjects(expected, identify.getUserPropertiesOperations()));
    }
}
