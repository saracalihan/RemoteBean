# Remote Class Loader Anotation

Java Custom Annotation ile Uzaktan Class Yükleme ve Spring Bean Kaydı

Bu proje, bir interface üzerine uygulanan custom annotation (`@RemoteBean`) aracılığıyla belirtilen URL'den bir Java class dosyasını (uzak implementasyon) çekmek, Class Loader ile yüklemek ve bu sınıfı Spring IoC Container'a `@Autowired` kullanılabilir bir Bean olarak kaydetmeyi sağlar.

## Setup

### Maven Dependency

```xml
<dependency>
    <groupId>com.saracalihan</groupId>
    <artifactId>remote-component-annotation</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usagee

```java
import com.saracalihan.RemoteComponent.RemoteBean;

// @RemoteBean(
//     value = "https://secure-server.com/classes/MyServiceImpl.class",
//     bearerToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
// )
@RemoteBean("http://localhost:8080/classes/MyServiceImpl.class")
public interface MyService {
    String getMessage();
    int calculate(int a, int b);
}
```

### 2. Remote Class Loading'i Aktif Etme

Spring Boot uygulamanızın main class'ına `@EnableRemoteBeanLoading` annotation'ını ekleyin:

```java
import com.saracalihan.RemoteComponent.EnableRemoteBeanLoading;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRemoteBeanLoading
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. Bean'i Kullanma

Interface'i `@Autowired` ile inject edin:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MyConsumer {

    @Autowired
    private MyService myService;

    public void useService() {
        String message = myService.getMessage();
        int result = myService.calculate(10, 20);
    }
}
```

## Paramaters

### @RemoteBean

| Parametre | Tip | Zorunlu | Açıklama |
|-----------|-----|---------|----------|
| `value` | String | Evet | Uzak class dosyasının URL adresi |
| `beanName` | String | Hayır | Bean'in Spring context'teki adı. Belirtilmezse interface adından otomatik üretilir. |
| `bearerToken` | String | Hayır | HTTP isteğinde kullanılacak Bearer Token. Belirtilirse Authorization header'ına eklenir. Belirtilmezse `REMOTE_BEAN_BEARER_TOKEN` ortam değişkeni kontrol edilir. |

### @EnableRemoteBeanLoading

| Parametre | Tip | Zorunlu | Açıklama |
|-----------|-----|---------|----------|
| `basePackages` | String[] | Hayır | Taranacak base package'lar. Boş bırakılırsa annotation'ın bulunduğu package taranır. |


## Lisans

Bu proje [MIT lisansı](./LICENSE) altında lisanslanmıştır.

