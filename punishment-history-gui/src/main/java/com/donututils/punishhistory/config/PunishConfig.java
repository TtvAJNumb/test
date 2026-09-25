package com.donututils.punishhistory.config;

public record PunishConfig(
        String rosterTitle,
        String detailTitleTemplate,
        int rosterScanSize,
        int maxNotesPerPlayer
) {
}
