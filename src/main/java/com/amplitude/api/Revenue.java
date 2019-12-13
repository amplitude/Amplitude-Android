package com.amplitude.api;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <h1>Revenue</h1>
 * Revenue objects are a wrapper for revenue events and revenue properties. This should be used
 * in conjunction with {@code AmplitudeClient.logRevenueV2()} to record in-app transactions.
 * Each set method returns the same Revenue object, allowing
 * you to chain multiple set calls together, for example:
 * {@code Revenue revenue = new Revenue().setProductId("com.product.id").setPrice(3.99);}
 * <br><br>
 * <b>Note:</b> {@code price} is a required field. If {@code quantity} is not
 * specified, it will default to 1. {@code productId}, {@code receipt} and {@code receiptSignature}
 * re required if you want to verify the revenue event.
 * <br><br>
 * <b>Note:</b> the total revenue amount is calculated as price * quantity.
 * <br><br>
 * After creating a Revenue object and setting the desired transaction properties, send it to
 * Amplitude servers by calling {@code Amplitude.getInstance().logRevenueV2(revenue);} and pass in
 * the object.
 *
 * @see <a href="https://github.com/amplitude/Amplitude-Android#tracking-revenue">
 *     Android SDK README</a> for more information on logging revenue.
 */
public class Revenue {
    /**
     * The class identifier tag used in logging. TAG = {@code "com.amplitude.api.Revenue"}
     */
    private static final String TAG = Revenue.class.getName();
    private static AmplitudeLog logger =  AmplitudeLog.getLogger();

    /**
     * The Product ID field.
     */
    protected String productId = null;
    /**
     * The Quantity field (defaults to 1).
     */
    protected int quantity = 1;
    /**
     * The Price field (required).
     */
    protected Double price = null;

    /**
     * The Revenue Type field (optional).
     */
    protected String revenueType = null;
    /**
     * The Receipt field (required if you want to verify the revenue event).
     */
    protected String receipt = null;
    /**
     * The Receipt Signature field (required if you want to verify the revenue event).
     */
    protected String receiptSig = null;
    /**
     * The Revenue Event Properties field (optional).
     */
    protected JSONObject properties = null;

    /**
     * Verifies that revenue object is valid and contains the required fields
     *
     * @return true if revenue object is valid, else false
     */
    protected boolean isValidRevenue() {
        if (price == null) {
            logger.w(TAG, "Invalid revenue, need to set price");
            return false;
        }
        return true;
    }

    /**
     * Set a value for the product identifier. Empty and invalid strings are ignored.
     *
     * @param productId the product id
     * @return the same Revenue object
     */
    public Revenue setProductId(String productId) {
        if (Utils.isEmptyString(productId)) {
            logger.w(TAG, "Invalid empty productId");
            return this;
        }
        this.productId = productId;
        return this;
    }

    /**
     * Set a value for the quantity. Note: revenue amount is calculated as price * quantity.
     *
     * @param quantity the quantity
     * @return the same Revenue object
     */
    public Revenue setQuantity(int quantity) {
        this.quantity = quantity;
        return this;
    }

    /**
     * Set a value for the price. Note: revenue amount is calculated as price * quantity.
     *
     * @param price the price
     * @return the same Revenue object
     */
    public Revenue setPrice(double price) {
        this.price = price;
        return this;
    }

    /**
     * Set a value for the revenue type.
     *
     * @param revenueType the revenue type
     * @return the same Revenue object
     */
    public Revenue setRevenueType(String revenueType) {
        this.revenueType = revenueType; // no input validation for optional field
        return this;
    }

    /**
     * Set the receipt and receipt signature. Both fields are required to verify the revenue event.
     *
     * @param receipt          the receipt
     * @param receiptSignature the receipt signature
     * @return the same Revenue object
     */
    public Revenue setReceipt(String receipt, String receiptSignature) {
        this.receipt = receipt;
        this.receiptSig = receiptSignature;
        return this;
    }

    /**
     * This is deprecated. RevenueProperties is a confusing name, should be EventProperties
     *
     * @param revenueProperties the revenue properties
     * @return the same Revenue object
     * @deprecated - use {@code Revenue.setEventProperties()} instead
     */
    public Revenue setRevenueProperties(JSONObject revenueProperties) {
        logger.w(TAG, "setRevenueProperties is deprecated, please use setEventProperties instead");
        return setEventProperties(revenueProperties);
    }

    /**
     * Set event properties for the revenue event, like you would for an event during logEvent.
     *
     * @param eventProperties the event properties
     * @return the same Revenue object
     * @see <a href="https://github.com/amplitude/Amplitude-Android#setting-event-properties">
     *     Event Properties</a> for more information about logging event properties.
     */
    public Revenue setEventProperties(JSONObject eventProperties) {
        this.properties = Utils.cloneJSONObject(eventProperties);
        return this;
    }

    /**
     * Converts Revenue object into a JSONObject to send to Amplitude servers
     *
     * @return the JSON representation of this Revenue object
     */
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

    /**
     * Custom equals function to compare 2 revenue objects. 2 Revenue objects are equal if
     * all of their fields are equal. Generated by Android Studio
     *
     * @param o the other object to compare to
     * @return true if the two Revenue objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Revenue revenue = (Revenue) o;

        if (quantity != revenue.quantity) return false;
        if (productId != null ? !productId.equals(revenue.productId) : revenue.productId != null)
            return false;
        if (price != null ? !price.equals(revenue.price) : revenue.price != null) return false;
        if (revenueType != null ? !revenueType.equals(revenue.revenueType) : revenue.revenueType != null)
            return false;
        if (receipt != null ? !receipt.equals(revenue.receipt) : revenue.receipt != null)
            return false;
        if (receiptSig != null ? !receiptSig.equals(revenue.receiptSig) : revenue.receiptSig != null)
            return false;
        return !(properties != null ? !Utils.compareJSONObjects(properties, revenue.properties): revenue.properties != null);

    }

    /**
     * Custom hashcode generator function for Revenue object. Generated by Android Studio.
     *
     * @return the hashcode for this Revenue instance~
     */
    @Override
    public int hashCode() {
        int result = productId != null ? productId.hashCode() : 0;
        result = 31 * result + quantity;
        result = 31 * result + (price != null ? price.hashCode() : 0);
        result = 31 * result + (revenueType != null ? revenueType.hashCode() : 0);
        result = 31 * result + (receipt != null ? receipt.hashCode() : 0);
        result = 31 * result + (receiptSig != null ? receiptSig.hashCode() : 0);
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        return result;
    }
}
