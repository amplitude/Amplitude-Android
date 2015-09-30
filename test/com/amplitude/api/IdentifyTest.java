package com.amplitude.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
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
        assertTrue(compareJSONObjects(expected, identify.userPropertiesOperations));
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

        Identify identify = new Identify().set(property1, value1).set(property2, value2);
        identify.set(property3, value3).set(property4, value4);

        // identify should ignore this since duplicate key
        identify.set(property1, value3);

        JSONObject expected = new JSONObject();
        JSONObject expectedOperations = new JSONObject().put(property1, value1);
        expectedOperations.put(property2, value2).put(property3, value3).put(property4, value4);
        expected.put(Constants.AMP_OP_SET, expectedOperations);
        assertTrue(compareJSONObjects(expected, identify.userPropertiesOperations));
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

        Identify identify = new Identify().setOnce(property1, value1).setOnce(property2, value2);
        identify.setOnce(property3, value3).setOnce(property4, value4);

        // identify should ignore this since duplicate key
        identify.setOnce(property1, value3);

        JSONObject expected = new JSONObject();
        JSONObject expectedOperations = new JSONObject().put(property1, value1);
        expectedOperations.put(property2, value2).put(property3, value3).put(property4, value4);
        expected.put(Constants.AMP_OP_SET_ONCE, expectedOperations);
        assertTrue(compareJSONObjects(expected, identify.userPropertiesOperations));
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


        Identify identify = new Identify().add(property1, value1).add(property2, value2);
        identify.add(property3, value3).add(property4, value4);

        // identify should ignore this since duplicate key
        identify.add(property1, value3);

        JSONObject expected = new JSONObject();
        JSONObject expectedOperations = new JSONObject().put(property1, value1);
        expectedOperations.put(property2, value2).put(property3, value3).put(property4, value4);
        expected.put(Constants.AMP_OP_ADD, expectedOperations);
        assertTrue(compareJSONObjects(expected, identify.userPropertiesOperations));
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

        Identify identify = new Identify().setOnce(property1, value1).add(property2, value2);
        identify.set(property3, value3).unset(property4);

        // identify should ignore this since duplicate key
        identify.set(property4, value3);

        JSONObject expected = new JSONObject();
        expected.put(Constants.AMP_OP_SET_ONCE, new JSONObject().put(property1, value1));
        expected.put(Constants.AMP_OP_ADD, new JSONObject().put(property2, value2));
        expected.put(Constants.AMP_OP_SET, new JSONObject().put(property3, value3));
        expected.put(Constants.AMP_OP_UNSET, new JSONObject().put(property4, "-"));
        assertTrue(compareJSONObjects(expected, identify.userPropertiesOperations));
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
        assertTrue(compareJSONObjects(expected, identify.userPropertiesOperations));
    }
}
