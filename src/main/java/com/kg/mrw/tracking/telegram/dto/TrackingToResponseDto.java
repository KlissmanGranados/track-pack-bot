package com.kg.mrw.tracking.telegram.dto;

public class TrackingToResponseDto {

    private String origin;
    private String destination;
    private String client;
    private String address;
    private String typeOfShipment;
    private String trackingCode;
    private String status;
    private String response;
    private String provider;
    private boolean hasArrived = false;

    public boolean isHasArrived() {
        return hasArrived;
    }

    public void setHasArrived(boolean hasArrived) {
        this.hasArrived = hasArrived;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTypeOfShipment() {
        return typeOfShipment;
    }

    public void setTypeOfShipment(String typeOfShipment) {
        this.typeOfShipment = typeOfShipment;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public void setTrackingCode(String trackingCode) {
        this.trackingCode = trackingCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String details(){
        return String.format(
        """
        <b>Tracking:</b> <i>%s</i>
        <b>Client:</b> <i>%s</i>
        <b>Address:</b> <i>%s</i>
        <b>Origin:</b> <i>%s</i>
        <b>Destination:</b> <i>%s</i>
        <b>Type of Shipment:</b> <i>%s</i>
        <b>Status:</b> <i>%s</i>
        """,
                trackingCode, client, address, origin, destination, typeOfShipment, status
        );
    }
}
