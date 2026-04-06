package com.example.roamio.models;

import com.google.firebase.Timestamp;

/**
 * Review model — stores a single user review for a place.
 * Saved under: places/{placeId}/reviews/{autoId}
 */
public class Review {

    private String    userId;
    private String    userName;
    private String    placeName;
    private String    placeId;
    private String    reviewText;
    private float     rating;       // 1.0 – 5.0
    private Timestamp createdAt;

    private String docId;

    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }

    // Required no-arg constructor for Firestore toObject()
    public Review() {}

    public Review(String userId, String userName, String placeName,
                  String placeId, String reviewText, float rating) {
        this.userId     = userId;
        this.userName   = userName;
        this.placeName  = placeName;
        this.placeId    = placeId;
        this.reviewText = reviewText;
        this.rating     = rating;
        this.createdAt  = Timestamp.now();
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────
    public String    getUserId()                       { return userId; }
    public void      setUserId(String userId)          { this.userId = userId; }

    public String    getUserName()                     { return userName; }
    public void      setUserName(String userName)      { this.userName = userName; }

    public String    getPlaceName()                    { return placeName; }
    public void      setPlaceName(String placeName)    { this.placeName = placeName; }

    public String    getPlaceId()                      { return placeId; }
    public void      setPlaceId(String placeId)        { this.placeId = placeId; }

    public String    getReviewText()                   { return reviewText; }
    public void      setReviewText(String reviewText)  { this.reviewText = reviewText; }

    public float     getRating()                       { return rating; }
    public void      setRating(float rating)           { this.rating = rating; }

    public Timestamp getCreatedAt()                    { return createdAt; }
    public void      setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    /** Display-friendly time-ago string */
    public String getTimeAgo() {
        if (createdAt == null) return "";
        long diffMs  = System.currentTimeMillis() - createdAt.toDate().getTime();
        long diffMin = diffMs / 60_000;
        if (diffMin < 1)   return "Just now";
        if (diffMin < 60)  return diffMin + "m ago";
        long diffHr = diffMin / 60;
        if (diffHr  < 24)  return diffHr  + "h ago";
        long diffDay = diffHr / 24;
        if (diffDay < 30)  return diffDay + "d ago";
        return (diffDay / 30) + "mo ago";
    }

    /** Single initial for avatar circle */
    public String getInitial() {
        if (userName == null || userName.isEmpty()) return "?";
        return String.valueOf(userName.charAt(0)).toUpperCase();
    }
}