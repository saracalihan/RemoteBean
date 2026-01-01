package com.saracalihan.RemoteComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.StringUtils;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * @RemoteBean annotation'ı ile işaretlenmiş interface'leri tarayarak,
 *              uzak implementasyonlarını yükleyen ve Spring Bean olarak
 *              kaydeden registrar.
 * 
 *              Bu sınıf Spring'in ImportBeanDefinitionRegistrar mekanizmasını
 *              kullanarak
 *              uygulama başlangıcında otomatik olarak uzak sınıfları yükler ve
 *              kayıt eder.
 * 
 * @author saracalihan
 */
public class RemoteBeanRegistrar implements ImportBeanDefinitionRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(RemoteBeanRegistrar.class);
    private static final String ENV_BEARER_TOKEN = "REMOTE_BEAN_BEARER_TOKEN";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
            BeanDefinitionRegistry registry) {

        Map<String, Object> attributes = importingClassMetadata
                .getAnnotationAttributes(EnableRemoteBeanLoading.class.getName());

        String[] basePackages = (String[]) attributes.get("basePackages");

        if (basePackages == null || basePackages.length == 0) {
            String className = importingClassMetadata.getClassName();
            String packageName = className.substring(0, className.lastIndexOf('.'));
            basePackages = new String[] { packageName };
        }

        logger.info("[RemoteBean] Remote class loading başlatılıyor. Taranacak paketler: {}",
                String.join(", ", basePackages));

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                AnnotationMetadata metadata = beanDefinition.getMetadata();
                return metadata.isIndependent() && (metadata.isInterface() || metadata.isConcrete());
            }
        };

        scanner.addIncludeFilter(new AnnotationTypeFilter(RemoteBean.class));

        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);
            logger.info("[RemoteBean] '{}' paketinde {} adet @RemoteBean bulundu", basePackage, candidateComponents.size());
            for (BeanDefinition beanDefinition : candidateComponents) {
                try {
                    processRemoteBean(beanDefinition, registry);
                } catch (Exception e) {
                    logger.error("[RemoteBean] Remote class yüklenirken hata oluştu: {}",
                            beanDefinition.getBeanClassName(), e);
                    throw new RuntimeException("[RemoteBean] Remote class yükleme hatası", e);
                }
            }
        }

        logger.info("[RemoteBean] Remote class loading tamamlandı");
    }

    /**
     * @RemoteBean annotation'ına sahip bir interface'i işler.
     *              Uzak implementasyonu yükler ve Spring Bean olarak kaydeder.
     */
    private void processRemoteBean(BeanDefinition beanDefinition,
            BeanDefinitionRegistry registry) throws Exception {

        String interfaceName = beanDefinition.getBeanClassName();
        Class<?> interfaceClass = Class.forName(interfaceName);

        RemoteBean RemoteBeanAnnotation = interfaceClass.getAnnotation(RemoteBean.class);

        if (RemoteBeanAnnotation == null) {
            logger.warn("[RemoteBean] @RemoteBean annotation bulunamadı: {}", interfaceName);
            return;
        }

        String remoteUrl = RemoteBeanAnnotation.value();
        String beanName = RemoteBeanAnnotation.beanName();
        String bearerToken = RemoteBeanAnnotation.bearerToken();

        if (!StringUtils.hasText(bearerToken)) {
            String envToken = System.getenv(ENV_BEARER_TOKEN);
            if (StringUtils.hasText(envToken)) {
                bearerToken = envToken;
            }
        }

        if (!StringUtils.hasText(beanName)) {
            beanName = StringUtils.uncapitalize(interfaceClass.getSimpleName());
        }

        logger.info("[RemoteBean] Remote class yükleniyor: {} <- {}", interfaceName, remoteUrl);

        // Uzak sınıfı yükle
        RemoteBeanLoader RemoteBeanLoader = new RemoteBeanLoader(
                Thread.currentThread().getContextClassLoader());

        Class<?> remoteImplClass;
        try {
            remoteImplClass = RemoteBeanLoader.loadRemoteBeanForInterface(
                remoteUrl, interfaceClass, StringUtils.hasText(bearerToken) ? bearerToken : null);
        } catch (IOException e) {
            logger.error("[RemoteBean] Remote class URL'den yüklenemedi: {}", remoteUrl, e);
            throw new RuntimeException("Remote class yükleme hatası: " + remoteUrl, e);
        }

        // Yüklenen sınıfın interface'i implement ettiğini doğrula
        if (!interfaceClass.isAssignableFrom(remoteImplClass)) {
            throw new IllegalStateException(
                    String.format("[RemoteBean] Remote class %s, interface %s'i implement etmiyor",
                            remoteImplClass.getName(), interfaceClass.getName()));
        }

        logger.info("[RemoteBean] Remote class başarıyla yüklendi: {} (Bean adı: {})",
                remoteImplClass.getName(), beanName);

        // Bean definition oluştur ve kaydet
        RootBeanDefinition remoteBeanDefinition = new RootBeanDefinition(remoteImplClass);
        remoteBeanDefinition.setTargetType(interfaceClass);
        remoteBeanDefinition.setRole(BeanDefinition.ROLE_APPLICATION);

        // Bean'i registry'ye kaydet
        registry.registerBeanDefinition(beanName, remoteBeanDefinition);

        logger.info("[RemoteBean] Bean başarıyla kaydedildi: {} -> {}", beanName, remoteImplClass.getName());
    }
}
