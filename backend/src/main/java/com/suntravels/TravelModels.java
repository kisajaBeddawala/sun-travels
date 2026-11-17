package com.suntravels;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class TravelModels {
    private TravelModels() {}

    public record HotelInput(@NotBlank @Size(max = 150) String name,
                             @Size(max = 120) String city) {}
    public record Hotel(long id, String name, String city) {}

    public record RoomInput(
            @NotBlank @Size(max = 120) String name,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal ratePerAdultPerNight,
            @NotNull @Min(0) @Max(10000) Integer roomCount,
            @NotNull @Min(1) @Max(100) Integer maxAdults) {}

    public record ContractInput(
            @NotNull @Positive Long hotelId,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotNull @DecimalMin("0") @Digits(integer = 3, fraction = 4) BigDecimal markupPercentage,
            @NotBlank @Pattern(regexp = "USD") String currency,
            @NotEmpty @Size(max = 100) List<@NotNull @Valid RoomInput> roomTypes) {}

    public record ContractSummary(long id, long hotelId, String hotelName, String city,
                                  LocalDate startDate, LocalDate endDate, String markupPercentage,
                                  String currency, int roomTypeCount) {}
    public record ContractRoom(long id, String name, String ratePerAdultPerNight, int roomCount, int maxAdults) {}
    public record ContractDetail(long id, long hotelId, String hotelName, String city,
                                 LocalDate startDate, LocalDate endDate, String markupPercentage,
                                 String currency, List<ContractRoom> roomTypes) {}

    public record Occupancy(@NotNull @Min(1) @Max(100) Integer adults) {}
    public record SearchInput(
            @NotNull LocalDate checkInDate,
            @NotNull @Min(1) @Max(365) Integer numberOfNights,
            @NotEmpty @Size(max = 100) List<@NotNull @Valid Occupancy> rooms) {}

    public enum Availability { AVAILABLE, NOT_AVAILABLE }
    public enum Reason { INSUFFICIENT_ROOMS, OCCUPANCY_EXCEEDED }
    public record Offer(long hotelId, String hotelName, String city, long contractId, long roomTypeId,
                        String roomType, Availability availability, List<Reason> reasons,
                        String totalPrice, String currency) {}
    public record SearchResult(LocalDate checkInDate, LocalDate checkOutDate, List<Offer> results) {}
}
