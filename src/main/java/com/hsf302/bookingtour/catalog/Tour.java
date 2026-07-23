package com.hsf302.bookingtour.catalog;

/**
 * Standalone tour record for the chat/RAG feature. Kept separate from
 * TourController's own nested Tour record so nothing in TourController needs
 * to change - this is a purely additive feature.
 */
public record Tour(String id, String name, String location, int price, double rating, String summary) {
}