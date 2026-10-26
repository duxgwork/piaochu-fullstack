package com.piaochu.app;
import static com.piaochu.app.Contracts.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController
class CatalogController {
    private final KitchenService service;CatalogController(KitchenService s){service=s;}
    @GetMapping("/api/health") Map<String,String> health(){return Map.of("service","piaochu-server","status","UP");}
    @GetMapping("/api/catalog") Catalog catalog(){return service.catalog();}
}
