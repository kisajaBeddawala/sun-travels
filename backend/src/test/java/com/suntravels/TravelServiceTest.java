package com.suntravels;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.suntravels.TravelModels.*;
import static org.junit.jupiter.api.Assertions.*;

class TravelServiceTest {
    private static final LocalDate START = LocalDate.of(2027, 1, 1);
    private static final LocalDate END = LocalDate.of(2027, 12, 31);
    private final FixtureRepository repository = new FixtureRepository();
    private final TravelService service = new TravelService(repository);

    @Test
    void quotesTheFullStayForEveryAdultWithoutMultiplyingRoomCountAgain() {
        var offers = search(START, 3, 2, 1).results();
        assertEquals("362.25", standard(offers).totalPrice());
        assertEquals("569.25", offers.stream().filter(o -> o.roomType().equals("Sea View"))
                .findFirst().orElseThrow().totalPrice());
        assertEquals("241.50", standard(search(START, 3, 2).results()).totalPrice());
    }

    @Test
    void checksAdultsIndividuallyAndReportsUnavailableWithNoQuote() {
        var offers = search(START, 1, 3, 1).results();
        var standard = standard(offers);
        assertEquals(Availability.NOT_AVAILABLE, standard.availability());
        assertEquals(List.of(Reason.OCCUPANCY_EXCEEDED), standard.reasons());
        assertNull(standard.totalPrice());
        assertEquals(Availability.AVAILABLE, offers.get(0).availability());
    }

    @Test
    void treatsContractEndAsTheLastOccupiedNight() {
        var quote = search(END, 1, 1);
        assertEquals(END.plusDays(1), quote.checkOutDate());
        assertEquals(END, repository.lastNight);
        assertEquals(Availability.AVAILABLE, standard(quote.results()).availability());
        assertTrue(search(END, 2, 1).results().isEmpty());
        assertTrue(search(START.minusDays(1), 1, 1).results().isEmpty());
    }

    @Test
    void allowsExactAllocationAndReportsBothReasonsWhenApplicable() {
        int[] full = new int[15];
        java.util.Arrays.fill(full, 1);
        assertEquals(Availability.AVAILABLE, standard(search(START, 1, full).results()).availability());
        int[] excess = java.util.Arrays.copyOf(full, 16);
        excess[15] = 3;
        var result = standard(search(START, 1, excess).results());
        assertEquals(List.of(Reason.INSUFFICIENT_ROOMS, Reason.OCCUPANCY_EXCEEDED), result.reasons());
    }

    @Test
    void roundsOnlyTheFinalAmountAndAllowsZeroMarkup() {
        assertEquals(new BigDecimal("64.56"), TravelService.total(new BigDecimal("10.01"), new BigDecimal("7.5"), 3, 2));
        assertEquals(new BigDecimal("60.06"), TravelService.total(new BigDecimal("10.01"), BigDecimal.ZERO, 3, 2));
    }

    @Test
    void validatesTheWholeContractBeforeWritingAnyRows() {
        var room = new RoomInput("Standard", new BigDecimal("35"), 10, 2);
        var duplicate = new RoomInput(" standard ", new BigDecimal("40"), 5, 2);
        var contract = new ContractInput(1L, START, END, new BigDecimal("15"), "USD", List.of(room, duplicate));
        var error = assertThrows(TravelService.FieldProblem.class, () -> service.addContract(contract));
        assertEquals("roomTypes[1].name", error.field());
        var reversed = new ContractInput(1L, END, START, BigDecimal.ZERO, "USD", List.of(room));
        assertEquals("endDate", assertThrows(TravelService.FieldProblem.class, () -> service.addContract(reversed)).field());
    }

    private SearchResult search(LocalDate date, int nights, int... adults) {
        return service.search(new SearchInput(date, nights, java.util.Arrays.stream(adults).mapToObj(Occupancy::new).toList()));
    }

    private Offer standard(List<Offer> offers) {
        return offers.stream().filter(offer -> offer.roomType().equals("Standard")).findFirst().orElseThrow();
    }

    private static final class FixtureRepository extends TravelRepository {
        private LocalDate lastNight;

        private FixtureRepository() { super(null); }

        @Override
        boolean hasHotel(long id) { return id == 1; }

        @Override
        List<Candidate> candidates(LocalDate checkIn, LocalDate lastNight) {
            this.lastNight = lastNight;
            if (checkIn.isBefore(START) || lastNight.isAfter(END)) {
                return List.of();
            }
            return List.of(
                    new Candidate(1, "Demo Hotel", "Colombo", 1, 1, "Standard",
                            new BigDecimal("35.00"), new BigDecimal("15"), "USD", 15, 2),
                    new Candidate(1, "Demo Hotel", "Colombo", 1, 2, "Sea View",
                            new BigDecimal("55.00"), new BigDecimal("15"), "USD", 8, 3));
        }
    }
}
