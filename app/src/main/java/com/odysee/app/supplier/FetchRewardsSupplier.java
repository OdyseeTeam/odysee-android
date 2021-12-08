package com.odysee.app.supplier;

import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.model.lbryinc.Reward;
import com.odysee.app.utils.Lbryio;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class FetchRewardsSupplier implements Supplier<List<Reward>> {
    Map<String, String> options;

    public FetchRewardsSupplier(Map<String, String> options) {
        this.options = options;
    }

    @Override
    public List<Reward> get() {
        List<Reward> rewards = new ArrayList<>();
        try {
            JSONArray results = (JSONArray) Lbryio.parseResponse(Lbryio.call("reward", "list", options, null));
            rewards = new ArrayList<>();
            if (results != null) {
                for (int i = 0; i < results.length(); i++) {
                    rewards.add(Reward.fromJSONObject(results.getJSONObject(i)));
                }
            }
        } catch (LbryioResponseException | JSONException | LbryioRequestException e) {
            e.printStackTrace();
        }
        return rewards;
    }
}
