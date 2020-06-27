
# CyclicBarrier




## CyclicBarrier多任务协同的利器

疫情逐渐好转，部门也有半年多没有TB团建了，并且金三银四，部门又招了一波新人；
leader让你组织一次TB：周六上午，大家先到公司集合，然后一起去朝阳公园玩，最后一起去餐厅聚餐，然后回家。
为了体现团队集体意识，在每次开启新项目时，需要所有人一起开始行动（不能早来的人都把东西吃光了吧~），并且每个阶段活动完成后，需要统计人数、向上汇报。

这个场景，如何借助JUC并发工具来实现呢？
我们先来梳理一下，任务特点：
- 很显然，`每次开启新项目时，需要所有人一起开始行动` 这是个多任务相互等待，直到所有人都到达一个点时，才开始执行；
- 同时，TB活动是分为多个阶段的，每个阶段都有具体要做的事；
- 每个阶段完后，组织者还得做点而外的事；
- 参与者的数量，是确定的；

看到多任务相互等待，相信很多人已经想到了 CyclicBarrier。

没错，这个TB任务的特点，其实也是使用 CyclicBarrier 时的特点。
下面来看 如何使用 CyclicBarrier 实现TB成员管理的。

先来看看 CyclicBarrier 的源码注释；
> A synchronizati on aid that allows a set of threads to all wait for each other to reach a common barrier point.

描述如下：多个线程相互等待，直到所有线程到达同一个同步点，再继续一起执行。

CyclicBarrier适用于多个线程有固定的多步需要执行，线程间互相等待，当都执行完了，在一起执行下一步。

CyclicBarrier 字面意思回环栅栏，通过它可以实现让一组线程等待至某个状态之后再全部同时执行。
- 叫做回环，是因为当所有等待线程都被释放以后，CyclicBarrier可以被重用。
- 叫做栅栏，大概是描述所有线程被栅栏挡住了，当都达到时，一起跳过栅栏执行，也算形象。我们可以把这个状态就叫做barrier。

CyclicBarrier 的API
```
public CyclicBarrier(int parties)
public int await()
```
构造函数，指定参与者数量；
await()让线程阻塞在栅栏。

CyclicBarrier实现TB成员协同
我们用 parties 变量指定了参与者数量；用sleep(随机数)来模拟每个TB活动不同成员的耗时；代码实现如下：
```java
    static final Random random = new Random();
    static final CyclicBarrier cyclicBarrier = new CyclicBarrier(parties);


    static class StaffThread extends Thread {
        @Override
        public void run() {
            try {
                String staff = "员工【" + Thread.currentThread().getName() + "】";

                // 第一阶段：来公司集合
                System.out.println(staff + "从家出发了……");
                Thread.sleep(random.nextInt(5000));
                System.out.println(staff + "到达公司");

                // 协同，第一次等大家到齐
                cyclicBarrier.await();

                // 第二阶段：出发去公园
                System.out.println(staff + "出发去公园玩");
                Thread.sleep(random.nextInt(5000));
                System.out.println(staff + "到达公园门口集合");

                // 协同：第二次等大家到齐
                cyclicBarrier.await();

                // 第三阶段：去餐厅
                System.out.println(staff + "出发去餐厅");
                Thread.sleep(random.nextInt(5000));
                System.out.println(staff + "到达餐厅");

                // 协同：第三次等大家到齐
                cyclicBarrier.await();

                // 第四阶段：就餐
                System.out.println(staff + "开始用餐");
                Thread.sleep(random.nextInt(5000));
                System.out.println(staff + "用餐结束，回家");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {

        // 所有人，开始行动
        for (int i = 0; i < parties; i++) {
            new StaffThread().start();
        }

    }
```
我们用 StaffThread 代表每个员工参加TB活动要做的事；每个成员在各个阶段，花费的时间可能不同；
每个员工，在执行完成当前阶段后 cyclicBarrier.await()进行阻塞（任务协同），等待大家到齐了再进入下一阶段。

