package com.suntravels;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import static com.suntravels.TravelModels.*;

@Service
class TravelService {
    private final TravelRepository store;

    TravelService(TravelRepository store) {
        this.store = store;
    }

    List<Hotel> hotels() {
        return store.hotels();
    }

    Hotel addHotel(HotelInput input) {
        String city = input.city() == null || input.city().isBlank() ? null : input.city().strip();
        return store.addHotel(input.name().strip(), city);
    }

    List<ContractSummary> contracts() {
        return store.contracts();
    }

    @Transactional(readOnly = true)
    public ContractDetail contract(long id) {
        var summary = store.contract(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract was not found."));
        return new ContractDetail(summary.id(), summary.hotelId(), summary.hotelName(), summary.city(),
                summary.startDate(), summary.endDate(), summary.markupPercentage(), summary.currency(),
                store.contractRooms(id));
    }

    @Transactional
    public ContractDetail addContract(ContractInput input) {
        validateDate("startDate", input.startDate());
        validateDate("endDate", input.endDate());
        if (input.endDate().isBefore(input.startDate())) {
            throw new FieldProblem("endDate", "Use an end date on or after the start date.");
        }
        if (!store.hasHotel(input.hotelId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The selected hotel was not found.");
        }
        var names = new HashSet<String>();
        for (int index = 0; index < input.roomTypes().size(); index++) {
            String name = input.roomTypes().get(index).name().strip().toLowerCase(Locale.ROOT);
            if (!names.add(name)) {
                throw new FieldProblem("roomTypes[" + index + "].name", "Use a different name for each room type.");
            }
        }
        long id = store.addContract(input);
        input.roomTypes().forEach(room -> store.addRoom(id, room));
        return contract(id);
    }

    SearchResult search(SearchInput input) {
        validateDate("checkInDate", input.checkInDate());
        LocalDate checkout = input.checkInDate().plusDays(input.numberOfNights());
        int adults = input.rooms().stream().mapToInt(Occupancy::adults).sum();
        int largestRoom = input.rooms().stream().mapToInt(Occupancy::adults).max().orElseThrow();
        var results = store.candidates(input.checkInDate(), checkout.minusDays(1)).stream()
                .map(candidate -> quote(candidate, input.rooms().size(), largestRoom, adults, input.numberOfNights()))
                .sorted(Comparator.comparing(Offer::availability)
                        .thenComparing(Offer::hotelName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Offer::roomType, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingLong(Offer::contractId).thenComparingLong(Offer::roomTypeId))
                .toList();
        return new SearchResult(input.checkInDate(), checkout, results);
    }

    private static Offer quote(TravelRepository.Candidate candidate, int rooms, int largestRoom, int adults, int nights) {
        var reasons = new ArrayList<Reason>(2);
        if (rooms > candidate.roomCount()) {
            reasons.add(Reason.INSUFFICIENT_ROOMS);
        }
        if (largestRoom > candidate.maxAdults()) {
            reasons.add(Reason.OCCUPANCY_EXCEEDED);
        }
        var availability = reasons.isEmpty() ? Availability.AVAILABLE : Availability.NOT_AVAILABLE;
        String price = reasons.isEmpty() ? total(candidate.rate(), candidate.markup(), adults, nights).toPlainString() : null;
        return new Offer(candidate.hotelId(), candidate.hotelName(), candidate.city(), candidate.contractId(),
                candidate.roomId(), candidate.roomType(), availability, List.copyOf(reasons), price, candidate.currency());
    }

    static BigDecimal total(BigDecimal rate, BigDecimal markup, int adults, int nights) {
        return rate.multiply(BigDecimal.ONE.add(markup.movePointLeft(2)))
                .multiply(BigDecimal.valueOf(adults))
                .multiply(BigDecimal.valueOf(nights))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static void validateDate(String field, LocalDate date) {
        if (date.getYear() < 1900 || date.getYear() > 9998) {
            throw new FieldProblem(field, "Enter a year between 1900 and 9998.");
        }
    }

    static final class FieldProblem extends RuntimeException {
        private final String field;

        FieldProblem(String field, String message) {
            super(message);
            this.field = field;
        }

        String field() {
            return field;
        }
    }
}
