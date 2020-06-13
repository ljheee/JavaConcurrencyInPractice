package com.ljheee.proxy;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.LongStream;

/**
 * https://www.jianshu.com/p/b464487ff844
 */
public class Main {
    public static void main(String[] args) {

        Instant start = Instant.now();
        long reduce = LongStream.rangeClosed(0, 10)
                .parallel()//并行流，默认ForkJoin框架去完成并行操作
                .reduce(0, Long::sum);
        System.out.println(reduce);//55


        LongStream.rangeClosed(0, 110)
                .sequential() //顺序流
                .reduce(0, Long::sum);


        Instant end = Instant.now();
        System.out.println("耗费时间" + Duration.between(start, end).toMillis());
    }
}
