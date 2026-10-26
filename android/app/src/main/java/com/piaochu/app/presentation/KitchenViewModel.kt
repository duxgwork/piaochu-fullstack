package com.piaochu.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.piaochu.app.domain.Address
import com.piaochu.app.domain.Booking
import com.piaochu.app.domain.BookingDraft
import com.piaochu.app.domain.Catalog
import com.piaochu.app.domain.Dish
import com.piaochu.app.domain.FoodOrder
import com.piaochu.app.domain.KitchenRepository
import com.piaochu.app.domain.ServiceException
import com.piaochu.app.domain.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

enum class Screen { MENU, SERVICES, ORDERS, PROFILE }
data class KitchenState(
    val catalog: Catalog? = null,
    val user: User? = null,
    val addresses: List<Address> = emptyList(),
    val orders: List<FoodOrder> = emptyList(),
    val bookings: List<Booking> = emptyList(),
    val cart: Map<Long, Int> = emptyMap(),
    val screen: Screen = Screen.MENU,
    val category: String = "全部",
    val busy: Boolean = false,
    val error: String? = null,
    val success: Int = 0
)

/** 一次提交对应一个稳定的幂等键。网络失败保留购物袋和原文，刷新结果后再决定是否重试。 */
class KitchenViewModel(private var repository: KitchenRepository) : ViewModel() {
    private val mutable = MutableStateFlow(KitchenState());
    val state = mutable.asStateFlow()
    private var orderDraft: Pair<String, String>? = null;
    private var bookingDraft: Pair<String, String>? = null

    init {
        refresh()
    }

    fun navigate(screen: Screen) {
        mutable.value = mutable.value.copy(screen = screen, error = null)
    }

    fun category(value: String) {
        mutable.value = mutable.value.copy(category = value)
    }

    fun add(dish: Dish, change: Int) {
        if (mutable.value.busy) return
        val cart = mutable.value.cart.toMutableMap();
        val qty = (cart[dish.id] ?: 0) + change
        if (qty > 20) {
            error("单个菜品最多20份，大额用餐可使用团餐预约"); return
        }
        if (qty <= 0) cart.remove(dish.id) else cart[dish.id] = qty
        mutable.value = mutable.value.copy(cart = cart, error = null)
    }

    fun error(text: String) {
        mutable.value = mutable.value.copy(error = text)
    }

    fun refresh() = perform {
        val catalog = repository.catalog(); mutable.value = mutable.value.copy(catalog = catalog)
        if (repository.currentUser() != null) {
            val user = repository.me();
            val addresses = repository.addresses();
            val orders = repository.orders();
            val bookings = repository.bookings()
            mutable.value = mutable.value.copy(
                user = user,
                addresses = addresses,
                orders = orders,
                bookings = bookings
            )
        }
    }

    fun authenticate(register: Boolean, username: String, name: String, password: String) {
        if (username.isBlank() || password.length < 8 || (register && name.isBlank())) {
            error("请填写账号、至少8位密码，以及注册昵称"); return
        }
        val anonymousCart = if (mutable.value.user == null) mutable.value.cart else emptyMap()
        perform {
            val user =
                if (register) repository.register(username, name, password) else repository.login(
                    username,
                    password
                )
            // 登录成功先清除上一账号的所有私有状态，再加载当前账号的数据。
            mutable.value = KitchenState(
                catalog = mutable.value.catalog,
                user = user,
                cart = anonymousCart,
                busy = true,
                screen = Screen.PROFILE,
                success = mutable.value.success + 1
            )
            val addresses = repository.addresses();
            val orders = repository.orders();
            val bookings = repository.bookings()
            mutable.value =
                mutable.value.copy(addresses = addresses, orders = orders, bookings = bookings)
        }
    }

    fun logout() = perform {
        try {
            repository.logout()
        } finally {
            mutable.value = KitchenState(
                catalog = mutable.value.catalog,
                screen = Screen.PROFILE,
                busy = true,
                success = mutable.value.success + 1
            ); orderDraft = null; bookingDraft = null
        }
    }

