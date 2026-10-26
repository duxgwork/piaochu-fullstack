# 漂厨 · 中餐移动厨房

根据《漂厨说明书》中的零售连锁、团餐直送和私厨上门业务建立的原生 Android + Java 工程。Android 使用 Kotlin、XML/ViewBinding、ViewModel、协程和 Retrofit；Java 使用 Spring Boot、JPA 与 Flyway。`android/` 和 `server/` 可分别在 Android Studio 和 IntelliJ IDEA 打开。

首版覆盖账号注册/登录/退出、地址增改删、分类菜单、购物袋、零售下单与取消/收货、团餐/私厨预约及取消、个人订单查询。默认菜品、插画与价格均为明确标注的示例，预约保存到本地服务，不会自动派出厨师。订单从待确认开始，零售按线下确认付款与履约处理，预约报价由运营确认；没有模拟扣款、虚构销量或自动配送。

## 本地启动

需要 JDK 17+、Android SDK 36、支持 AGP 9.4 的 Android Studio。macOS 启动脚本自动识别 Android Studio 自带的 JDK；Gradle Wrapper、Maven Wrapper 已包含在项目中。首次构建需要联网下载依赖。

第一个终端：

```bash
cd /Users/nevin/Documents/piaochu-fullstack
./scripts/run-server.sh
```

服务默认使用 `8090` 端口，独立于待办工程的8080。默认 H2 数据库为 `server/data/piaochu.mv.db`，用户、地址、订单、预约及登录会话重启后保留。请从 `server/` 启动 Java，避免相对路径产生另一份数据库；同一文件不能同时由两份服务或数据库工具打开。

第二个终端，在 Android Studio 启动模拟器后：

```bash
cd /Users/nevin/Documents/piaochu-fullstack
./scripts/run-android.sh
```

脚本构建、安装并启动 `com.piaochu.app.debug`。多设备时设置 `ANDROID_SERIAL`。也可直接在 Android Studio 打开 `android/` 并 Run。调试 APK 为 `android/app/build/outputs/apk/debug/app-debug.apk`。

App 默认根地址 `http://10.0.2.2:8090/` 指向模拟器宿主机。USB 真机可使用脚本建立的 `adb reverse`，在联调设置中改为 `http://127.0.0.1:8090/`；局域网真机改为电脑局域网 IP，并允许本机防火墙访问8090。接口设置只填根地址，不附加 `/api`。

启动后在“我的”注册自己的账号，再添加地址即可点餐或预约。不提供默认顾客账号；账号支持4至32位字母、数字或下划线，密码至少8位。服务预约使用中国标准时间，需提前至少2小时，最远一年。

## 接口与抓包

Charles 在电脑监听8888时，在App“联调”中设置：

- 接口根地址 `http://127.0.0.1:8090/`。
- 模拟器抓包代理主机 `10.0.2.2`，端口 `8888`。
- 允许 Charles 的设备连接请求，操作App后检查 `/api/orders`、`/api/bookings` 等请求。

HTTP无需安装证书。HTTPS抓包需安装用户CA，并在Charles对目标域名启用SSL Proxying；debug source set 信任用户主动安装的CA，release只信任系统CA并要求HTTPS。默认TLS及主机名校验始终保留。

也可以运行本项目的HTTP调试代理：

```bash
python3 scripts/capture-proxy.py
```

此时App代理端口改为 `18890`，主机仍为 `10.0.2.2`，接口仍为 `http://127.0.0.1:8090/`。该代理只允许转发本机8090，不支持HTTPS；`captures/http.jsonl` 对密码、令牌和联系方式脱敏。普通服务日志及Logcat仅记录方法、路径、状态码和请求编号，不输出正文或Authorization。不要把Charles会话与本地数据库提交到Git。

调试完清空App代理及系统代理，恢复正确的直连接口地址，避免代理工具关闭后无法联网。

## 业务边界与代码入口

Android从 `presentation/MainActivity.kt` 的操作进入 `KitchenViewModel.kt`，通过 `domain/KitchenRepository` 调用 `data/KitchenApi.kt`；Retrofit 注解以 `data/KitchenService.kt` 为准，DTO只在data层映射为领域模型。修改接口地址时按环境隔离登录态，不携带另一后端的令牌或私有数据；401会清理当前账号状态。

Java入口为 `PiaochuApplication.java`。HTTP入口以 `AuthController.java`、`CustomerController.java`、`CatalogController.java` 为准；`AuthService.java` 管理密码与会话，`KitchenService.java` 统一校验地址/订单归属、金额和状态，各 `*Repository.java` 访问数据库，表结构以Flyway迁移为准。中文注释保留长期设计理由。

