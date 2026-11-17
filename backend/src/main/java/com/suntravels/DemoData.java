package com.suntravels;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.suntravels.TravelModels.*;

@Component
@ConditionalOnProperty(name = "suntravels.demo-enabled", havingValue = "true")
class DemoData implements ApplicationRunner {
    private final TravelService service;

    DemoData(TravelService service) {
        this.service = service;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!service.hotels().isEmpty()) {
            return;
        }
        var hotel = service.addHotel(new HotelInput("Demo Hotel", "Colombo"));
        service.addContract(new ContractInput(hotel.id(), LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31),
                new BigDecimal("15"), "USD", List.of(
                new RoomInput("Standard", new BigDecimal("35.00"), 15, 2),
                new RoomInput("Sea View", new BigDecimal("55.00"), 8, 3))));
    }
}
