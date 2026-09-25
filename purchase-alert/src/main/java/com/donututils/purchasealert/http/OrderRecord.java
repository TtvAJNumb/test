package com.donututils.purchasealert.http;

/** One row from StoreBridge's backend's read-only order list. */
public record OrderRecord(String id, String username, String product, String status, String event) {
}