值得一提的是，CyclicBarrier 的计数器有自动重置的功能，当减到 0 的时候，会自动重置你设置的初始值，自动复原。这个功能用起来实在是太方便了。

由于 CyclicBarrier 的可重用特性，当所有等待线程都被释放以后，CyclicBarrier可以被重用；
因此只要每个阶段，所有成员都完成后，CyclicBarrier就会自动重用，以此往复。

看上去，很完美。就差一点了 —— TB组织者，要在每个阶段结束后向上汇报；
这就用到了 CyclicBarrier 的回调函数功能，CyclicBarrier 的第二个构造方法：
```
  CyclicBarrier(int parties, Runnable barrierAction);
```
barrierAction 可以指定一个善后处理的task，在所有人都到达屏障点时，来执行；
就好比团建时，所有人都到达公园门口了，这时组织者喊“都别走，先拍个照”，然后横幅一拉……
（呜呜呜……）

下面来看 如何实现；
由于构造函数中，只能指定一个Runnable善后任务，但我们的TB活动有多个阶段，每个阶段都需要汇报一次，因此我们实现的Runnable任务，需要判断在不同的阶段，做不同的汇报；

我们用 peroid 变量代表当前阶段，初始值为1；
CyclicBarrier的可复用功能，在所有人都达到集合点后，执行一次 milestoneRunnable 善后任务，意味着 milestoneRunnable 执行一次后，就代表进入下一阶段，因此peroid++;
```java
    //阶段
    static int peroid = 1;

    /**
     * 里程碑
     * 每阶段完成后，会执行这里；
     */
    static Runnable milestoneRunnable = new Runnable() {
        @Override
        public void run() {
            switch (peroid) {
                case 1:
                    System.out.println("********第1阶段***************");
                    break;
                case 2:
                    System.out.println("********第2阶段***************");
                    break;
                case 3:
                    System.out.println("********第3阶段***************");
                    break;
            }
            peroid++;
        }
    };
```

我们换用带回调函数的构造方法，再执行
```java
static final CyclicBarrier cyclicBarrier = new CyclicBarrier(parties, milestoneRunnable);

public static void main(String[] args) {
    // 所有人，开始行动
    for (int i = 0; i < parties; i++) {
        new StaffThread().start();
    }

}
```
运行结果：
```
员工【Thread-0】从家出发了……
员工【Thread-2】从家出发了……
员工【Thread-1】从家出发了……
员工【Thread-2】到达公司
员工【Thread-0】到达公司
员工【Thread-1】到达公司
********第1阶段***************
员工【Thread-1】出发去公园玩
员工【Thread-2】出发去公园玩
员工【Thread-0】出发去公园玩
员工【Thread-2】到达公园门口集合
员工【Thread-1】到达公园门口集合
员工【Thread-0】到达公园门口集合
********第2阶段***************
员工【Thread-0】出发去餐厅
员工【Thread-2】出发去餐厅
员工【Thread-1】出发去餐厅
员工【Thread-0】到达餐厅
员工【Thread-1】到达餐厅
员工【Thread-2】到达餐厅
********第3阶段***************
员工【Thread-2】开始用餐
员工【Thread-0】开始用餐
员工【Thread-1】开始用餐
员工【Thread-2】用餐结束，回家
员工【Thread-0】用餐结束，回家
员工【Thread-1】用餐结束，回家
```
通过这个例子，对 CyclicBarrier 的基本使用，是不是清晰很多。

#### 关于CyclicBarrier的回调函数
CyclicBarrier 的回调函数，可以指定一个线程池来运行，相当于异步完成；
如果不指定线程池，默认在最后一个执行await()的线程执行，相当于同步完成。

这有什么区别呢？
实则关注性能的场景，区别很大。
https://mp.weixin.qq.com/s/IqBbH8JA707Fb7Vsb0ZKPA
文中的例子，对账系统每天会校验是否存在异常订单，简易实现如下:
```
while(存在未对账订单){
  // 查询未对账订单
  pos = getPOrders();
  // 查询派送单
  dos = getDOrders();
  // 执行对账操作
  diff = check(pos, dos);
  // 差异写入差异库
  save(diff);
}
```
> 文中有使用CyclicBarrier的实现：查询订单和查询派单 两个线程相互等待，都完成时执行回调:进行check对账

