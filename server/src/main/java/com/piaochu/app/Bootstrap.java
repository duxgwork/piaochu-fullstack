package com.piaochu.app;
import java.time.Clock;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component
class Bootstrap implements ApplicationRunner {
    private final MenuRepository menu;private final AccountRepository accounts;private final Clock clock;
    @Value("${app.demo-data:true}") boolean demo;
    @Value("${app.operator.username:}") String operator;
    @Value("${app.operator.password:}") String password;
    Bootstrap(MenuRepository m,AccountRepository a,Clock c){menu=m;accounts=a;clock=c;}
    @Override @Transactional public void run(ApplicationArguments args) {
        // 仅初始化明确标注的演示菜单，不制造销量、评论、付款或用户订单。
        if(demo&&menu.count()==0) {
            dish("水煮馆","川味水煮牛肉","鲜嫩牛肉搭配时蔬，麻辣鲜香。示例菜品。",3200,"spicy");
            dish("水煮馆","藤椒鱼片","鱼片、青笋与藤椒，一口鲜麻。示例菜品。",3600,"fish");
            dish("砂锅炖","菌菇鸡汤煲","鸡肉与菌菇慢炖，热汤暖胃。示例菜品。",2800,"soup");
            dish("砂锅炖","番茄牛腩煲","番茄浓汤炖牛腩，酸香开胃。示例菜品。",3400,"stew");
            dish("烧菜煲","招牌红烧肉","小火慢烧，搭配应季蔬菜。示例菜品。",3000,"stew");
            dish("烧菜煲","家常宫保鸡丁","鸡丁与花生现炒，咸香微辣。示例菜品。",2400,"spicy");
            dish("蒸菜坊","豉汁蒸排骨","排骨蒸至软嫩，保留食材本味。示例菜品。",2600,"steam");
            dish("蒸菜坊","清蒸时蔬配米饭","清蒸时蔬与一份米饭。示例菜品。",1200,"greens");
        }
        if(!operator.isBlank()||!password.isBlank()) {
            if(!operator.matches("[a-zA-Z0-9_]{4,32}")||password.length()<12) throw new IllegalStateException("运营账号需4至32位，密码至少12位；请通过环境变量配置");
            String username=operator.toLowerCase(Locale.ROOT);var existing=accounts.findByUsername(username);
            if(existing.isPresent()&&!existing.get().role.equals("OPERATOR")) throw new IllegalStateException("运营账号名已被普通用户使用，拒绝自动提权");
            if(existing.isEmpty()) { Account a=new Account();a.username=username;a.displayName="漂厨运营";a.role="OPERATOR";a.passwordHash=Passwords.hash(password);a.stamp(clock.instant());accounts.save(a); }
        }
    }
    private void dish(String category,String name,String description,long cents,String art) {
        MenuItem d=new MenuItem();d.category=category;d.name=name;d.description=description;d.priceCents=cents;d.artKey=art;d.available=true;d.demo=true;d.stamp(clock.instant());menu.save(d);
    }
}
