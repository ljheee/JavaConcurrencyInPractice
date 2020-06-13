package com.ljheee.forkjoin;

import java.util.Arrays;
import java.util.concurrent.RecursiveAction;

/**
 * RecursiveAction 无返回值的任务，如排序
 */
public class MergeSortAction extends RecursiveAction {
    // 阈值
    private int threshold;

    // 待排 数组 (执行完成后，就是有序数组了)
    private int[] arrayToSort;

    public MergeSortAction(int threshold, int[] arrayToSort) {
        this.threshold = threshold;
        this.arrayToSort = arrayToSort;
    }

    public int[] getSortedArray() {
        return arrayToSort;
    }

    @Override
    protected void compute() {
        if (arrayToSort.length <= threshold) {
            Arrays.sort(arrayToSort);
            return;
        }

        // 任务分解
        int midPoint = arrayToSort.length / 2;
        int[] leftArray = Arrays.copyOfRange(arrayToSort, 0, midPoint);
        int[] rightArray = Arrays.copyOfRange(arrayToSort, midPoint, arrayToSort.length);

        MergeSortAction left = new MergeSortAction(threshold, leftArray);
        MergeSortAction right = new MergeSortAction(threshold, rightArray);

        left.fork();
        right.fork();

        left.join();
        right.join();
//        arrayToSort = merge(leftArray, rightArray);// 错误
        arrayToSort = merge(left.getSortedArray(), right.getSortedArray());
    }

    /**
     * 归并排序
     * 将2个有序数组  归并排序
     *
     * @param leftArray
     * @param rightArray
     * @return
     */
    public int[] merge(int[] leftArray, int[] rightArray) {

        int[] mergedArray = new int[leftArray.length + rightArray.length];
        int mergedArrayPos = 0;
        int leftArrayPos = 0;
        int rightArrayPos = 0;

        while (leftArrayPos < leftArray.length && rightArrayPos < rightArray.length) {

            if (leftArray[leftArrayPos] <= rightArray[rightArrayPos]) {
                mergedArray[mergedArrayPos] = leftArray[leftArrayPos];
                leftArrayPos++;
            } else {
                mergedArray[mergedArrayPos] = rightArray[rightArrayPos];
                rightArrayPos++;
            }
            mergedArrayPos++;
        }

        while (leftArrayPos < leftArray.length) {
            mergedArray[mergedArrayPos] = leftArray[leftArrayPos];
            leftArrayPos++;
            mergedArrayPos++;
        }

        while (rightArrayPos < rightArray.length) {
            mergedArray[mergedArrayPos] = rightArray[rightArrayPos];
            rightArrayPos++;
            mergedArrayPos++;
        }
        return mergedArray;
    }


    // 测试归并排序
    public static void main(String[] args) {
        int[] a1 = new int[]{1, 3, 4};
        int[] a2 = new int[]{2, 5, 6};

        MergeSortAction mergeSortAction = new MergeSortAction(5, a1);
        int[] merge = mergeSortAction.merge(a1, a2);

        for (int i = 0; i < merge.length; i++) {
            System.out.println(merge[i]);
        }
        System.out.println("len=" + merge.length);
        System.out.println(merge[0]);
    }


}