    fun saveAddress(id: Long?, contact: String, phone: String, detail: String, label: String) {
        if (contact.isBlank() || !phone.matches(Regex("1[3-9][0-9]{9}")) || detail.isBlank()) {
            error("请填写联系人、11位大陆手机号和详细地址"); return
        }
        perform {
            val saved = repository.saveAddress(id, contact, phone, detail, label)
            mutable.value = mutable.value.copy(
                addresses = listOf(saved) + mutable.value.addresses.filterNot { it.id == saved.id },
                success = mutable.value.success + 1
            )
        }
    }

    fun deleteAddress(id: Long) = perform {
        repository.deleteAddress(id); mutable.value = mutable.value.copy(
        addresses = mutable.value.addresses.filterNot { it.id == id },
        success = mutable.value.success + 1
    )
    }

    fun order(addressId: Long, note: String) = perform {
        val cart =
            mutable.value.cart; if (cart.isEmpty()) throw IllegalArgumentException("购物袋还是空的")
        val fingerprint = "$addressId|${cart.toSortedMap()}|${note.trim()}"
        val key =
            orderDraft?.takeIf { it.first == fingerprint }?.second ?: UUID.randomUUID().toString()
                .also { orderDraft = fingerprint to it }
        val saved = repository.createOrder(key, addressId, cart, note)
        orderDraft = null; mutable.value = mutable.value.copy(
        cart = emptyMap(),
        orders = listOf(saved) + mutable.value.orders.filterNot { it.id == saved.id },
        screen = Screen.ORDERS,
        success = mutable.value.success + 1
    )
    }

    fun cancelOrder(id: Long) = perform {
        val saved = repository.cancelOrder(id); mutable.value = mutable.value.copy(
        orders = mutable.value.orders.map { if (it.id == id) saved else it },
        success = mutable.value.success + 1
    )
    }

    fun receiveOrder(id: Long) = perform {
        val saved = repository.receiveOrder(id); mutable.value = mutable.value.copy(
        orders = mutable.value.orders.map { if (it.id == id) saved else it },
        success = mutable.value.success + 1
    )
    }

    fun book(draft: BookingDraft) = perform {
        val fingerprint = draft.toString();
        val key =
            bookingDraft?.takeIf { it.first == fingerprint }?.second ?: UUID.randomUUID().toString()
                .also { bookingDraft = fingerprint to it }
        val saved = repository.createBooking(key, draft); bookingDraft = null
        mutable.value = mutable.value.copy(
            bookings = listOf(saved) + mutable.value.bookings.filterNot { it.id == saved.id },
            screen = Screen.ORDERS,
            success = mutable.value.success + 1
        )
    }

    fun cancelBooking(id: Long) = perform {
        val saved = repository.cancelBooking(id); mutable.value = mutable.value.copy(
        bookings = mutable.value.bookings.map { if (it.id == id) saved else it },
        success = mutable.value.success + 1
    )
    }

    fun reconnect(next: KitchenRepository) {
        if (mutable.value.busy) return; repository = next; orderDraft = null; bookingDraft =
            null; mutable.value = KitchenState(success = mutable.value.success); refresh()
    }

    private fun perform(action: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                if (e is ServiceException && e.status == 401) {
                    repository.forgetSession(); mutable.value = KitchenState(
                        catalog = mutable.value.catalog,
                        screen = Screen.PROFILE,
                        busy = true,
                        success = mutable.value.success
                    )
                }
                val message = when (e) {
                    is ServiceException -> "${e.message}\n请求编号 ${e.requestId ?: "未知"}"; is IOException -> "网络请求未完成：${e.message}\n提交结果可能未知，请先刷新订单核实。"; else -> e.message
                        ?: "操作失败"
                }
                mutable.value = mutable.value.copy(error = message)
            } finally {
                mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    class Factory(private val repository: KitchenRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(KitchenViewModel::class.java)); return KitchenViewModel(
                repository
            ) as T
        }
    }
}
