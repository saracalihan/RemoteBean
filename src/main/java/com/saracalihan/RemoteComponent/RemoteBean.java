package com.saracalihan.RemoteComponent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Uzak bir URL'den class dosyası yüklemek için kullanılan custom annotation.
 * Bu annotation bir interface üzerine uygulanır ve belirtilen URL'den
 * implementasyon sınıfı yüklenerek Spring Bean olarak kaydedilir.
 * 
 * @author saracalihan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RemoteBean {
    /**
     * Uzak class dosyasının URL adresi.
     * Örnek: "http://localhost:8080/classes/MyServiceImpl.class"
     * 
     * @return Uzak class dosyasının tam URL adresi
     */
    String value();
    
    /**
     * Bean'in Spring context'teki adı.
     * Belirtilmezse interface adından otomatik üretilir.
     * 
     * @return Bean adı
     */
    String beanName() default "";
    
    /**
     * HTTP isteğinde kullanılacak Bearer Token.
     * Belirtilirse Authorization header'ına "Bearer {token}" formatında eklenir.
     * 
     * @return Bearer token değeri (opsiyonel)
     */
    String bearerToken() default "";
}
