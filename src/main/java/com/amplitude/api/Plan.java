package com.amplitude.api;

import org.json.JSONException;
import org.json.JSONObject;

public class Plan {
    private static final String TAG = Plan.class.getName();
    /**
     * The tracking plan branch name e.g. "main"
     */
    private String branch;
    /**
     * The tracking plan source e.g. "web", "mobile"
     */
    private String source;
    /**
     * The tracking plan version e.g. "1", "15"
     */
    private String version;
    /**
     * The tracking plan version Id e.g. "9ec23ba0-275f-468f-80d1-66b88bff9529"
     */
    private String versionId;

    /**
     * Set the tracking plan branch information.
     * @param branch The tracking plan branch name e.g. "main"
     * @return the same Plan object
     */
    public Plan setBranch(String branch) {
        this.branch = branch;
        return this;
    }

    /**
     * Set the tracking plan source information.
     * @param source The tracking plan source e.g. "web", "mobile"
     * @return the same Plan object
     */
    public Plan setSource(String source) {
        this.source = source;
        return this;
    }

    /**
     * Set the tracking plan version information.
     * @param version The tracking plan version e.g. "1", "15"
     * @return the same Plan object
     */
    public Plan setVersion(String version) {
        this.version = version;
        return this;
    }

    /**
     * Set the tracking plan version Id.
     * @param version The tracking plan version e.g. "9ec23ba0-275f-468f-80d1-66b88bff9529"
     * @return the same Plan object
     */
    public Plan setVersionId(String versionId) {
        this.versionId = versionId;
        return this;
    }

    /**
     * Get JSONObject of current tacking plan
     * @return JSONObject including plan information
     */
    protected JSONObject toJSONObject() {
        JSONObject plan = new JSONObject();
        try {
            if (!Utils.isEmptyString(branch)) {
                plan.put(Constants.AMP_PLAN_BRANCH, branch);
            }
            if (!Utils.isEmptyString(source)) {
                plan.put(Constants.AMP_PLAN_SOURCE, source);
            }
            if (!Utils.isEmptyString(version)) {
                plan.put(Constants.AMP_PLAN_VERSION, version);
            }
            if (!Utils.isEmptyString(versionId)) {
                plan.put(Constants.AMP_PLAN_VERSION_ID, versionId);
            }
        } catch (JSONException e) {
            AmplitudeLog.getLogger().e(TAG, "JSON Serialization of tacking plan object failed");
        }
        return plan;
    }
}