对账任务可以大致分为两步，第一步查询需要核对的订单、账单，第二步执行check()、记录差异。
两个查询操作可以并发完成，`第二步执行check()、记录差异`可以看作是第一阶段任务完成后的“结果汇总”，可以使用CyclicBarrier的回调函数来完成；
最终达到的效果图：

也就是并发去查询，查询的结果让 回调函数异步执行，好处是查询线程直接进入下一阶段、继续查询下一组数据；
中间使用一个同步容器，保存订单数据即可。详细思路&代码，见：

试想，假设让CyclicBarrier的回调函数执行在一个回合里最后执行await()的线程上，而且同步调用回调函数check()，调用完check之后，才会开始第二回合。
所以check如果不另开一线程异步执行，就起不到性能优化的作用了。


CyclicBarrier 的回调函数究竟是哪个线程执行的呢？
如果你分析源码，你会发现执行回调函数的线程是将 CyclicBarrier 内部计数器减到 0 的那个线程。
这里强调一下：当看到回调函数的时候，一定问一问执行回调函数的线程是谁。
如果CyclicBarrier回调函数不使用隔离的线程池，则CyclicBarrier最后一个线程忙着执行回调，其他线程还在阻塞。


#### 问题思考：需求升级
需求升级了：实际TB活动中，可能有人白天有事，不能参加公园的活动、但晚上会来聚餐；有人白天能参加，晚上不能参加；
并且公园的门票，聚餐费用，因参与人数不同，又有不同。
思考：需求升级后，如何实现？CyclicBarrier 能完成吗？






## CyclicBarrier的克星——BrokenBarrierException
上篇 我们借助部门TB的例子，一步步分析了 CyclicBarrier 多线程协调的功能。
CyclicBarrier 功能强大的同时，意味着提供了更多的API，并且在使用过程中，可能有一些注意点。

今天就来聊聊 BrokenBarrierException，从名字就能看出，是“屏障被破坏异常”，屏障被破坏时，CyclicBarrier 的期望功能就不能完成，甚至导致程序异常；
BrokenBarrierException 可谓是 CyclicBarrier 的克星。

上篇的例子，我们仅仅使用了 CyclicBarrier 最基本的API
```
public CyclicBarrier(int parties);
CyclicBarrier(int parties, Runnable barrierAction);
public int await();
```
实际还有：
```
int getParties():获取CyclicBarrier打开屏障的线程数量，也成为方数
int getNumberWaiting():获取正在CyclicBarrier上等待的线程数量
int await(timeout,TimeUnit):带限时的阻塞等待
boolean isBroken():获取是否破损标志位broken的值
void reset():使得CyclicBarrier回归初始状态
```

我们重点介绍一下，能够导致 BrokenBarrierException 的操作，然后给出详细示例：

