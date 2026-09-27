package com.donututils.arsenal.model;

public final class PlayerWeaponState {

    private long lastShotAtMillis;
    private double currentSpreadDegrees;
    private boolean reloading;

    public long lastShotAtMillis() {
        return lastShotAtMillis;
    }

    public void setLastShotAtMillis(long lastShotAtMillis) {
        this.lastShotAtMillis = lastShotAtMillis;
    }

    public double currentSpreadDegrees() {
        return currentSpreadDegrees;
    }

    public void setCurrentSpreadDegrees(double currentSpreadDegrees) {
        this.currentSpreadDegrees = currentSpreadDegrees;
    }

    public boolean isReloading() {
        return reloading;
    }

    public void setReloading(boolean reloading) {
        this.reloading = reloading;
    }
}
