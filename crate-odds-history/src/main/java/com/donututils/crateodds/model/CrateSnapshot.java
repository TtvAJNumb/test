package com.donututils.crateodds.model;

import java.util.List;

/** A crate definition's rewards, resolved to display data and sorted by descending drop chance. */
public record CrateSnapshot(String crateId, List<RewardOdds> rewards) {
}