首先是 await() 和 await(timeout,TimeUnit)带时限的阻塞等待
```
    /**
     * Waits until all {@linkplain #getParties parties} have invoked
     * {@code await} on this barrier.
     *
     * <p>If the current thread is not the last to arrive then it is
     * disabled for thread scheduling purposes and lies dormant until
     * one of the following things happens:
     * <ul>
     * <li>The last thread arrives; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * one of the other waiting threads; or
     * <li>Some other thread times out while waiting for barrier; or
     * <li>Some other thread invokes {@link #reset} on this barrier.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the barrier is {@link #reset} while any thread is waiting,
     * or if the barrier {@linkplain #isBroken is broken} when
     * {@code await} is invoked, or while any thread is waiting, then
     * {@link BrokenBarrierException} is thrown.
     *
     * <p>If any thread is {@linkplain Thread#interrupt interrupted} while waiting,
     * then all other waiting threads will throw
     * {@link BrokenBarrierException} and the barrier is placed in the broken
     * state.
     *
     * <p>If the current thread is the last thread to arrive, and a
     * non-null barrier action was supplied in the constructor, then the
     * current thread runs the action before allowing the other threads to
     * continue.
     * If an exception occurs during the barrier action then that exception
     * will be propagated in the current thread and the barrier is placed in
     * the broken state.
     *
     * @return the arrival index of the current thread, where index
     *         {@code getParties() - 1} indicates the first
     *         to arrive and zero indicates the last to arrive
     * @throws InterruptedException if the current thread was interrupted
     *         while waiting
     * @throws BrokenBarrierException if <em>another</em> thread was
     *         interrupted or timed out while the current thread was
     *         waiting, or the barrier was reset, or the barrier was
     *         broken when {@code await} was called, or the barrier
     *         action (if present) failed due to an exception
     */
    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            return dowait(false, 0L);
        } catch (TimeoutException toe) {
            throw new Error(toe); // cannot happen
        }
    }
```
await() 源码注释，描述了方法功能：调用该方法的线程进入等待，在CyclicBarrier上进行阻塞等待，直到发生以下情形之一：
- 在CyclicBarrier上等待的线程数量达到parties，则所有线程被释放，继续执行。—— 正常情形
- 当前线程被中断，则抛出InterruptedException异常，并停止等待，继续执行。
- 其他等待的线程被中断，则当前线程抛出BrokenBarrierException异常，并停止等待，继续执行。
- 其他等待的线程超时，则当前线程抛出BrokenBarrierException异常，并停止等待，继续执行。
- 其他线程调用CyclicBarrier.reset()方法，则当前线程抛出BrokenBarrierException异常，并停止等待，继续执行。

除了第一种属于正常的情形，其他的都会导致 BrokenBarrierException。
带时限的await() 会抛出 TimeoutException；
```
public int await(long timeout, TimeUnit unit) throws InterruptedException,
                                                             BrokenBarrierException,
                                                             TimeoutException
```
当前线程等待超时，则抛出TimeoutException异常，并停止等待，继续执行。
当前线程抛出 TimeoutException 异常时，其他线程会抛出 BrokenBarrierException 异常。


await() 和 await(timeout,TimeUnit)带时限的阻塞等待，总共会有4种情形，产生 BrokenBarrierException；
下面我们一一来看。


#### Barrier被破坏的4中情形
为了加深大家对 CyclicBarrier 使用场景的熟悉，我们在复现产生 BrokenBarrierException 的4种情形时，使用运动员比赛的例子：

**1.如果有线程已经处于等待状态，调用reset方法会导致已经在等待的线程出现BrokenBarrierException异常。并且由于出现了BrokenBarrierException，将会导致始终无法等待。**

比如，五个运动员，其中一个在等待发令枪的过程中错误地接收到裁判传过来的指令，导致这个运动员以为今天比赛取消就离开了赛场。但是其他运动员都领会的裁判正确的指令，剩余的运动员在起跑线上无限地等待下去，并且裁判看到运动员没有到齐，也不会打发令枪。
```
class MyThread extends Thread {
    private CyclicBarrier cyclicBarrier;
    private String name;
    private int ID;

    public MyThread(CyclicBarrier cyclicBarrier, String name,int ID) {
        super();
        this.cyclicBarrier = cyclicBarrier;
        this.name = name;
        this.ID=ID;

    }
    @Override
    public void run() {
        System.out.println(name + "开始准备");
        try {
            Thread.sleep(ID*1000);  //不同运动员准备时间不一样，方便模拟不同情况
            System.out.println(name + "准备完毕！在起跑线等待发令枪");
            try {
                cyclicBarrier.await();
                System.out.println(name + "跑完了路程！");
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
                System.out.println(name+"看不见起跑线了");
            }
            System.out.println(name+"退场！");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
public class Test {

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
```
当发生 BrokenBarrierException 时，CyclicBarrier的保障被破坏，不能完成原功能；对应比赛场景，相当于运动员退场了。

