package com.example.roamio.models;

import com.google.firebase.Timestamp;
import java.util.List;

/**
 * Trip — canonical data model for a Roamio trip.
 *
 * tripScope is the cross-city extensibility hook:
 *   "LOCAL"    — exploring within the same city (current default)
 *   "DISTRICT" — origin and destination in the same district (future)
 *   "STATE"    — origin and destination in the same state (future)
 *   "NATIONAL" — any two cities in India (future)
 *
 * originCity is nullable for now. When cross-city support is added it will be
 * populated from the user's homeCity profile field or a "Travelling from" input.
 */
public class Trip {

    public static final String SCOPE_LOCAL    = "LOCAL";
    public static final String SCOPE_DISTRICT = "DISTRICT";
    public static final String SCOPE_STATE    = "STATE";
    public static final String SCOPE_NATIONAL = "NATIONAL";

    public static final String STATUS_UPCOMING  = "upcoming";
    public static final String STATUS_ACTIVE    = "active";
    public static final String STATUS_COMPLETED = "completed";

    private String       tripId;
    private String       tripName;
    private String       destination;
    private String       originCity;   // nullable; future cross-city use
    private String       tripScope;    // LOCAL / DISTRICT / STATE / NATIONAL
    private String       startDate;    // dd/MM/yyyy
    private String       endDate;      // dd/MM/yyyy
    private String       travellers;
    private String       budget;
    private String       notes;
    private List<String> keywords;
    private String       tripStatus;
    private double       destLat;
    private double       destLng;
    private String       hotelLink;    // optional booking URL pasted by user
    private Timestamp    createdAt;

    public Trip() {}

    public Trip(String tripName, String destination, String tripScope,
                String startDate, String endDate, String travellers,
                String budget, String notes, List<String> keywords) {
        this.tripName    = tripName;
        this.destination = destination;
        this.tripScope   = tripScope != null ? tripScope : SCOPE_LOCAL;
        this.startDate   = startDate;
        this.endDate     = endDate;
        this.travellers  = travellers;
        this.budget      = budget;
        this.notes       = notes;
        this.keywords    = keywords;
        this.tripStatus  = STATUS_UPCOMING;
        this.createdAt   = Timestamp.now();
    }

    public String       getTripId()                            { return tripId; }
    public void         setTripId(String v)                    { tripId = v; }
    public String       getTripName()                          { return tripName; }
    public void         setTripName(String v)                  { tripName = v; }
    public String       getDestination()                       { return destination; }
    public void         setDestination(String v)               { destination = v; }
    public String       getOriginCity()                        { return originCity; }
    public void         setOriginCity(String v)                { originCity = v; }
    public String       getTripScope()                         { return tripScope; }
    public void         setTripScope(String v)                 { tripScope = v; }
    public String       getStartDate()                         { return startDate; }
    public void         setStartDate(String v)                 { startDate = v; }
    public String       getEndDate()                           { return endDate; }
    public void         setEndDate(String v)                   { endDate = v; }
    public String       getTravellers()                        { return travellers; }
    public void         setTravellers(String v)                { travellers = v; }
    public String       getBudget()                            { return budget; }
    public void         setBudget(String v)                    { budget = v; }
    public String       getNotes()                             { return notes; }
    public void         setNotes(String v)                     { notes = v; }
    public List<String> getKeywords()                          { return keywords; }
    public void         setKeywords(List<String> v)            { keywords = v; }
    public String       getTripStatus()                        { return tripStatus; }
    public void         setTripStatus(String v)                { tripStatus = v; }
    public double       getDestLat()                           { return destLat; }
    public void         setDestLat(double v)                   { destLat = v; }
    public double       getDestLng()                           { return destLng; }
    public void         setDestLng(double v)                   { destLng = v; }
    public String       getHotelLink()                         { return hotelLink; }
    public void         setHotelLink(String v)                 { hotelLink = v; }
    public Timestamp    getCreatedAt()                         { return createdAt; }
    public void         setCreatedAt(Timestamp v)              { createdAt = v; }

    public boolean isCrossCity() {
        return SCOPE_DISTRICT.equals(tripScope)
                || SCOPE_STATE.equals(tripScope)
                || SCOPE_NATIONAL.equals(tripScope);
    }

    public String getScopeLabel() {
        if (tripScope == null) return "Local Trip";
        switch (tripScope) {
            case SCOPE_DISTRICT: return "District Trip";
            case SCOPE_STATE:    return "State Trip";
            case SCOPE_NATIONAL: return "Cross-City Trip";
            default:             return "Local Trip";
        }
    }
}
