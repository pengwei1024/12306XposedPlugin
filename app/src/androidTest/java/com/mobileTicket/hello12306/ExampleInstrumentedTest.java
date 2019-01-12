package com.mobileTicket.hello12306;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.mobileTicket.hello12306.model.SecKill;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        final CountDownLatch countDownLatch = new CountDownLatch(2);
        assertEquals("com.mobileTicket.hello12306", appContext.getPackageName());
        SecKill.getInstance().addTask("13点39分起售", new Runnable() {
            @Override
            public void run() {
                System.out.println("query!!!");
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
