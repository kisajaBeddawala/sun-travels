package com.suntravels;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

import static com.suntravels.TravelModels.*;

@RestController
@RequestMapping("/api")
class TravelController {
    private final TravelService service;

    TravelController(TravelService service) {
        this.service = service;
    }

    @GetMapping("/hotels")
    List<Hotel> hotels() {
        return service.hotels();
    }

    @PostMapping("/hotels")
    ResponseEntity<Hotel> addHotel(@Valid @RequestBody HotelInput input) {
        return ResponseEntity.status(201).body(service.addHotel(input));
    }

    @GetMapping("/contracts")
    List<ContractSummary> contracts() {
        return service.contracts();
    }

    @GetMapping("/contracts/{id}")
    ContractDetail contract(@PathVariable long id) {
        return service.contract(id);
    }

    @PostMapping("/contracts")
    ResponseEntity<ContractDetail> addContract(@Valid @RequestBody ContractInput input) {
        var contract = service.addContract(input);
        return ResponseEntity.created(URI.create("/api/contracts/" + contract.id())).body(contract);
    }

    @PostMapping("/search")
    SearchResult search(@Valid @RequestBody SearchInput input) {
        return service.search(input);
    }
}
