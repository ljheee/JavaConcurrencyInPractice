package com.ljheee.juc;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * BrokenBarrierException 异常示例
 */
public class BrokenBarrierExceptionDemo {

    static class MyThread extends Thread {
        private CyclicBarrier cyclicBarrier;
        private String name;
        private int ID;

        public MyThread(CyclicBarrier cyclicBarrier, String name, int ID) {
            super();
            this.cyclicBarrier = cyclicBarrier;
            this.name = name;
            this.ID = ID;

        }

        @Override
        public void run() {
            System.out.println(name + "开始准备");
            try {
                Thread.sleep(ID * 1000);  //不同运动员准备时间不一样，方便模拟不同情况
                System.out.println(name + "准备完毕！在起跑线等待发令枪");
                try {
                    try {
                        cyclicBarrier.await(ID * 1000, TimeUnit.MILLISECONDS);
                    } catch (TimeoutException e) {
                        e.printStackTrace();
                    }
                    System.out.println(name + "跑完了路程！");
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                    System.out.println(name + "看不见起跑线了");
                }
                System.out.println(name + "退场！");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

    public static void main(String[] args) throws InterruptedException {
        CyclicBarrier barrier = new CyclicBarrier(5, new Runnable() {
            @Override
            public void run() {
                System.out.println("发令枪响了，跑！");

            }
        });

        for (int i = 0; i < 5; i++) {
            new MyThread(barrier, "运动员" + i + "号", i).start();
        }
        Thread.sleep(1000);
        barrier.reset();
    }

}
