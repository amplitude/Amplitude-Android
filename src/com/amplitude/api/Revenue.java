package com.amplitude.api;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/*
 *   Wrapper class for revenue events and revenue properties
 *   Includes helper logic to determine if required fields are filled out, and toJSON function
 */
public class Revenue {

    public static final String TAG = "com.amplitude.api.Revenue";
    private static AmplitudeLog logger =  AmplitudeLog.getLogger();

    // required fields
    protected String productId = null;
    protected int quantity = 1;
    protected Double price = null;

    // optional fields
    protected String revenueType = null;
    protected String receipt = null;
    protected String receiptSig = null;
    protected JSONObject properties = null;

    // verify that current revenue object has required fields: productId, either price or revenue
    protected boolean isValidRevenue() {
        if (TextUtils.isEmpty(productId)) {
            logger.w(TAG, "Invalid revenue, need to set productId field");
            return false;
        }

        if (price == null) {
            logger.w(TAG, "Invalid revenue, need to set price");
            return false;
        }
        return true;
    }

    public Revenue setProductId(String productId) {
        if (TextUtils.isEmpty(productId)) {
            logger.w(TAG, "Invalid empty productId");
            return this;
        }
        this.productId = productId;
        return this;
    }

    public Revenue setQuantity(int quantity) {
        this.quantity = quantity;
        return this;
    }

    public Revenue setPrice(double price) {
        this.price = price;
        return this;
    }

    public Revenue setRevenueType(String revenueType) {
        this.revenueType = revenueType; // no input validation for optional field
        return this;
    }

    public Revenue setReceipt(String receipt, String receiptSignature) {
        this.receipt = receipt;
        this.receiptSig = receiptSignature;
        return this;
    }

    public Revenue setRevenueProperties(JSONObject revenueProperties) {
        this.properties = Utils.cloneJSONObject(revenueProperties);
        return this;
    }

    protected JSONObject toJSONObject() {
        JSONObject obj = properties == null ? new JSONObject() : properties;
        try {
            obj.put(Constants.AMP_REVENUE_PRODUCT_ID, productId);
            obj.put(Constants.AMP_REVENUE_QUANTITY, quantity);
            obj.put(Constants.AMP_REVENUE_PRICE, price);
            obj.put(Constants.AMP_REVENUE_REVENUE_TYPE, revenueType);
            obj.put(Constants.AMP_REVENUE_RECEIPT, receipt);
            obj.put(Constants.AMP_REVENUE_RECEIPT_SIG, receiptSig);
        } catch (JSONException e) {
            logger.e(
                TAG, String.format("Failed to convert revenue object to JSON: %s", e.toString())
            );
        }

        return obj;
    }
}
