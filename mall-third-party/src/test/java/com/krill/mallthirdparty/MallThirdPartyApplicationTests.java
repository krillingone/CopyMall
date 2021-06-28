package com.krill.mallthirdparty;

import com.aliyun.oss.OSSClient;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@SpringBootTest
class MallThirdPartyApplicationTests {

    @Autowired
    OSSClient ossClient;

    @Test
    public void testOSS() throws FileNotFoundException {

        InputStream inputStream = new FileInputStream("C:\\Users\\krill\\Desktop\\nao.jpg");

        ossClient.putObject("copymall-krill","nao3.jpg", inputStream);

        // 关闭OSSClient。
        ossClient.shutdown();

        System.out.println("OK");
    }

    @Test
    void contextLoads() {
    }

}
