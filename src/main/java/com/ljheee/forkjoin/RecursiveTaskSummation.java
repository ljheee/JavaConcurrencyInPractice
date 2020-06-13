package com.ljheee.forkjoin;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;

/**
 * RecursiveTask 有返回值的任务，如累计求和
 * RecursiveTask<Integer>  指定返回值
 * <p>
 * 大任务是：计算随机的1000个数字的和。
 */
public class RecursiveTaskSummation extends RecursiveTask<Integer> {


    private static final int MAX = 70;
    private int arr[];
    private int start;
    private int end;


    public RecursiveTaskSummation(int[] arr, int start, int end) {
        this.arr = arr;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Integer compute() {
        int sum = 0;
        // 当end-start的值小于MAX时候，开始执行 就不再分解了
        if ((end - start) < MAX) {
            for (int i = start; i < end; i++) {
                sum += arr[i];
            }
            return sum;
        } else {
            // 将大任务分解成两个小任务
            int middle = (start + end) / 2;
            RecursiveTaskSummation left = new RecursiveTaskSummation(arr, start, middle);
            RecursiveTaskSummation right = new RecursiveTaskSummation(arr, middle, end);
//            // 并行执行两个小任务
//            left.fork();
//            right.fork();
//            // 把两个小任务累加的结果合并起来
//            return left.join() + right.join();

            left.fork();
            return left.join() + right.compute();//只fork一边,和上面3行代码等效
        }
    }


    public static void main(String[] args) throws ExecutionException, InterruptedException {
        int arr[] = new int[1000];
        Random random = new Random();
        int total = 0;
        // 初始化100个数字元素
        for (int i = 0; i < arr.length; i++) {
            int temp = random.nextInt(100);
            // 对数组元素赋值,并将数组元素的值添加到total总和中
            total += (arr[i] = temp);
        }
        System.out.println("初始化时的总和=" + total);


        ForkJoinPool forkJoinPool = new ForkJoinPool();

        // 提交可分解的PrintTask任务
        Future<Integer> future = forkJoinPool.submit(new RecursiveTaskSummation(arr, 0, arr.length));//submit将 ForkJoinTask 类的对象提交给 ForkJoinPool，ForkJoinPool 将立刻开始执行 ForkJoinTask。
        System.out.println("计算出来的总和1=" + future.get());

        Integer integer = forkJoinPool.invoke(new RecursiveTaskSummation(arr, 0, arr.length));

        System.out.println("计算出来的总和=" + integer);

        // 关闭线程池
        forkJoinPool.shutdown();
    }


}
