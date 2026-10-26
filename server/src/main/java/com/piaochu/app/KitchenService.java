package com.piaochu.app;
import static com.piaochu.app.Contracts.*;
import static com.piaochu.app.AuthService.fail;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/** 业务事务入口：校验归属、冻结价格与地址快照，再写订单；状态不由客户端任意指定。 */
@Service @Transactional(readOnly=true)
class KitchenService {
    private final AddressRepository addresses;private final MenuRepository menu;private final OrderRepository orders;
    private final LineRepository lines;private final BookingRepository bookings;private final Clock clock;
    KitchenService(AddressRepository a,MenuRepository m,OrderRepository o,LineRepository l,BookingRepository b,Clock c) {
        addresses=a;menu=m;orders=o;lines=l;bookings=b;clock=c;
    }
    Catalog catalog() { var dishes=menu.findByAvailableTrueOrderByIdAsc();boolean demo=dishes.stream().anyMatch(d->d.demo);
        return new Catalog(demo?"成都 · 示例厨房":"漂厨厨房",demo,dishes.stream().map(Dish::from).toList(),500,5900); }
    List<AddressView> addresses(long user) { return addresses.findByUserIdOrderByIdDesc(user).stream().map(AddressView::from).toList(); }
    @Transactional AddressView saveAddress(long user,Long id,AddressInput input) {
        if(id==null && addresses.countByUserId(user)>=20) throw fail(400,"最多保存20个地址");
        Address a=id==null?new Address():address(user,id);a.userId=user;a.contactName=input.contactName().strip();a.phone=input.phone();
        a.detail=input.detail().strip();a.label=input.label().strip();a.stamp(clock.instant());return AddressView.from(addresses.saveAndFlush(a));
    }
    @Transactional void deleteAddress(long user,long id) { addresses.delete(address(user,id)); }
    private Address address(long user,long id) { return addresses.findByIdAndUserId(id,user).orElseThrow(()->fail(404,"地址不存在")); }
    List<OrderView> orders(long user) { return orders.findTop100ByUserIdOrderByIdDesc(user).stream().map(this::view).toList(); }
    OrderView order(long user,long id) { return view(ownOrder(user,id)); }
    private FoodOrder ownOrder(long user,long id) { return orders.findByIdAndUserId(id,user).orElseThrow(()->fail(404,"订单不存在")); }
    @Transactional OrderView createOrder(long user,OrderInput input) {
        String key=input.clientRequestId().toString();String digest=Passwords.digest(input.toString());
        var previous=orders.findByUserIdAndRequestKey(user,key);
        if(previous.isPresent()) { if(!previous.get().requestDigest.equals(digest)) throw fail(409,"同一提交编号不能用于不同订单");return view(previous.get()); }
        Address a=address(user,input.addressId());Set<Long> seen=new HashSet<>();List<OrderLine> snapshots=new ArrayList<>();long subtotal=0;boolean demo=false;
        for(ItemInput item:input.items()) {
            if(!seen.add(item.dishId())) throw fail(400,"同一菜品请合并数量后提交");
            MenuItem dish=menu.findById(item.dishId()).filter(x->x.available).orElseThrow(()->fail(400,"菜品已下架，请刷新菜单"));
            OrderLine line=new OrderLine();line.menuItemId=dish.id;line.name=dish.name;line.unitCents=dish.priceCents;line.quantity=item.quantity();line.stamp(clock.instant());
            subtotal=Math.addExact(subtotal,Math.multiplyExact(dish.priceCents,item.quantity()));demo|=dish.demo;snapshots.add(line);
        }
        if(subtotal>2_000_000) throw fail(400,"大额用餐请使用团餐预约");
        FoodOrder order=new FoodOrder();order.userId=user;order.requestKey=key;order.requestDigest=digest;order.status="PENDING_CONFIRMATION";
        order.subtotalCents=subtotal;order.deliveryCents=subtotal>=5900?0:500;order.totalCents=subtotal+order.deliveryCents;
        order.contactName=a.contactName;order.phone=a.phone;order.addressSnapshot=a.detail;order.note=input.note().strip();order.demo=demo;order.stamp(clock.instant());
        orders.saveAndFlush(order);for(OrderLine line:snapshots) line.orderId=order.id;lines.saveAllAndFlush(snapshots);return view(order);
    }
    @Transactional OrderView cancel(long user,long id) { FoodOrder o=ownOrder(user,id);
        if(o.status.equals("CANCELLED")) return view(o);
        if(!o.status.equals("PENDING_CONFIRMATION")) throw fail(409,"商家已接单，暂不支持直接取消，请联系商家");
        o.status="CANCELLED";o.stamp(clock.instant());return view(o);
    }
    @Transactional OrderView receive(long user,long id) { FoodOrder o=ownOrder(user,id);
        if(o.status.equals("COMPLETED")) return view(o);
        if(!o.status.equals("DELIVERING")) throw fail(409,"当前订单尚未进入配送阶段");
        o.status="COMPLETED";o.stamp(clock.instant());return view(o);
    }
    private OrderView view(FoodOrder o) { return new OrderView(o.id,o.status,o.subtotalCents,o.deliveryCents,o.totalCents,o.contactName,o.phone,
        o.addressSnapshot,o.note,o.demo,o.createdAt.toString(),lines.findByOrderIdOrderByIdAsc(o.id).stream().map(l->new LineView(l.menuItemId,l.name,l.unitCents,l.quantity)).toList()); }
    List<BookingView> bookings(long user) { return bookings.findTop100ByUserIdOrderByIdDesc(user).stream().map(BookingView::from).toList(); }
    BookingView booking(long user,long id) { return BookingView.from(ownBooking(user,id)); }
    private Booking ownBooking(long user,long id) { return bookings.findByIdAndUserId(id,user).orElseThrow(()->fail(404,"预约不存在")); }
    @Transactional BookingView createBooking(long user,BookingInput input) {
        String key=input.clientRequestId().toString(),digest=Passwords.digest(input.toString());var previous=bookings.findByUserIdAndRequestKey(user,key);
        if(previous.isPresent()) { if(!previous.get().requestDigest.equals(digest)) throw fail(409,"同一提交编号不能用于不同预约");return BookingView.from(previous.get()); }
        var scenes=input.serviceType().equals("GROUP")?Set.of("高铁航空","油站电信","企业工餐","家庭月餐"):Set.of("家宴代工","商务会餐","现场烹饪","定制餐宴");
        if(!scenes.contains(input.scene())) throw fail(400,"请选择对应服务的用餐场景");
        Instant when=input.scheduledAt().toInstant();if(!when.isAfter(clock.instant().plus(Duration.ofHours(2)))) throw fail(400,"请至少提前2小时预约");
        if(when.isAfter(clock.instant().plus(Duration.ofDays(365)))) throw fail(400,"预约时间不能超过一年");
        Address a=address(user,input.addressId());Booking b=new Booking();b.userId=user;b.requestKey=key;b.requestDigest=digest;
        b.serviceType=input.serviceType();b.scene=input.scene();b.scheduledAt=when;b.people=input.people();b.budgetCents=input.budgetCents();
        b.status="REQUESTED";b.contactName=a.contactName;b.phone=a.phone;b.addressSnapshot=a.detail;b.note=input.note().strip();b.stamp(clock.instant());
        return BookingView.from(bookings.saveAndFlush(b));
    }
    @Transactional BookingView cancelBooking(long user,long id) { Booking b=ownBooking(user,id);
        if(b.status.equals("CANCELLED")) return BookingView.from(b);
        if(!b.status.equals("REQUESTED")) throw fail(409,"预约已确认，请联系商家协商变更");
        b.status="CANCELLED";b.stamp(clock.instant());return BookingView.from(b);
    }
    private void requireOperator(Actor actor) { if(actor==null || !"OPERATOR".equals(actor.role())) throw fail(403,"需要运营账号"); }
    List<OrderView> operatorOrders(Actor actor) { requireOperator(actor);return orders.findTop100ByOrderByIdDesc().stream().map(this::view).toList(); }
    List<BookingView> operatorBookings(Actor actor) { requireOperator(actor);return bookings.findTop100ByOrderByIdDesc().stream().map(BookingView::from).toList(); }
    @Transactional OrderView advanceOrder(Actor actor,long id,StatusInput input) { requireOperator(actor);FoodOrder o=orders.findById(id).orElseThrow(()->fail(404,"订单不存在"));
        var next=Map.of("PENDING_CONFIRMATION","CONFIRMED","CONFIRMED","PREPARING","PREPARING","DELIVERING");
        if(!Objects.equals(next.get(o.status),input.status())) throw fail(409,"不允许跳过订单处理阶段");
        o.status=input.status();o.stamp(clock.instant());return view(o);
    }
    @Transactional BookingView advanceBooking(Actor actor,long id,StatusInput input) { requireOperator(actor);Booking b=bookings.findById(id).orElseThrow(()->fail(404,"预约不存在"));
        if(b.status.equals("REQUESTED")&&input.status().equals("CONFIRMED")) {
            if(input.quotedTotalCents()==null || input.quotedTotalCents()<0 || input.quotedTotalCents()>100_000_000) throw fail(400,"确认预约需提供合法报价（分）");
            b.quotedTotalCents=input.quotedTotalCents();
        } else if(!(b.status.equals("CONFIRMED")&&input.status().equals("COMPLETED"))) throw fail(409,"不允许跳过预约处理阶段");
        b.status=input.status();b.stamp(clock.instant());return BookingView.from(b);
    }
}
