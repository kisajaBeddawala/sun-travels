package com.suntravels;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.suntravels.TravelModels.*;

@Repository
class TravelRepository {
    private static final String SUMMARY_SELECT = """
            SELECT c.id, c.hotel_id, h.name AS hotel_name, h.city, c.start_date, c.end_date,
                   c.markup_pct, c.currency,
                   (SELECT COUNT(*) FROM contract_room_type r WHERE r.contract_id = c.id) AS room_type_count
            FROM hotel_contract c
            JOIN hotel h ON h.id = c.hotel_id
            """;
    private static final RowMapper<ContractSummary> SUMMARY = (row, index) -> new ContractSummary(
            row.getLong("id"), row.getLong("hotel_id"), row.getString("hotel_name"), row.getString("city"),
            row.getObject("start_date", LocalDate.class), row.getObject("end_date", LocalDate.class),
            row.getBigDecimal("markup_pct").stripTrailingZeros().toPlainString(), row.getString("currency"),
            row.getInt("room_type_count"));
    private final JdbcClient db;

    TravelRepository(JdbcClient db) {
        this.db = db;
    }

    List<Hotel> hotels() {
        return db.sql("SELECT id, name, city FROM hotel ORDER BY lower(name), id")
                .query((row, index) -> new Hotel(row.getLong("id"), row.getString("name"), row.getString("city")))
                .list();
    }

    Hotel addHotel(String name, String city) {
        long id = db.sql("INSERT INTO hotel (name, city) VALUES (:name, :city) RETURNING id")
                .param("name", name).param("city", city).query(Long.class).single();
        return new Hotel(id, name, city);
    }

    boolean hasHotel(long id) {
        return db.sql("SELECT EXISTS (SELECT 1 FROM hotel WHERE id = :id)")
                .param("id", id).query(Boolean.class).single();
    }

    List<ContractSummary> contracts() {
        return db.sql(SUMMARY_SELECT + " ORDER BY c.id DESC").query(SUMMARY).list();
    }

    Optional<ContractSummary> contract(long id) {
        return db.sql(SUMMARY_SELECT + " WHERE c.id = :id").param("id", id).query(SUMMARY).optional();
    }

    List<ContractRoom> contractRooms(long contractId) {
        return db.sql("""
                SELECT id, name, rate_per_adult_per_night, room_count, max_adults
                FROM contract_room_type WHERE contract_id = :id ORDER BY id
                """).param("id", contractId).query((row, index) -> new ContractRoom(
                row.getLong("id"), row.getString("name"), row.getBigDecimal("rate_per_adult_per_night").toPlainString(),
                row.getInt("room_count"), row.getInt("max_adults"))).list();
    }

    long addContract(ContractInput input) {
        return db.sql("""
                INSERT INTO hotel_contract (hotel_id, start_date, end_date, markup_pct, currency)
                VALUES (:hotel, :start, :end, :markup, :currency) RETURNING id
                """).param("hotel", input.hotelId()).param("start", input.startDate()).param("end", input.endDate())
                .param("markup", input.markupPercentage()).param("currency", input.currency()).query(Long.class).single();
    }

    void addRoom(long contractId, RoomInput input) {
        db.sql("""
                INSERT INTO contract_room_type (contract_id, name, rate_per_adult_per_night, room_count, max_adults)
                VALUES (:contract, :name, :rate, :quantity, :capacity)
                """).param("contract", contractId).param("name", input.name().strip())
                .param("rate", input.ratePerAdultPerNight()).param("quantity", input.roomCount())
                .param("capacity", input.maxAdults()).update();
    }

    List<Candidate> candidates(LocalDate checkIn, LocalDate lastNight) {
        return db.sql("""
                SELECT h.id AS hotel_id, h.name AS hotel_name, h.city, c.id AS contract_id,
                       r.id AS room_id, r.name AS room_type, r.rate_per_adult_per_night,
                       c.markup_pct, c.currency, r.room_count, r.max_adults
                FROM hotel_contract c
                JOIN hotel h ON h.id = c.hotel_id
                JOIN contract_room_type r ON r.contract_id = c.id
                WHERE c.start_date <= :checkIn AND c.end_date >= :lastNight
                """).param("checkIn", checkIn).param("lastNight", lastNight).query((row, index) -> new Candidate(
                row.getLong("hotel_id"), row.getString("hotel_name"), row.getString("city"),
                row.getLong("contract_id"), row.getLong("room_id"), row.getString("room_type"),
                row.getBigDecimal("rate_per_adult_per_night"), row.getBigDecimal("markup_pct"), row.getString("currency"),
                row.getInt("room_count"), row.getInt("max_adults"))).list();
    }

    record Candidate(long hotelId, String hotelName, String city, long contractId, long roomId, String roomType,
                     BigDecimal rate, BigDecimal markup, String currency, int roomCount, int maxAdults) {}
}
