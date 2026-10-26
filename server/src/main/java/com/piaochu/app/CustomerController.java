package com.piaochu.app;
import static com.piaochu.app.Contracts.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/api")
class CustomerController {
    private final KitchenService service;CustomerController(KitchenService s){service=s;}
    @GetMapping("/addresses") List<AddressView> addresses(@RequestAttribute("actor") Actor a){return service.addresses(a.id());}
    @PostMapping("/addresses") @ResponseStatus(HttpStatus.CREATED) AddressView add(@RequestAttribute("actor") Actor a,@Valid @RequestBody AddressInput r){return service.saveAddress(a.id(),null,r);}
    @PutMapping("/addresses/{id}") AddressView edit(@RequestAttribute("actor") Actor a,@PathVariable long id,@Valid @RequestBody AddressInput r){return service.saveAddress(a.id(),id,r);}
    @DeleteMapping("/addresses/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@RequestAttribute("actor") Actor a,@PathVariable long id){service.deleteAddress(a.id(),id);}
    @GetMapping("/orders") List<OrderView> orders(@RequestAttribute("actor") Actor a){return service.orders(a.id());}
    @GetMapping("/orders/{id}") OrderView order(@RequestAttribute("actor") Actor a,@PathVariable long id){return service.order(a.id(),id);}
    @PostMapping("/orders") @ResponseStatus(HttpStatus.CREATED) OrderView create(@RequestAttribute("actor") Actor a,@Valid @RequestBody OrderInput r){return service.createOrder(a.id(),r);}
    @PostMapping("/orders/{id}/cancel") OrderView cancel(@RequestAttribute("actor") Actor a,@PathVariable long id){return service.cancel(a.id(),id);}
    @PostMapping("/orders/{id}/receive") OrderView receive(@RequestAttribute("actor") Actor a,@PathVariable long id){return service.receive(a.id(),id);}
    @GetMapping("/bookings") List<BookingView> bookings(@RequestAttribute("actor") Actor a){return service.bookings(a.id());}
    @GetMapping("/bookings/{id}") BookingView booking(@RequestAttribute("actor") Actor a,@PathVariable long id){return service.booking(a.id(),id);}
    @PostMapping("/bookings") @ResponseStatus(HttpStatus.CREATED) BookingView book(@RequestAttribute("actor") Actor a,@Valid @RequestBody BookingInput r){return service.createBooking(a.id(),r);}
    @PostMapping("/bookings/{id}/cancel") BookingView cancelBooking(@RequestAttribute("actor") Actor a,@PathVariable long id){return service.cancelBooking(a.id(),id);}
}

