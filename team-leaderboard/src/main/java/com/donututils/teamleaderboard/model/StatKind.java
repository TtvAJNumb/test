package com.donututils.teamleaderboard.model;

public enum StatKind {
    MONEY("MONEY", "&6Team BalTop", "Money"),
    SHARDS("SHARDS", "&bTeam ShardsTop", "Shards");

    /** Exact enum constant name on UltimateDonutSmp's own LeaderboardManager.LeaderboardType. */
    private final String udsLeaderboardTypeName;
    private final String menuTitle;
    private final String label;

    StatKind(String udsLeaderboardTypeName, String menuTitle, String label) {
        this.udsLeaderboardTypeName = udsLeaderboardTypeName;
        this.menuTitle = menuTitle;
        this.label = label;
    }

    public String getUdsLeaderboardTypeName() {
        return udsLeaderboardTypeName;
    }

    public String getMenuTitle() {
        return menuTitle;
    }

    public String getLabel() {
        return label;
    }
}
