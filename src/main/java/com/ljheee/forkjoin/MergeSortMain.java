package com.ljheee.forkjoin;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * Created by lijianhua04 on 2018/8/13.
 */
public class MergeSortMain {


    static Random random = new Random();

    /**
     * 生成 (待排序的)随机数组
     * @return
     */
    private static int[] builtArray() {
        int[] arr = new int[20000];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = random.nextInt(99999);//99999以内的随机数
        }
        return arr;

    }



    public static void main(String[] args) throws InterruptedException {
        int arr[] = builtArray();

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        MergeSortAction mergeSortAction = new MergeSortAction(100, arr);

        ForkJoinPool forkJoinPool = new ForkJoinPool(availableProcessors);
        forkJoinPool.invoke(mergeSortAction);

//        for (int i = 0; i < arr.length; i++) {
//            System.out.println(arr[i]);
//        }

        int[] sortedArray = mergeSortAction.getSortedArray();
        for (int i = 0; i < sortedArray.length; i++) {
            System.out.println(sortedArray[i]);
        }
        System.out.println("sortedArray.length="+sortedArray.length);
        forkJoinPool.shutdown();

    }
}
