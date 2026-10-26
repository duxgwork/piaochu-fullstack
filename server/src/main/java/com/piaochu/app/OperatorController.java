package com.piaochu.app;
import static com.piaochu.app.Contracts.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/api/operator")
class OperatorController {
    private final KitchenService service;OperatorController(KitchenService s){service=s;}
    @GetMapping("/orders") List<OrderView> orders(@RequestAttribute("actor") Actor a){return service.operatorOrders(a);}
    @GetMapping("/bookings") List<BookingView> bookings(@RequestAttribute("actor") Actor a){return service.operatorBookings(a);}
    @PatchMapping("/orders/{id}/status") OrderView order(@RequestAttribute("actor") Actor a,@PathVariable long id,@Valid @RequestBody StatusInput r){return service.advanceOrder(a,id,r);}
    @PatchMapping("/bookings/{id}/status") BookingView booking(@RequestAttribute("actor") Actor a,@PathVariable long id,@Valid @RequestBody StatusInput r){return service.advanceBooking(a,id,r);}
}
