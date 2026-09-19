package com.newtech.note;

import com.newtech.note.client.BailianClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class BailianTest {

    //@Test
    public void testCallWorkflow() throws InterruptedException {
        BailianClient.getInstance()
                .callAppFiltered(
                        "2d02940ad45c49499ab732df94b249c2",
                        Map.of("url", "http://aifunc.top/upload-files/216f7fc2-fcee-4f03-bb5a-865c63f4807d.jpg"))
                .<String>handle((data, sink) -> {
                    System.out.println("data: " + data);
                    sink.next(data);
                }).subscribe(System.out::println);
        TimeUnit.SECONDS.sleep(10000);
    }


}
