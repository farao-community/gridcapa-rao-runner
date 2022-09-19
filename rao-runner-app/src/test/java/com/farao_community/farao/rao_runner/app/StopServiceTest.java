/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class StopServiceTest {

    @Autowired
    StopService stopService;

    private class MyThread extends Thread {

        public MyThread(String id) {
            super(id);
        }

        @Override
        public void run() {
            int count = 0;
            for (int i = 0; i < 10; i++) {
                count += i;
                await().atMost(i, SECONDS);
            }
        }
    }

    @Test
    void threadInterruption() {
        MyThread th = new MyThread("myThread");
        assertEquals(false, stopService.isRunning("myThread").isPresent());

        th.start();
        assertEquals(true, stopService.isRunning("myThread").isPresent());

        stopService.stop("myThread");
        assertEquals(false, stopService.isRunning("myThread").isPresent());

    }

}


