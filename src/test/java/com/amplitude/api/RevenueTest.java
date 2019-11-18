package com.amplitude.api;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@Config(manifest=Config.NONE)
public class RevenueTest extends BaseTest {

    @Before
    public void setUp() throws Exception { setUp(false); }

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testProductId() {
        Revenue revenue = new Revenue();
        assertNull(revenue.productId);

        String productId = "testProductId";
        revenue.setProductId(productId);
        assertEquals(revenue.productId, productId);

        // test that ignore empty inputs
        revenue.setProductId(null);
        assertEquals(revenue.productId, productId);
        revenue.setProductId("");
        assertEquals(revenue.productId, productId);

        JSONObject obj = revenue.toJSONObject();
        assertEquals(obj.optString("$productId"), productId);
    }

    @Test
    public void testQuantity() {
        Revenue revenue = new Revenue();
        assertEquals(revenue.quantity, 1);

        int quantity = 100;
        revenue.setQuantity(quantity);
        assertEquals(revenue.quantity, quantity);

        JSONObject obj = revenue.toJSONObject();
        assertEquals(obj.optInt("$quantity"), quantity);
    }

    @Test
    public void testPrice() {
        Revenue revenue = new Revenue();
        assertNull(revenue.price);

        double price = 10.99;
        revenue.setPrice(price);
        assertEquals(revenue.price.doubleValue(), price, 0);

        JSONObject obj = revenue.toJSONObject();
        assertEquals(obj.optDouble("$price"), price, 0);
    }

    @Test
    public void testRevenueType() {
        Revenue revenue = new Revenue();
        assertEquals(revenue.revenueType, null);

        String revenueType = "testRevenueType";
        revenue.setRevenueType(revenueType);
        assertEquals(revenue.revenueType, revenueType);

        // verify that null and empty strings allowed
        revenue.setRevenueType(null);
        assertNull(revenue.revenueType);
        revenue.setRevenueType("");
        assertEquals(revenue.revenueType, "");

        revenue.setRevenueType(revenueType);
        assertEquals(revenue.revenueType, revenueType);

        JSONObject obj = revenue.toJSONObject();
        assertEquals(obj.optString("$revenueType"), revenueType);
    }

    @Test
    public void testReceipt() {
        Revenue revenue = new Revenue();
        assertNull(revenue.receipt);
        assertNull(revenue.receiptSig);

        String receipt = "testReceipt";
        String receiptSig = "testReceiptSig";
        revenue.setReceipt(receipt, receiptSig);
        assertEquals(revenue.receipt, receipt);
        assertEquals(revenue.receiptSig, receiptSig);

        JSONObject obj = revenue.toJSONObject();
        assertEquals(obj.optString("$receipt"), receipt);
        assertEquals(obj.optString("$receiptSig"), receiptSig);
    }

    @Test
    public void testRevenueProperties() throws JSONException {
        Revenue revenue = new Revenue();
        assertNull(revenue.properties);

        JSONObject properties = new JSONObject().put("city", "san francisco");
        revenue.setRevenueProperties(properties);
        assertTrue(Utils.compareJSONObjects(properties, revenue.properties));

        JSONObject obj = revenue.toJSONObject();
        assertEquals(obj.optString("city"), "san francisco");
        assertEquals(obj.optInt("$quantity"), 1);

        // assert original json object was not modified
        assertFalse(properties.has("$quantity"));
    }

    @Test
    public void testEventProperties() throws JSONException {
        Revenue revenue = new Revenue();
        assertNull(revenue.properties);

        JSONObject properties = new JSONObject().put("city", "san francisco");
        revenue.setEventProperties(properties);
        assertTrue(Utils.compareJSONObjects(properties, revenue.properties));

        JSONObject obj = revenue.toJSONObject();
        assertEquals(obj.optString("city"), "san francisco");
        assertEquals(obj.optInt("$quantity"), 1);

        // assert original json object was not modified
        assertFalse(properties.has("$quantity"));
    }

    @Test
    public void testValidRevenue() {
        Revenue revenue = new Revenue();
        assertFalse(revenue.isValidRevenue());
        revenue.setProductId("testProductId");
        assertFalse(revenue.isValidRevenue());
        revenue.setPrice(10.99);
        assertTrue(revenue.isValidRevenue());

        Revenue revenue2 = new Revenue();
        assertFalse(revenue2.isValidRevenue());
        revenue2.setPrice(10.99);
        revenue2.setQuantity(15);
        assertTrue(revenue2.isValidRevenue());
        revenue2.setProductId("testProductId");
        assertTrue(revenue2.isValidRevenue());
    }

    @Test
    public void testToJSONObject() throws JSONException {
        double price = 10.99;
        int quantity = 15;
        String productId = "testProductId";
        String receipt = "testReceipt";
        String receiptSig = "testReceiptSig";
        String revenueType = "testRevenueType";
        JSONObject props = new JSONObject().put("city", "Boston");

        Revenue revenue = new Revenue().setProductId(productId).setPrice(price);
        revenue.setQuantity(quantity).setReceipt(receipt, receiptSig);
        revenue.setRevenueType(revenueType).setRevenueProperties(props);

        JSONObject obj = revenue.toJSONObject();
        assertEquals(obj.optDouble("$price"), price, 0);
        assertEquals(obj.optInt("$quantity"), 15);
        assertEquals(obj.optString("$productId"), productId);
        assertEquals(obj.optString("$receipt"), receipt);
        assertEquals(obj.optString("$receiptSig"), receiptSig);
        assertEquals(obj.optString("$revenueType"), revenueType);
        assertEquals(obj.optString("city"), "Boston");
    }

    @Test
    public void testEquals() throws JSONException {
        Revenue r1 = new Revenue();
        Revenue r2 = new Revenue();

        r1.setPrice(10.00).setQuantity(2).setProductId("testProductId");
        r1.setEventProperties(new JSONObject().put("testProp", "testValue"));
        r2.setPrice(9.99).setQuantity(2).setProductId("testProductId");
        r2.setEventProperties(new JSONObject().put("testProp", "testValue"));
        assertFalse(r1.equals(r2));
        r2.setPrice(10.00);
        assertTrue(r1.equals(r2));
        r2.setEventProperties(new JSONObject().put("testProp", "fakeValue"));
        assertFalse(r1.equals(r2));
    }

    @Test
    public void testHashCode() {
        Revenue r1 = new Revenue();
        Revenue r2 = new Revenue();

        r1.setPrice(10.00).setQuantity(2).setProductId("testProductId");
        r2.setPrice(9.99).setQuantity(2).setProductId("testProductId");
        assertNotEquals(r1.hashCode(), r2.hashCode());
        r2.setPrice(10.00);
        assertEquals(r1.hashCode(), r2.hashCode());
    }
}
