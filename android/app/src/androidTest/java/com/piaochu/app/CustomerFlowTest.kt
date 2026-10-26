package com.piaochu.app
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.piaochu.app.data.*
import com.piaochu.app.presentation.MainActivity
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.`is`
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import android.graphics.Bitmap
/** 启动真实Java服务及调试代理后执行。所有写入走App，接口读回校验；独立测试账号不触碰其他用户数据。 */
@RunWith(AndroidJUnit4::class)
class CustomerFlowTest {
 private val context=ApplicationProvider.getApplicationContext<android.content.Context>()
 private val config=NetworkConfig("http://127.0.0.1:8090/","10.0.2.2",18890)
 private fun api()=KitchenApi.create(context,config)
 private fun field(tag:String,text:String){onView(withTagValue(`is`(tag))).perform(scrollTo(),replaceText(text),closeSoftKeyboard())}
 @Test fun customerCanRegisterSaveAddressOrderAndBookBothServices(){
  val settings=NetworkSettings(context);val previous=settings.read();val store=SessionStore(context,config.baseUrl);val previousSession=store.session
  store.clear();settings.save(config)
  val username="ui_"+System.currentTimeMillis()
  val animationKeys=listOf("window_animation_scale","transition_animation_scale","animator_duration_scale")
  val animationValues=animationKeys.associateWith{shell("settings get global $it")}
  try{
   animationKeys.forEach{shell("settings put global $it 0")}
   ActivityScenario.launch(MainActivity::class.java).use{scenario->
    eventually{onView(withContentDescription("加购川味水煮牛肉")).check(matches(isDisplayed()))};capture("menu")
    onView(withContentDescription("加购川味水煮牛肉")).perform(scrollTo(),click())
    onView(withId(R.id.profileTab)).perform(click());onView(withText("登录 / 注册")).perform(click())
    onView(withText("去注册")).inRoot(isDialog()).perform(click())
    field("username",username);field("displayName","端到端验证用户");field("password","app-e2e-pass-123")
    onView(withText("注册并登录")).inRoot(isDialog()).perform(click())
    eventually{onView(withText("新增地址")).check(matches(isEnabled()))}
    onView(withText("新增地址")).perform(click());field("contact","联调联系人");field("phone","13800000000");field("detail","成都市 高新区 联调地址");field("label","公司")
    onView(withText("保存地址")).inRoot(isDialog()).perform(click())
    eventually{assertEquals(1,runBlocking{api().addresses()}.size)}
    // 每次api()从Keystore重新加载会话；重建Activity验证购物袋与地址能跨配置变更保留。
    scenario.recreate();eventually{onView(withText("联调联系人  13800000000")).check(matches(isDisplayed()))}
    onView(withId(R.id.menuTab)).perform(click())
    onView(withId(R.id.checkoutButton)).perform(click());field("orderNote","少辣，不放葱")
    onView(withText("提交订单")).inRoot(isDialog()).perform(click())
    eventually{assertEquals(1,runBlocking{api().orders()}.size)}
    val order=runBlocking{api().orders().single()};assertEquals("PENDING_CONFIRMATION",order.status);assertEquals("少辣，不放葱",order.note);assertEquals(3700,order.totalCents)
    eventually{onView(withText("取消订单")).check(matches(isEnabled()))}
    onView(withText("取消订单")).perform(scrollTo(),click());onView(withText("确认")).inRoot(isDialog()).perform(click())
    eventually{assertEquals("CANCELLED",runBlocking{api().orders().single().status})}
    for((label,type) in listOf("预约团餐" to "GROUP","预约私厨" to "CHEF")){
     onView(withId(R.id.servicesTab)).perform(click());eventually{onView(withText(label)).check(matches(isEnabled()))};onView(withText(label)).perform(scrollTo(),click())
     field("people","12");field("budget","80");field("bookingNote","花生过敏，提交后联系确认")
     onView(withText("提交预约")).inRoot(isDialog()).perform(click())
     eventually{assertTrue(runBlocking{api().bookings().any{it.serviceType==type}})}
    }
    val bookings=runBlocking{api().bookings()};assertEquals(2,bookings.size);assertTrue(bookings.all{it.quotedTotalCents==null&&it.status=="REQUESTED"})
    capture("orders")
    onView(withId(R.id.servicesTab)).perform(click());capture("services")
    onView(withId(R.id.profileTab)).perform(click());capture("profile")
    eventually{onView(withText("退出登录")).check(matches(isEnabled()))};onView(withText("退出登录")).perform(scrollTo(),click());onView(withText("确认")).inRoot(isDialog()).perform(click())
    eventually{onView(withText("登录 / 注册")).check(matches(isDisplayed()))}
   }
  }finally{
   runBlocking{runCatching{api().logout()}}
   store.clear();if(previousSession!=null)store.save(previousSession);settings.save(previous)
   animationValues.forEach{(key,value)->shell(if(value=="null")"settings delete global $key"else "settings put global $key $value")}
  }
 }
 private fun shell(command:String):String {
  val fd=InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
  return android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().use{it.readText().trim()}
 }
 private fun capture(name:String){
  InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let{bitmap->File(context.filesDir,"evidence-$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
 }
 private fun eventually(assertion:()->Unit){val end=System.nanoTime()+30_000_000_000L;var last:Throwable?=null
  while(System.nanoTime()<end){try{assertion();return}catch(t:Throwable){last=t};Thread.sleep(150)};throw AssertionError("等待页面或接口收敛超时",last)
 }
}
