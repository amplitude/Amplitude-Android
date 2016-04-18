package com.amplitude.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class RevenueTest extends BaseTest {

    @Before
    public void setUp() throws Exception { setUp(false); }

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testProductId() {
        Revenue revenue = new Revenue();
        assertEquals(revenue.productId, null);

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
        assertEquals(revenue.price, null);

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
        assertEquals(revenue.revenueType, null);
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
        assertEquals(revenue.receipt, null);
        assertEquals(revenue.receiptSig, null);

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
        assertEquals(revenue.properties, null);

        JSONObject properties = new JSONObject().put("city", "san francisco");
        revenue.setRevenueProperties(properties);
        assertEquals(compareJSONObjects(properties, revenue.properties), true);

        JSONObject obj = revenue.toJSONObject();
        assertEquals(obj.optString("city"), "san francisco");
        assertEquals(obj.optInt("$quantity"), 1);

        // assert original json object was not modified
        assertEquals(properties.has("$quantity"), false);
    }

    @Test
    public void testValidRevenue() {
        Revenue revenue = new Revenue();
        assertEquals(revenue.isValidRevenue(), false);
        revenue.setProductId("testProductId");
        assertEquals(revenue.isValidRevenue(), false);
        revenue.setPrice(10.99);
        assertEquals(revenue.isValidRevenue(), true);

        Revenue revenue2 = new Revenue();
        assertEquals(revenue2.isValidRevenue(), false);
        revenue2.setPrice(10.99);
        revenue2.setQuantity(15);
        assertEquals(revenue2.isValidRevenue(), false);
        revenue2.setProductId("testProductId");
        assertEquals(revenue2.isValidRevenue(), true);
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
}
