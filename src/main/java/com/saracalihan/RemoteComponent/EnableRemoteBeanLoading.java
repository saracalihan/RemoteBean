package com.saracalihan.RemoteComponent;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Remote class loading özelliğini aktif etmek için kullanılan annotation.
 * Bu annotation Spring Boot uygulamasının main class'ına eklenerek
 * @RemoteBean ile işaretlenmiş interface'lerin taranması ve
 * uzak implementasyonlarının yüklenmesi sağlanır.
 * 
 * Kullanım:
 * <pre>
 * @SpringBootApplication
 * @EnableRemoteBeanLoading
 * public class Application {
 *     public static void main(String[] args) {
 *         SpringApplication.run(Application.class, args);
 *     }
 * }
 * </pre>
 * 
 * @author saracalihan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(RemoteBeanRegistrar.class)
public @interface EnableRemoteBeanLoading {
    /**
     * Taranacak base package'lar.
     * Boş bırakılırsa, annotation'ın bulunduğu package taranır.
     * 
     * @return Taranacak package isimleri
     */
    String[] basePackages() default {};
}