金额统一为人民币分。零售价格、配送费由服务端计算，并将菜品价格与地址冻结到订单；删除地址不会改变历史订单。客户端提交UUID幂等键，相同请求重复提交返回原记录，改变内容必须使用新键。网络结果未知时先刷新订单核实，客户端不自动重试写请求。

订单由运营依次推进“待确认→已确认→制作中→配送中”，顾客确认收货后完成；仅待确认阶段可直接取消。服务预约由运营填写总报价后确认，履约后完成，仅待联系报价阶段可直接取消。列表分别返回当前账号最近100笔零售订单与100条预约。

密码使用带独立随机盐的PBKDF2，服务端只保存会话令牌SHA-256摘要，会话7天有效且退出即撤销；本机令牌由Android Keystore AES-GCM加密，关闭应用数据备份。接口包含基础单实例限流，多实例部署应在受控入口使用共享限流。

## 运营处理

为方便跑通真实订单生命周期，提供受运营角色保护的脚本入口，不包含商家App。首次启动服务前自行配置：

```bash
export OPERATOR_USERNAME=your_operator
# OPERATOR_PASSWORD从自己的受控环境注入，至少12位，不使用示例固定密码。
./scripts/run-server.sh
```

不配置时不会创建运营账号。环境变量仅用于首次创建；重复启动不会覆盖密码，也不会把同名顾客提升为运营角色。运营脚本交互输入密码，不将密码放在命令参数中：

```bash
python3 scripts/manage-kitchen.py --username your_operator orders
python3 scripts/manage-kitchen.py --username your_operator orders --id 订单ID --status CONFIRMED
python3 scripts/manage-kitchen.py --username your_operator orders --id 订单ID --status PREPARING
python3 scripts/manage-kitchen.py --username your_operator orders --id 订单ID --status DELIVERING
python3 scripts/manage-kitchen.py --username your_operator bookings
python3 scripts/manage-kitchen.py --username your_operator bookings --id 预约ID --status CONFIRMED --quote-cents 96000
python3 scripts/manage-kitchen.py --username your_operator bookings --id 预约ID --status COMPLETED
```

报价以分为单位，`96000`代表960元。顾客不能访问运营接口，也不能通过注册参数指定运营角色。

## 验证与打包

服务启动后，可运行 `python3 scripts/verify-api.py` 进行真实HTTP自检；它创建独立测试账号并取消本次订单/预约，不影响其他用户数据。

```bash
cd /Users/nevin/Documents/piaochu-fullstack/server
./mvnw verify

cd /Users/nevin/Documents/piaochu-fullstack/android
./gradlew testDebugUnitTest assembleDebug lintDebug
```

在JDK未设为系统默认的macOS环境，先 `source ../scripts/java-env.sh`。运行真实设备流程前，先启动Java服务、18890调试代理和模拟器，再执行：

```bash
./gradlew connectedDebugAndroidTest
```

设备测试通过UI注册独立 `ui_` 账号，保存地址、点餐、取消订单并提交两类预约；通过真实接口读回验证，退出登录后恢复原调试设置，不清空其他用户数据。测试账号及其业务记录保留在测试数据库，请使用独立测试环境。

服务端 `./mvnw package` 生成 `server/target/piaochu-server-1.0.0.jar`。正式Android包使用 `-PreleaseApiUrl=https://你的接口域名/` 指定地址，并通过Android Studio的签名向导配置自己的正式证书。默认release地址为不可访问占位域名，命令行未配置正式签名。

## PostgreSQL 与部署

```bash
cd /Users/nevin/Documents/piaochu-fullstack
cp .env.example .env
# 编辑.env，替换数据库密码及可选运营账号密码。
docker compose up --build -d
docker compose logs -f server
```

PostgreSQL不开放宿主机端口，服务端只绑定 `127.0.0.1:8090`。`docker compose down`保留数据库卷，`down -v`会删除数据。H2与PostgreSQL之间不会自动迁移数据。Maven镜像配置在 `server/.mvn/settings.xml`，网络不通时根据环境使用受信任镜像，不关闭TLS验证。

面向真实用户时需要配置真实菜单与服务范围、商家经营资质、隐私政策/账号注销、域名HTTPS和大陆APP备案，并按需要接入支付、配送与通知。`DEMO_DATA=false`只停止初始化示例菜单，不会把既有示例菜品转换为真实菜品。生产环境应使用独立数据库与托管备份，不能把此开发数据直接当作真实经营数据。