运行结果：
```
运动员0号开始准备
运动员2号开始准备
运动员3号开始准备
运动员0号准备完毕！在起跑线等待发令枪
运动员1号开始准备
运动员4号开始准备
运动员1号准备完毕！在起跑线等待发令枪
运动员0号看不见起跑线了
运动员0号退场！
java.util.concurrent.BrokenBarrierException
	at java.util.concurrent.CyclicBarrier.dowait(CyclicBarrier.java:250)
	at java.util.concurrent.CyclicBarrier.await(CyclicBarrier.java:362)
	at com.ljheee.juc.BrokenBarrierExceptionDemo$MyThread.run(BrokenBarrierExceptionDemo.java:31)
运动员2号准备完毕！在起跑线等待发令枪
运动员3号准备完毕！在起跑线等待发令枪
运动员4号准备完毕！在起跑线等待发令枪
```
从输出可以看到，运动员0号在等待的过程中，主线程调用了reset方法，导致抛出BrokenBarrierException异常。但是其他线程并没有受到影响，它们会一直等待下去，从而一直被阻塞。
此时程序一直没停。

这种场景下，因为有参与者提前离开，导致剩余参与者永久等待。

**2.如果在等待的过程中，线程被中断，也会抛出BrokenBarrierException异常，并且这个异常会传播到其他所有的线程。**
```java
public class Test {
    static   Map<Integer,Thread>   threads=new HashMap<>();
    public static void main(String[] args) throws InterruptedException {
        CyclicBarrier barrier = new CyclicBarrier(5, new Runnable() {
            @Override
            public void run() {
                System.out.println("发令枪响了，跑！");

            }
        });

        for (int i = 0; i < 5; i++) {
        MyThread t = new MyThread(barrier, "运动员" + i + "号", i);
            threads.put(i, t);
            t.start();
        }
        Thread.sleep(3000);
        threads.get(1).interrupt();
    }
}
```
运行结果：
```
运动员0号开始准备
运动员2号开始准备
运动员3号开始准备
运动员1号开始准备
运动员0号准备完毕！在起跑线等待发令枪
运动员4号开始准备
运动员1号准备完毕！在起跑线等待发令枪
运动员2号准备完毕！在起跑线等待发令枪
运动员3号准备完毕！在起跑线等待发令枪
java.lang.InterruptedException
运动员3号看不见起跑线了
运动员3号退场！
运动员2号看不见起跑线了
运动员2号退场！
运动员0号看不见起跑线了
运动员0号退场！
    at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.reportInterruptAfterWait(AbstractQueuedSynchronizer.java:2014)
    at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2048)
    at java.util.concurrent.CyclicBarrier.dowait(CyclicBarrier.java:234)
    at java.util.concurrent.CyclicBarrier.await(CyclicBarrier.java:362)
    at thread.MyThread.run(MyThread.java:27)
java.util.concurrent.BrokenBarrierException
    at java.util.concurrent.CyclicBarrier.dowait(CyclicBarrier.java:250)
    at java.util.concurrent.CyclicBarrier.await(CyclicBarrier.java:362)
    at thread.MyThread.run(MyThread.java:27)
java.util.concurrent.BrokenBarrierException
    at java.util.concurrent.CyclicBarrier.dowait(CyclicBarrier.java:250)
    at java.util.concurrent.CyclicBarrier.await(CyclicBarrier.java:362)
    at thread.MyThread.run(MyThread.java:27)
java.util.concurrent.BrokenBarrierException
    at java.util.concurrent.CyclicBarrier.dowait(CyclicBarrier.java:250)
    at java.util.concurrent.CyclicBarrier.await(CyclicBarrier.java:362)
    at thread.MyThread.run(MyThread.java:27)
运动员4号准备完毕！在起跑线等待发令枪
java.util.concurrent.BrokenBarrierException
    at java.util.concurrent.CyclicBarrier.dowait(CyclicBarrier.java:207)
    at java.util.concurrent.CyclicBarrier.await(CyclicBarrier.java:362)
    at thread.MyThread.run(MyThread.java:27)
运动员4号看不见起跑线了
运动员4号退场！
```
从输出可以看到，其中一个线程被中断，那么所有的运动员都退场了。
在实际使用CyclicBarrier，一定要防止这种情况发生。

