package com.suntravels;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TravelControllerTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var repository = new TravelRepository(null) {
            @Override
            List<Candidate> candidates(java.time.LocalDate checkIn, java.time.LocalDate lastNight) {
                return List.of();
            }
        };
        mvc = MockMvcBuilders.standaloneSetup(new TravelController(new TravelService(repository)))
                .setControllerAdvice(new ApiErrors()).build();
    }

    @Test
    void validSearchReturnsIsoDatesAndAnEmptyArray() throws Exception {
        mvc.perform(post("/api/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checkInDate\":\"2027-06-10\",\"numberOfNights\":3,\"rooms\":[{\"adults\":2}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.checkInDate").value("2027-06-10"))
                .andExpect(jsonPath("$.checkOutDate").value("2027-06-13")).andExpect(jsonPath("$.results").isEmpty());
    }

    @Test
    void invalidSearchIdentifiesTheNestedAdultField() throws Exception {
        mvc.perform(post("/api/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checkInDate\":\"2027-06-10\",\"numberOfNights\":3,\"rooms\":[{\"adults\":0}]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("rooms[0].adults"));
    }
}
