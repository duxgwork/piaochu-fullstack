package com.piaochu.app.presentation
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.*
import com.piaochu.app.BuildConfig
import com.piaochu.app.data.*
import com.piaochu.app.databinding.ActivityMainBinding
import com.piaochu.app.domain.*
import java.math.BigDecimal
import java.time.*
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
/** 页面负责展示与输入；所有网络动作经 ViewModel → 领域接口 → Retrofit，便于逐层断点。 */
class MainActivity:AppCompatActivity(){
    private lateinit var binding:ActivityMainBinding
    private lateinit var vm:KitchenViewModel
    private lateinit var settings:NetworkSettings
    private var editor:AlertDialog?=null
    private var editorError:TextView?=null
    private var seenSuccess=0
    private var seenUser:Long?=null
    private val green=0xFF204D3C.toInt();private val orange=0xFFCE4D2C.toInt();private val muted=0xFF73776D.toInt()
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState);WindowCompat.setDecorFitsSystemWindows(window,false)
        binding=ActivityMainBinding.inflate(layoutInflater);setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root){view,insets->
            val safe=insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime());view.setPadding(safe.left,safe.top,safe.right,safe.bottom);insets
        }
        settings=NetworkSettings(this)
        vm=ViewModelProvider(this,KitchenViewModel.Factory(KitchenApi.create(this,settings.read())))[KitchenViewModel::class.java]
        seenSuccess=vm.state.value.success;seenUser=vm.state.value.user?.id
        binding.refreshButton.setOnClickListener{vm.refresh()}
        binding.settingsButton.visibility=if(BuildConfig.DEBUG)View.VISIBLE else View.GONE
        binding.settingsButton.setOnClickListener{networkSettings()}
        listOf(binding.menuTab to Screen.MENU,binding.servicesTab to Screen.SERVICES,binding.ordersTab to Screen.ORDERS,binding.profileTab to Screen.PROFILE).forEach{(button,screen)->
            button.setOnClickListener{vm.navigate(screen);(binding.body.parent as ScrollView).scrollTo(0,0)}
        }
        binding.checkoutButton.setOnClickListener{checkout()}
        lifecycleScope.launch{repeatOnLifecycle(Lifecycle.State.STARTED){vm.state.collect{render(it)}}}
    }
    private fun render(s:KitchenState){
        if(seenUser!=null&&s.user==null)editor?.dismiss();seenUser=s.user?.id
        if(s.success!=seenSuccess){seenSuccess=s.success;editor?.dismiss()}
        editor?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled=!s.busy;editorError?.text=s.error.orEmpty()
        binding.progress.visibility=if(s.busy)View.VISIBLE else View.GONE
        binding.errorLabel.text=s.error.orEmpty();binding.errorLabel.visibility=if(s.error==null)View.GONE else View.VISIBLE
        binding.refreshButton.isEnabled=!s.busy;binding.settingsButton.isEnabled=!s.busy
        listOf(binding.menuTab to Screen.MENU,binding.servicesTab to Screen.SERVICES,binding.ordersTab to Screen.ORDERS,binding.profileTab to Screen.PROFILE).forEach{(b,screen)->
            b.setTextColor(if(s.screen==screen)orange else muted);b.setTypeface(null,if(s.screen==screen)Typeface.BOLD else Typeface.NORMAL)
        }
        binding.body.removeAllViews()
        when(s.screen){Screen.MENU->menu(s);Screen.SERVICES->services();Screen.ORDERS->orders(s);Screen.PROFILE->profile(s)}
        val subtotal=s.catalog?.dishes?.sumOf{it.priceCents*(s.cart[it.id]?:0)}?:0
        binding.checkoutBar.visibility=if(s.screen==Screen.MENU&&s.cart.isNotEmpty())View.VISIBLE else View.GONE
        binding.cartLabel.text="已选 ${s.cart.values.sum()} 份 · ¥${money(subtotal)}"
        binding.checkoutButton.text="去下单";binding.checkoutButton.isEnabled=!s.busy
    }
    private fun menu(s:KitchenState){
        val hero=card(binding.body,0xFFE8EEE2.toInt());label(hero,"中餐移动厨房",12,green);label(hero,"今天，吃点热乎的",27,green,true)
        label(hero,"随时随地随心，享一餐好滋味。",14,muted)
        val kitchen=s.catalog
        label(hero,kitchen?.kitchenName?:"正在连接厨房…",13,green)
        if(kitchen?.demoMode==true)label(binding.body,"开发演示 · 示例菜品与价格，不接受真实支付",12,orange)
        val shortcut=row(binding.body);button(shortcut,"团餐直送",false){vm.navigate(Screen.SERVICES)};button(shortcut,"私厨上门",false){vm.navigate(Screen.SERVICES)}
        val scroll=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false};binding.body.addView(scroll)
        val categories=LinearLayout(this);scroll.addView(categories)
        (listOf("全部")+(kitchen?.dishes?.map{it.category}?.distinct()?:emptyList())).forEach{category->
            button(categories,category,s.category==category){vm.category(category)}
        }
        section("现做中餐", "一人好好吃，多人一起吃")
        if(kitchen==null){label(binding.body,if(s.busy)"菜单加载中…"else"暂未取得菜单，请点击刷新重试",15,muted);return}
        if(kitchen.dishes.isEmpty())label(binding.body,"厨房尚未上架菜品",15,muted)
        kitchen.dishes.filter{s.category=="全部"||it.category==s.category}.forEach{dish->
            val c=card(binding.body);val r=row(c)
            r.addView(DishArtView(this,dish.artKey),LinearLayout.LayoutParams(dp(92),dp(106)))
            val detail=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),0,0,0)};r.addView(detail,LinearLayout.LayoutParams(0,-2,1f))
            label(detail,dish.name,18,green,true);label(detail,dish.description.replace("示例菜品。",""),12,muted)
            val controls=row(detail);label(controls,"¥${money(dish.priceCents)}",19,orange,true,1f)
            val qty=s.cart[dish.id]?:0
            if(qty>0){small(controls,"−","减少${dish.name}"){vm.add(dish,-1)};label(controls,qty.toString(),15,green)}
            small(controls,"＋","加购${dish.name}"){vm.add(dish,1)}
            if(dish.isDemo)label(detail,"示例 · 插画示意",10,muted)
        }
        label(binding.body,"配送费 ¥${money(kitchen.deliveryCents)}，满 ¥${money(kitchen.freeDeliveryThresholdCents)} 免配送费。\n提交后待商家确认，线下协商付款与履约。",12,muted)
    }
    private fun services(){
        section("不止一餐，还有一席", "团餐直送 · 私厨上门")
        if(vm.state.value.catalog?.demoMode==true)label(binding.body,"开发演示 · 预约只保存到测试服务，不会派出真实厨师或配送人员。",12,orange)
        val group=card(binding.body,0xFFE8EEE2.toInt());label(group,"团餐直送",26,green,true);label(group,"把一群人的用餐，安排妥当。",14,muted)
        label(group,"高铁航空 / 油站电信 / 企业工餐 / 家庭月餐",13,green)
        button(group,"预约团餐",true){bookingForm("GROUP")}
        val chef=card(binding.body,0xFFF8E8D4.toInt());label(chef,"私厨上门",26,green,true);label(chef,"厨房交给厨师，相聚留给你。",14,muted)
        label(chef,"家宴代工 / 商务会餐 / 现场烹饪 / 定制餐宴",13,green)
        button(chef,"预约私厨",true){bookingForm("CHEF")}
        label(binding.body,"预约只提交用餐需求。商家联系确认菜单、时间与报价后再履约；人均预算不是最终成交价格。",13,muted)
    }
    private fun orders(s:KitchenState){
        section("每一餐，都有着落", "最近100笔零售订单与100条服务预约")
        if(s.user==null){loginCard();return}
        label(binding.body,"零售订单",19,green,true)
        if(s.orders.isEmpty())label(binding.body,"还没有点餐订单，去选一份热乎的吧。",14,muted)
        s.orders.forEach{o->
            val c=card(binding.body);label(c,"零售订单 #${o.id}",13,muted);label(c,statusLabel(o.status),19,green,true)
            label(c,o.items.joinToString("、"){"${it.name} ×${it.quantity}"},14,green)
            label(c,"合计 ¥${money(o.totalCents)} · ${date(o.createdAt)}",13,orange)
            if(o.isDemo)label(c,"示例订单 · 无线上支付",11,muted)
            val actions=row(c);button(actions,"查看详情",false){orderDetail(o)}
            if(o.status=="PENDING_CONFIRMATION")button(actions,"取消订单",false){confirm("取消订单 #${o.id}？"){vm.cancelOrder(o.id)}}
            if(o.status=="DELIVERING")button(actions,"确认收货",true){confirm("确认已收到餐品？"){vm.receiveOrder(o.id)}}
        }
        label(binding.body,"服务预约",19,green,true)
        if(s.bookings.isEmpty())label(binding.body,"团餐和私厨预约会显示在这里。",14,muted)
        s.bookings.forEach{b->
            val c=card(binding.body);label(c,"${if(b.serviceType=="GROUP")"团餐"else"私厨"}预约 #${b.id}",13,muted)
            label(c,"${b.scene} · ${statusLabel(b.status)}",18,green,true)
            label(c,"${date(b.scheduledAt)} · ${b.people} 人",14,green)
            label(c,if(b.quotedTotalCents==null)"人均预算 ¥${money(b.budgetCents)} · 报价待确认"else"商家报价 ¥${money(b.quotedTotalCents)}",13,orange)
            val actions=row(c);button(actions,"预约详情",false){message("预约 #${b.id}","${b.scene}\n${date(b.scheduledAt)}\n${b.people} 人 · 人均预算 ¥${money(b.budgetCents)}\n${b.contactName} ${b.phone}\n${b.address}\n备注：${b.note.ifBlank{"无"}}\n${statusLabel(b.status)}\n${b.quotedTotalCents?.let{"报价 ¥${money(it)}"}?:"尚未报价"}")}
            if(b.status=="REQUESTED")button(actions,"取消预约",false){confirm("取消预约 #${b.id}？"){vm.cancelBooking(b.id)}}
        }
    }
    private fun profile(s:KitchenState){
        section("我的漂厨", "认真对待每一顿饭")
        val user=s.user
        if(user==null)loginCard()else{
            val c=card(binding.body,0xFFE8EEE2.toInt());label(c,user.displayName,25,green,true);label(c,"账号 ${user.username}",14,muted)
            button(c,"退出登录",false){confirm("退出当前账号？"){vm.logout()}}
            val heading=row(binding.body);label(heading,"常用地址",20,green,true,1f);button(heading,"新增地址",false){addressForm(null)}
            if(s.addresses.isEmpty())label(binding.body,"添加地址后，即可点餐或预约服务。",14,muted)
            s.addresses.forEach{a->
                val address=card(binding.body);label(address,"${a.contactName}  ${a.phone}",16,green,true);label(address,a.detail,14,muted)
                if(a.label.isNotBlank())label(address,a.label,12,orange)
                val actions=row(address);button(actions,"编辑地址",false){addressForm(a)};button(actions,"删除地址",false){confirm("删除这个地址？历史订单会保留地址快照。"){vm.deleteAddress(a.id)}}
            }
        }
        val about=card(binding.body);label(about,"随时随地随心，享尽天下美食",17,green,true)
        label(about,"漂厨 · 中餐移动厨房\n零售连锁 / 团餐直送 / 私厨上门",14,muted)
        button(about,"分享漂厨",false){startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,"漂厨 · 中餐移动厨房\n零售点餐、团餐直送、私厨上门。当前为开发体验版本。")},"分享漂厨"))}
        button(about,"数据与体验说明",false){message("数据与体验说明","注册账号与主动提交的联系人、手机号、地址、用餐需求和订单保存在你连接的服务端。登录令牌在本机使用 Keystore 加密保存。\n\n当前为开发体验版本，菜单及价格为示例，不提供线上支付、自动派单或真实经营承诺。正式运营前需配置真实菜单、业务资质、隐私政策、备案及支付/配送服务。")}
        if(BuildConfig.DEBUG)label(binding.body,"接口 ${settings.read().baseUrl}\n当前代理 ${settings.read().proxyHost.ifBlank{"系统代理或直连"}}",11,muted)
    }
    private fun loginCard(){val c=card(binding.body,0xFFE8EEE2.toInt());label(c,"登录，开启一餐好时光",21,green,true);label(c,"订单、预约和地址仅对自己的账号可见。",14,muted);button(c,"登录 / 注册",true){authForm(false)}}
    private fun authForm(register:Boolean){
        val form=form();label(form,if(register)"创建自己的漂厨账号"else"欢迎回来",16,green,true)
        val username=field(form,"账号（4–32位字母、数字或下划线）","",32,"username")
        val name=if(register)field(form,"昵称","",40,"displayName")else null
        val password=field(form,"密码（至少8位）","",128,"password",InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        label(form,"仅向当前配置的服务端发送账号信息。请使用开发专用密码。",12,muted)
        val dialog=dialog(if(register)"注册账号"else"账号登录",form,if(register)"注册并登录"else"登录"){
            vm.authenticate(register,username.text.toString(),name?.text.toString(),password.text.toString())
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).text=if(register)"已有账号？登录"else"去注册"
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener{dialog.dismiss();authForm(!register)}
    }
    private fun addressForm(address:Address?){
        val f=form();val contact=field(f,"联系人",address?.contactName.orEmpty(),40,"contact")
        val phone=field(f,"手机号码",address?.phone.orEmpty(),11,"phone",InputType.TYPE_CLASS_PHONE)
        val detail=field(f,"城市、街道和详细门牌",address?.detail.orEmpty(),200,"detail")
        val tag=field(f,"地址标签，如家、公司",address?.label.orEmpty(),20,"label")
        dialog(if(address==null)"新增地址"else"编辑地址",f,"保存地址"){vm.saveAddress(address?.id,contact.text.toString(),phone.text.toString(),detail.text.toString(),tag.text.toString())}
    }
    private fun needsAddress():Boolean{
        if(vm.state.value.user==null){authForm(false);return true}
        if(vm.state.value.addresses.isEmpty()){vm.navigate(Screen.PROFILE);addressForm(null);return true}
        return false
    }
    private fun checkout(){
        if(needsAddress())return
        val s=vm.state.value;val f=form();val addresses=s.addresses
        label(f,"收餐地址",14,green,true);val address=spinner(f,addresses.map{"${it.contactName} · ${it.detail}"},"checkoutAddress")
        val dishes=s.catalog?.dishes.orEmpty().filter{s.cart.containsKey(it.id)}
        val subtotal=dishes.sumOf{it.priceCents*s.cart.getValue(it.id)}
        val delivery=if(subtotal>=(s.catalog?.freeDeliveryThresholdCents?:5900))0 else s.catalog?.deliveryCents?:500
        label(f,dishes.joinToString("\n"){"${it.name} ×${s.cart[it.id]}  ¥${money(it.priceCents*(s.cart[it.id]?:0))}"},15,green)
        label(f,"商品 ¥${money(subtotal)} + 配送 ¥${money(delivery)}\n预估合计 ¥${money(subtotal+delivery)}，以服务端订单回执为准。",14,orange)
        val note=field(f,"口味与用餐备注（选填）","",500,"orderNote")
        label(f,"当前订单提交后待商家确认，线下协商付款和履约。示例菜单仅供联调。",12,muted)
        dialog("确认点餐","提交订单",f){vm.order(addresses[address.selectedItemPosition].id,note.text.toString())}
    }
    private fun bookingForm(type:String){
        if(needsAddress())return
        val f=form();val addresses=vm.state.value.addresses
        label(f,"用餐场景",14,green,true)
        val scene=spinner(f,if(type=="GROUP")listOf("高铁航空","油站电信","企业工餐","家庭月餐")else listOf("家宴代工","商务会餐","现场烹饪","定制餐宴"),"scene")
        var whenToServe=LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1).atTime(18,0)
        val schedule=button(f,"用餐时间 ${whenToServe.format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm"))}",false){}
        schedule.contentDescription="选择用餐时间"
        schedule.setOnClickListener{
            DatePickerDialog(this,{_,year,month,day->
                TimePickerDialog(this,{_,hour,minute->whenToServe=LocalDateTime.of(year,month+1,day,hour,minute);schedule.text="用餐时间 ${whenToServe.format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm"))}"},whenToServe.hour,whenToServe.minute,true).show()
            },whenToServe.year,whenToServe.monthValue-1,whenToServe.dayOfMonth).apply{datePicker.minDate=System.currentTimeMillis()}.show()
        }
        val people=field(f,"用餐人数","",5,"people",InputType.TYPE_CLASS_NUMBER)
        val budget=field(f,"人均预算（元，至少10元）","",8,"budget",InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        label(f,"服务地址",14,green,true);val address=spinner(f,addresses.map{"${it.contactName} · ${it.detail}"},"bookingAddress")
        val note=field(f,"过敏原、菜单偏好和其他需求（选填）","",500,"bookingNote")
        label(f,"至少提前2小时预约。人均预算用于沟通需求，最终报价由商家确认。",12,muted)
        dialog(if(type=="GROUP")"预约团餐"else"预约私厨",f,"提交预约"){
            val count=people.text.toString().toIntOrNull()
            val cents=runCatching{BigDecimal(budget.text.toString()).movePointRight(2).longValueExact()}.getOrNull()
            if(count==null||count !in 1..50000||cents==null||cents !in 1000..1000000){vm.error("请填写有效人数和人均预算（10至10000元，最多两位小数）");return@dialog}
            vm.book(BookingDraft(type,scene.selectedItem.toString(),whenToServe.atOffset(ZoneOffset.ofHours(8)).toString(),count,cents,addresses[address.selectedItemPosition].id,note.text.toString()))
        }
    }
    private fun orderDetail(o:FoodOrder){message("订单 #${o.id}","${statusLabel(o.status)}\n${o.items.joinToString("\n"){"${it.name} ×${it.quantity}  ¥${money(it.unitCents*it.quantity)}"}}\n配送费 ¥${money(o.deliveryCents)}\n合计 ¥${money(o.totalCents)}\n${o.contactName} ${o.phone}\n${o.address}\n备注：${o.note.ifBlank{"无"}}\n线下协商付款，未进行线上扣款。")}
    private fun networkSettings(){
        val old=settings.read();val f=form();label(f,"模拟器访问电脑用 10.0.2.2。HTTP 调试无需证书；HTTPS 抓包需安装用户 CA。",13,muted)
        val base=field(f,"接口根地址",old.baseUrl,300,"baseUrl",InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
        val host=field(f,"代理主机（留空为系统代理）",old.proxyHost,100,"proxyHost")
        val port=field(f,"代理端口",old.proxyPort.toString(),5,"proxyPort",InputType.TYPE_CLASS_NUMBER)
        label(f,"Charles：模拟器代理 10.0.2.2:8888；后端在电脑时接口可用 http://127.0.0.1:8090/。切换接口会切换到该环境的账号和数据。",12,muted)
        dialog("接口与抓包",f,"保存并连接"){
            try{val config=NetworkConfig(base.text.toString(),host.text.toString().trim(),port.text.toString().toIntOrNull()?:0);NetworkSettings.validate(config);settings.save(config);editor?.dismiss();vm.reconnect(KitchenApi.create(this,settings.read()))}
            catch(e:Exception){vm.error(e.message?:"配置无效")}
        }
    }
    private fun section(title:String,subtitle:String){label(binding.body,title,26,green,true);label(binding.body,subtitle,13,muted)}
    private fun card(parent:LinearLayout,color:Int=Color.WHITE):LinearLayout=LinearLayout(this).apply{
        orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(16),dp(16),dp(14));background=rounded(color,18)
        parent.addView(this,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12);bottomMargin=dp(3)})
    }
    private fun row(parent:LinearLayout)=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;parent.addView(this,LinearLayout.LayoutParams(-1,-2))}
    private fun label(parent:LinearLayout,value:String,size:Int=14,color:Int=green,bold:Boolean=false,weight:Float=0f)=TextView(this).apply{
        text=value;textSize=size.toFloat();setTextColor(color);setLineSpacing(dp(3).toFloat(),1f);if(bold)setTypeface(null,Typeface.BOLD)
        setPadding(0,dp(4),0,dp(4));parent.addView(this,if(weight>0)LinearLayout.LayoutParams(0,-2,weight)else LinearLayout.LayoutParams(-2,-2))
    }
    private fun button(parent:LinearLayout,value:String,primary:Boolean,action:()->Unit):Button=Button(this).apply{
        text=value;textSize=14f;isAllCaps=false;minHeight=dp(48);setTextColor(if(primary)Color.WHITE else green)
        backgroundTintList=null;background=rounded(if(primary)green else 0xFFF0F1E9.toInt(),12);setPadding(dp(12),0,dp(12),0)
        isEnabled=!vm.state.value.busy;setOnClickListener{action()}
        parent.addView(this,LinearLayout.LayoutParams(if(parent.orientation==LinearLayout.VERTICAL)-1 else -2,dp(48)).apply{topMargin=dp(8);rightMargin=dp(6);bottomMargin=dp(5)})
    }
    private fun small(parent:LinearLayout,value:String,description:String,action:()->Unit)=button(parent,value,value=="＋",action).apply{
        contentDescription=description;minWidth=dp(44);minimumWidth=dp(44);layoutParams=LinearLayout.LayoutParams(dp(44),dp(48)).apply{leftMargin=dp(2)}
    }
    private fun form()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(22),dp(12),dp(22),dp(12))}
    private fun field(parent:LinearLayout,hint:String,value:String,max:Int,tag:String,type:Int=InputType.TYPE_CLASS_TEXT)=EditText(this).apply{
        this.hint=hint;this.tag=tag;setText(value);inputType=type;textSize=15f;minHeight=dp(56);filters=arrayOf(InputFilter.LengthFilter(max));parent.addView(this,LinearLayout.LayoutParams(-1,-2))
    }
    private fun spinner(parent:LinearLayout,values:List<String>,tag:String)=Spinner(this).apply{this.tag=tag;adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,values);parent.addView(this,LinearLayout.LayoutParams(-1,dp(56)))}
    private fun dialog(title:String,positive:String,content:LinearLayout,action:()->Unit)=dialog(title,content,positive,action)
    private fun dialog(title:String,content:LinearLayout,positive:String,action:()->Unit):AlertDialog{
        val error=TextView(this).apply{setTextColor(orange);textSize=13f};content.addView(error)
        val scroll=ScrollView(this).apply{addView(content)}
        val d=AlertDialog.Builder(this).setTitle(title).setView(scroll).setNegativeButton("取消",null).setPositiveButton(positive,null).create()
        editor=d;editorError=error;d.setOnDismissListener{if(editor===d){editor=null;editorError=null}}
        d.setOnShowListener{d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{action()}}
        d.show();return d
    }
    private fun message(title:String,body:String){AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("知道了",null).show()}
    private fun confirm(title:String,action:()->Unit){AlertDialog.Builder(this).setTitle(title).setNegativeButton("返回",null).setPositiveButton("确认"){_,_->action()}.show()}
    private fun rounded(color:Int,radius:Int)=GradientDrawable().apply{setColor(color);cornerRadius=dp(radius).toFloat()}
    private fun date(value:String):String=runCatching{DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.of("Asia/Shanghai")).format(Instant.parse(value))}.getOrElse{value}
    private fun dp(value:Int)=(resources.displayMetrics.density*value).toInt()
}
