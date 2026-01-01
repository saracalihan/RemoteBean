package com.saracalihan.RemoteComponent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Uzak URL'lerden Java class dosyalarını yüklemek için özel ClassLoader.
 * Bu sınıf verilen URL'den .class dosyasını byte array olarak okur ve
 * JVM'e yükler.
 * 
 * @author saracalihan
 */
public class RemoteBeanLoader extends ClassLoader {

    public RemoteBeanLoader(ClassLoader parent) {
        super(parent);
    }

    /**
     * Verilen URL'den class dosyasını byte array olarak yükler.
     * 
     * @param url Class dosyasının URL adresi
     * @return Class dosyasının byte array içeriği
     * @throws IOException URL'den okuma hatası durumunda
     */
    public byte[] loadClassData(String url) throws IOException {
        return loadClassData(url, null);
    }

    /**
     * Verilen URL'den class dosyasını byte array olarak yükler.
     * Bearer token belirtilirse Authorization header'ına eklenir.
     * 
     * @param url         Class dosyasının URL adresi
     * @param bearerToken Authorization için Bearer token (opsiyonel)
     * @return Class dosyasının byte array içeriği
     * @throws IOException URL'den okuma hatası durumunda
     */
    public byte[] loadClassData(String url, String bearerToken) throws IOException {
        URL classUrl = new URL(url);
        URLConnection connection = classUrl.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        if (bearerToken != null && !bearerToken.trim().isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
        }

        try (InputStream inputStream = connection.getInputStream();
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteStream.write(buffer, 0, bytesRead);
            }

            return byteStream.toByteArray();
        }
    }

    /**
     * Uzak URL'den class dosyasını yükler ve Class nesnesine dönüştürür.
     * 
     * @param className Yüklenecek sınıfın tam adı (package dahil)
     * @param url       Class dosyasının URL adresi
     * @return Yüklenen Class nesnesi
     * @throws IOException      URL'den okuma hatası durumunda
     * @throws ClassFormatError Class dosyası geçersiz format durumunda
     */
    public Class<?> loadRemoteBean(String className, String url) throws IOException {
        byte[] classData = loadClassData(url);
        return defineClass(className, classData, 0, classData.length);
    }

    /**
     * URL'den class dosyasını yükler.
     * Class adını JVM'in class dosyasından otomatik olarak belirlemesini sağlar.
     * 
     * @param url Class dosyasının URL adresi
     * @return Yüklenen Class nesnesi
     * @throws IOException URL'den okuma hatası durumunda
     */
    public Class<?> loadRemoteBean(String url) throws IOException {
        byte[] classData = loadClassData(url);
        return defineClass(null, classData, 0, classData.length);
    }

    /**
     * Belirtilen interface ile aynı package'ta bulunan implementation class'ını
     * yükler.
     * 
     * @param url            Class dosyasının URL adresi
     * @param interfaceClass Implementation'ın uyması gereken interface
     * @return Yüklenen Class nesnesi
     * @throws IOException URL'den okuma hatası durumunda
     */
    public Class<?> loadRemoteBeanForInterface(String url, Class<?> interfaceClass) throws IOException {
        return loadRemoteBeanForInterface(url, interfaceClass, null);
    }

    /**
     * Belirtilen interface ile aynı package'ta bulunan implementation class'ını
     * yükler.
     * Bearer token belirtilirse HTTP isteğinde Authorization header'ına eklenir.
     * 
     * @param url            Class dosyasının URL adresi
     * @param interfaceClass Implementation'ın uyması gereken interface
     * @param bearerToken    Authorization için Bearer token (opsiyonel)
     * @return Yüklenen Class nesnesi
     * @throws IOException URL'den okuma hatası durumunda
     */
    public Class<?> loadRemoteBeanForInterface(String url, Class<?> interfaceClass, String bearerToken)
            throws IOException {
        byte[] classData = loadClassData(url, bearerToken);

        // TODO: class ismini disaridan al
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        String simpleClassName = fileName.replace(".class", "");

        String packageName = interfaceClass.getPackage().getName();
        String fullClassName = packageName + "." + simpleClassName;

        return defineClass(fullClassName, classData, 0, classData.length);
    }
}