**3.如果在执行屏障操作过程中发生异常，则该异常将传播到当前线程中，其他线程会抛出BrokenBarrierException，屏障被损坏。**
这个就好比运动员都没有问题，而是裁判出问题了。裁判权力比较大，直接告诉所有的运动员，今天不比赛了，你们都回家吧！
```java
public class Test {
    static Map<Integer, Thread> threads = new HashMap<>();

    public static void main(String[] args) throws InterruptedException {
        CyclicBarrier barrier = new CyclicBarrier(5, new Runnable() {
            @Override
            public void run() {
                String str = null;
                str.substring(0, 1);// 模拟异常
                System.out.println("发令枪响了，跑！");

            }
        });

        for (int i = 0; i < 5; i++) {
            MyThread t = new MyThread(barrier, "运动员" + i + "号", i);
            threads.put(i, t);
            t.start();
        }

    }
}
```
运行结果：
```
运动员0号开始准备
运动员3号开始准备
运动员2号开始准备
运动员1号开始准备
运动员4号开始准备
运动员0号准备完毕！在起跑线等待发令枪
运动员1号准备完毕！在起跑线等待发令枪
运动员2号准备完毕！在起跑线等待发令枪
运动员3号准备完毕！在起跑线等待发令枪
运动员4号准备完毕！在起跑线等待发令枪
Exception in thread "Thread-4" java.util.concurrent.BrokenBarrierException
    at java.util.concurrent.CyclicBarrier.dowait(CyclicBarrier.java:250)
    at java.util.concurrent.CyclicBarrier.await(CyclicBarrier.java:362)
    at thread.MyThread.run(MyThread.java:27)
运动员0号看不见起跑线了
运动员0号退场！
java.util.concurrent.BrokenBarrierException
    at java.util.concurrent.CyclicBarrier.dowait(CyclicBarrier.java:250)
    at java.util.concurrent.CyclicBarrier.await(CyclicBarrier.java:362)
    at thread.MyThread.run(MyThread.java:27)
运动员3号看不见起跑线了
运动员3号退场！
java.util.concurrent.BrokenBarrierException
    at java.util.concurrent.CyclicBarrier.dowait(CyclicBarrier.java:250)
    at java.util.concurrent.CyclicBarrier.await(CyclicBarrier.java:362)
    at thread.MyThread.run(MyThread.java:27)
运动员1号看不见起跑线了
运动员1号退场！
java.lang.NullPointerException
    at thread.Test$1.run(Test.java:15)
    at java.util.concurrent.CyclicBarrier.dowait(CyclicBarrier.java:220)
    at java.util.concurrent.CyclicBarrier.await(CyclicBarrier.java:362)
    at thread.MyThread.run(MyThread.java:27)
java.util.concurrent.BrokenBarrierException
    at java.util.concurrent.CyclicBarrier.dowait(CyclicBarrier.java:250)
    at java.util.concurrent.CyclicBarrier.await(CyclicBarrier.java:362)
    at thread.MyThread.run(MyThread.java:27)
运动员2号看不见起跑线了
运动员2号退场！
```
我们在 CyclicBarrier 的构造方法中指定回调函数，并模拟了异常；
可以看到，如果在执行屏障动作的过程中出现异常，那么所有的线程都会抛出BrokenBarrierException异常。
这也提醒我们，使用带回调的CyclicBarrier构造方法时，指定的回调任务一定不要抛出异常，或者实现异常处理。

