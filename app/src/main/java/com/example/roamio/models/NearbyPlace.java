package com.example.roamio.models;

/**
 * NearbyPlace
 * Data model for a place returned from Google Places Nearby Search API.
 */
public class NearbyPlace {

    private final String name;
    private final String address;
    private final double lat;
    private final double lng;
    private final double rating;
    private final int    userRatingsTotal;
    private final String placeId;
    private final String photoReference;
    private final String category;
    private boolean isFavourite;

    public NearbyPlace(String name, String address, double lat, double lng,
                       double rating, int userRatingsTotal,
                       String placeId, String photoReference, String category) {
        this.name             = name;
        this.address          = address;
        this.lat              = lat;
        this.lng              = lng;
        this.rating           = rating;
        this.userRatingsTotal = userRatingsTotal;
        this.placeId          = placeId;
        this.photoReference   = photoReference;
        this.category         = category;
        this.isFavourite      = false;
    }

    public String getName()              { return name; }
    public String getAddress()           { return address; }
    public double getLat()               { return lat; }
    public double getLng()               { return lng; }
    public double getRating()            { return rating; }
    public int    getUserRatingsTotal()  { return userRatingsTotal; }
    public String getPlaceId()           { return placeId; }
    public String getPhotoReference()    { return photoReference; }
    public String getCategory()          { return category; }
    public boolean isFavourite()         { return isFavourite; }
    public void setFavourite(boolean favourite) { isFavourite = favourite; }

    public boolean hasPhoto() {
        return photoReference != null && !photoReference.isEmpty();
    }
}
