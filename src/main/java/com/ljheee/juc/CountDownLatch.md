


## CountDownLatch的两种常用场景


#### CountDownLatch的两种使用场景
先来看看 CountDownLatch 的源码注释；
```
/**
 * A synchronization aid that allows one or more threads to wait until
 * a set of operations being performed in other threads completes.
 *
 * @since 1.5
 * @author Doug Lea
 */
public class CountDownLatch {
}
```
描述如下：它是一个同步工具类，允许一个或多个线程一直等待，直到其他线程运行完成后再执行。

通过描述，可以清晰的看出，CountDownLatch的两种使用场景：
- 场景1：让多个线程等待
- 场景2：和让单个线程等待。


###### 场景1 让多个线程等待：模拟并发，让并发线程一起执行
为了模拟高并发，让一组线程在指定时刻(秒杀时间)执行抢购，这些线程在准备就绪后，进行等待(CountDownLatch.await())，直到秒杀时刻的到来，然后一拥而上；
这也是本地测试接口并发的一个简易实现。

在这个场景中，CountDownLatch充当的是一个`发令枪`的角色；
就像田径赛跑时，运动员会在起跑线做准备动作，等到发令枪一声响，运动员就会奋力奔跑。和上面的秒杀场景类似，代码实现如下：
```
CountDownLatch countDownLatch = new CountDownLatch(1);
for (int i = 0; i < 5; i++) {
    new Thread(() -> {
        try {
            //准备完毕……运动员都阻塞在这，等待号令
            countDownLatch.await();
            String parter = "【" + Thread.currentThread().getName() + "】";
            System.out.println(parter + "开始执行……");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }).start();
}

Thread.sleep(2000);// 裁判准备发令
countDownLatch.countDown();// 发令枪：执行发令
```
运行结果：
```
【Thread-0】开始执行……
【Thread-1】开始执行……
【Thread-4】开始执行……
【Thread-3】开始执行……
【Thread-2】开始执行……
```
我们通过CountDownLatch.await()，让多个参与者线程启动后阻塞等待，然后在主线程 调用CountDownLatch.countdown(1) 将计数减为0，让所有线程一起往下执行；
以此实现了多个线程在同一时刻并发执行，来模拟并发请求的目的。


###### 场景2 让单个线程等待：多个线程(任务)完成后，进行汇总合并
很多时候，我们的并发任务，存在前后依赖关系；比如数据详情页需要同时调用多个接口获取数据，并发请求获取到数据后、需要进行结果合并；或者多个数据操作完成后，需要数据check；
这其实都是：在多个线程(任务)完成后，进行汇总合并的场景。

代码实现如下：
```
CountDownLatch countDownLatch = new CountDownLatch(5);
for (int i = 0; i < 5; i++) {
    final int index = i;
    new Thread(() -> {
        try {
            Thread.sleep(1000 + ThreadLocalRandom.current().nextInt(1000));
            System.out.println("finish" + index + Thread.currentThread().getName());
            countDownLatch.countDown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }).start();
}

countDownLatch.await();// 主线程在阻塞，当计数器==0，就唤醒主线程往下执行。
System.out.println("主线程:在所有任务运行完成后，进行结果汇总");
```
运行结果：
```
finish4Thread-4
finish1Thread-1
finish2Thread-2
finish3Thread-3
finish0Thread-0
主线程:在所有任务运行完成后，进行结果汇总
```
在每个线程(任务) 完成的最后一行加上CountDownLatch.countDown()，让计数器-1；
当所有线程完成-1，计数器减到0后，主线程往下执行汇总任务。


从上面两个例子的代码，可以看出 CountDownLatch 的API并不多；
- CountDownLatch的构造函数中的count就是闭锁需要等待的线程数量。这个值只能被设置一次，而且不能重新设置；
- await()：调用该方法的线程会被阻塞，直到构造方法传入的 N 减到 0 的时候，才能继续往下执行；
- countDown()：使 CountDownLatch 计数值 减 1；


#### CountDownLatch 工作原理
CountDownLatch是通过一个计数器来实现的，计数器的初始值为线程的数量；
调用await()方法的线程会被阻塞，直到计数器 减到 0 的时候，才能继续往下执行；
countDown()方法则是将计数器减1；

在CountDownLatch的构造函数中，指定的线程数量，只能指定一次；由于CountDownLatch采用的是减计数，因此当计数减为0时，计数器不能被重置。
这是和CyclicBarrier的一个重要区别。

CountDownLatch 的源码在JUC并发工具中，也相对算是简单的；
底层基于 AbstractQueuedSynchronizer 实现，CountDownLatch 构造函数中指定的count直接赋给AQS的state；每次countDown()则都是release(1)减1，最后减到0时unpark阻塞线程；这一步是由最后一个执行countdown方法的线程执行的。
而调用await()方法时，当前线程就会判断state属性是否为0，如果为0，则继续往下执行，如果不为0，则使当前线程进入等待状态，直到某个线程将state属性置为0，其就会唤醒在await()方法中等待的线程。


#### CountDownLatch与Thread.join
CountDownLatch的作用就是允许一个或多个线程等待其他线程完成操作，看起来有点类似join() 方法，但其提供了比 join() 更加灵活的API。
CountDownLatch可以手动控制在n个线程里调用n次countDown()方法使计数器进行减一操作，也可以在一个线程里调用n次执行减一操作。
而 join() 的实现原理是不停检查join线程是否存活，如果 join 线程存活则让当前线程永远等待。所以两者之间相对来说还是CountDownLatch使用起来较为灵活。


#### CountDownLatch与CyclicBarrier
CountDownLatch和CyclicBarrier都能够实现线程之间的等待，只不过它们侧重点不同：
- CountDownLatch一般用于一个或多个线程，等待其他线程执行完任务后，再才执行
- CyclicBarrier一般用于一组线程互相等待至某个状态，然后这一组线程再同时执行
另外，CountDownLatch是减计数，计数减为0后不能重用；而CyclicBarrier是加计数，可置0后复用。

更多CyclicBarrier的内容，请见下篇。


源码
https://www.jianshu.com/p/128476015902