**4.如果超出指定的等待时间，当前线程会抛出 TimeoutException 异常，其他线程会抛出BrokenBarrierException异常。**
```java
public class MyThread extends Thread {
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
            Thread.sleep(ID * 1000);
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
```
运行结果：
```
运动员0号开始准备
运动员3号开始准备
运动员2号开始准备
运动员1号开始准备
运动员0号准备完毕！在起跑线等待发令枪
运动员4号开始准备
运动员0号跑完了路程！
运动员0号退场！
java.util.concurrent.TimeoutException
	at java.util.concurrent.CyclicBarrier.dowait(CyclicBarrier.java:257)
	at java.util.concurrent.CyclicBarrier.await(CyclicBarrier.java:435)
	at com.ljheee.juc.BrokenBarrierExceptionDemo$MyThread.run(BrokenBarrierExceptionDemo.java:34)
运动员1号准备完毕！在起跑线等待发令枪
运动员2号准备完毕！在起跑线等待发令枪
运动员1号跑完了路程！
运动员1号退场！
运动员2号看不见起跑线了
运动员2号退场！
java.util.concurrent.TimeoutException
	at java.util.concurrent.CyclicBarrier.dowait(CyclicBarrier.java:257)
	at java.util.concurrent.CyclicBarrier.await(CyclicBarrier.java:435)
	at com.ljheee.juc.BrokenBarrierExceptionDemo$MyThread.run(BrokenBarrierExceptionDemo.java:34)
java.util.concurrent.BrokenBarrierException
	at java.util.concurrent.CyclicBarrier.dowait(CyclicBarrier.java:250)
	at java.util.concurrent.CyclicBarrier.await(CyclicBarrier.java:435)
	at com.ljheee.juc.BrokenBarrierExceptionDemo$MyThread.run(BrokenBarrierExceptionDemo.java:34)
运动员3号准备完毕！在起跑线等待发令枪
java.util.concurrent.BrokenBarrierException
运动员3号看不见起跑线了
	at java.util.concurrent.CyclicBarrier.dowait(CyclicBarrier.java:207)
运动员3号退场！
	at java.util.concurrent.CyclicBarrier.await(CyclicBarrier.java:435)
	at com.ljheee.juc.BrokenBarrierExceptionDemo$MyThread.run(BrokenBarrierExceptionDemo.java:34)
运动员4号准备完毕！在起跑线等待发令枪
java.util.concurrent.BrokenBarrierException
运动员4号看不见起跑线了
	at java.util.concurrent.CyclicBarrier.dowait(CyclicBarrier.java:207)
运动员4号退场！
	at java.util.concurrent.CyclicBarrier.await(CyclicBarrier.java:435)
	at com.ljheee.juc.BrokenBarrierExceptionDemo$MyThread.run(BrokenBarrierExceptionDemo.java:34)
```
从输出可以看到，如果其中一个参与者抛出TimeoutException，其他参与者会抛出 BrokenBarrierException。


#### 如何处理 BrokenBarrierException ？
可以看到，使用 CyclicBarrier 还需注意许多事项，其中 BrokenBarrierException 被称为是 CyclicBarrier 的克星；
那又如何 处理/预防 BrokenBarrierException 呢？

当然，要预防，还需从 CyclicBarrier 的设计开始考虑，设计者已经帮我们考虑了一些问题，如检查是否被破坏，重置 CyclicBarrier 等。

**boolean isBroken():获取是否破损标志位broken的值，此值有以下几种情况：**
- CyclicBarrier初始化时，broken=false，表示屏障未破损。
- 如果正在等待的线程被中断，则broken=true，表示屏障破损。
- 如果正在等待的线程超时，则broken=true，表示屏障破损。
- 如果有线程调用CyclicBarrier.reset()方法，则broken=false，表示屏障回到未破损状态。

**void reset():使得 CyclicBarrier 回归初始状态，直观来看它做了两件事：**
- 如果有正在等待的线程，则会抛出BrokenBarrierException异常，且这些线程停止等待，继续执行。
- 将是否破损标志位broken置为false。

在任务协同阶段，我们可以借助这两个API来做辅助；
当然源码设计者肯定不能从源头将所有问题都解决，剩下的是需要我们根据业务情况，看是需要终止协作：抛异常、还是直接退出。
并且根据触发 BrokenBarrierException 的场景，我们在相关代码实现时，尽量规避。




示例程序 https://juejin.im/entry/596a05fdf265da6c4f34f2f9
API https://www.jianshu.com/p/0c8255ede7bc



- Phaser
