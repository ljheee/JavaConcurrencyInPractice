
上篇 [CyclicBarrier多任务协同的利器](https://mp.weixin.qq.com/s/ABwOGa9yPq9cF7jKe84w5Q) 我们借助部门TB团建的例子，一步步分析了 CyclicBarrier 多线程协调的功能。
并在文章末尾，留出思考：
实际部门TB活动中，可能有人白天有事，不能参加公园的活动、但晚上会来聚餐；有人白天能参加，晚上不能参加；
并且公园的门票，聚餐费用，因参与人数不同，又有不同，需要统计各阶段的参与人数，以此计算经费。
需求升级后，如何实现呢？CyclicBarrier 能完成吗？

其实在上篇文章中，我们分析了初版TB需求的任务特点，其中之一就是`参与者的数量，是确定的`。
但当前需求，多个参与阶段的参与者数量，各不相同，基本确定 CyclicBarrier 完成不了。
———— 别慌，针对多个阶段，灵活设置参与者数量的场景，JDK提供了工具类 Phaser。

照旧，先看看 Phaser 的源码注释：
```java
A reusable synchronization barrier, similar in functionality to
 * {@link java.util.concurrent.CyclicBarrier CyclicBarrier} and
 * {@link java.util.concurrent.CountDownLatch CountDownLatch}
 * but supporting more flexible usage.
```
Phaser 是一个可重用的同步屏障，功能上跟 CyclicBarrier 和 CountDownLatch 相似，但支持更多灵活的用法。

看过 [CountDownLatch的两种常用场景](https://mp.weixin.qq.com/s/RhK_BrYrooGGYbN5OvyXow) 和 [CyclicBarrier多任务协同的利器](https://mp.weixin.qq.com/s/ABwOGa9yPq9cF7jKe84w5Q) 的朋友，一定了解：CountDownLatch 能够实现一个或多个线程阻塞等待，直到其他线程完成后再执行；
而 CyclicBarrier 允许多个线程相互等待，直到所有参与者到达屏障同步点后，再往下执行。
可以说，Phaser 是二者功能的增强和结合。

#### Phaser 阶段协同器
Java 7 中增加的一个用于多阶段同步控制的工具类，他包含了 CycIicBarrier 和 CountDownLatch 的相关功能，让它们更强大灵活。
下面通过部门TB，多阶段不同参与者的例子，具体探究 Phaser 的原理。

###### 部门团建，需求升级
公司组织周末郊游，大家各自从公司出发到公园集合，大家都到了之后，出发到公园各自游玩，然后在公园门口集合，再去餐厅就餐，大家都到了就可以用餐，有的员工白天有事，选择
参加晚上的聚餐，有的员工则晚上有事，只参加白天的活动。

任务特点分析:
➢多阶段协同，但阶段的参与数是可变的，用 CyclicBarrier 好像不好实现。
➢假定 第一阶段：到公司集合5人(任务数5)，去公园游玩。
➢第二阶段：到公园门口集合，有2人因晚上有事，自行回家了；则3人去餐厅，这是减少参与数(任务数变为3)
➢第三阶段：餐厅集合，有另4人参与聚餐，这是增加参与数(任务数变为7)

实际上，当前任务最大的特点是：多阶段等待一起出发、每阶段的任务数可灵活调整。

多个线程协作执行的任务，分为多个阶段，每个阶段都可以有任意个参与者线程，可以随时注册并参与到某个阶段；
当一个阶段中所有任务都完成之后，Phaser 的 onAdvance() 被调用(可以通过覆盖添加自定文处理逻辑(类似CyclicBarrier循环屏障使用的Runnable接口))，然后Phaser释放等待线程，自动进入下个阶段，如此循环，直到Phaser不再包含任何参与者。

由于 Phaser 比较复杂，API也较为繁多，下面将 Phaser 提供的API分为多组。
**构造方法**
- newPhaser() 不指定数量，参与任务数为0。
- new Phaser(int parties) 指定初始参与任务数
- new Phaser(Phaser phaser) 指定父阶段器，子对象整体作为一个参与者加入到父对象，当子对象中没有参与者时，自动从父对象解除注册
- new Phaser(Phaser phaser，int parties) 

**增减参与任务数方法**
- int register() 增加一个数，返回当前阶段号。
- int bulkRegister(int parties) 增加指定个数，返回当前阶段号。
- int arriveAndDeregister() 减少一个任务数，返回当前阶段号。

**到达、等待方法**
- int arrive() 到达(任务完成)，返回当前阶段号。
- int arriveAndAwaitAdvance() 到达后等待其他任务到达，返回到达阶段号。
- int awaitAdvance(int phase) 在指定阶段等待(必须是当前阶段才有效)
- int awaitAdvanceInterruptibly(int phase) 阶段到达触发动作
- int awaitAdvanceInterruptiBly(int phase，long timeout，TimeUnit unit)
- protected boolean onAdvance(int phase，int registeredParties)类似CyclicBarrier的触发命令，通过重写该方法来增加阶段到达动作，该方法返回true将终结Phaser对象。

**Phaser其他API:**
- void forceTermination() 强制结束
- int getPhase() 获取当前阶段号
- boolean isTerminated() 判断是否结束


**注意事项:**
单个 Phaser 实例允许的注册任务数的.上限是65535，如果参与任务数超过，可以用父子Phaser树的方式，通过父子关联来增加参与者上限。
> 为什么是65535，这和 Phaser 的实现有关:
Phaser中的state状态，64位的属性state不同位被用来存放不同的值，低16位存放unarrived，低32位中的高16位存放parties，高32位的低31位存放phase，最高位存放terminated，即Phaser是否关闭；
2^16=65536

#### Phaser 实现多任务协同
下面来看，如何使用 Phaser 完成多阶段任务协同。
我们首先将团建的不同阶段任务，定义在 StaffTask ：
```java
static final Random random = new Random();

static class StaffTask {
    public void step1Task() throws InterruptedException {
        // 第一阶段：来公司集合
        String staff = "员工【" + Thread.currentThread().getName() + "】";
        System.out.println(staff + "从家出发了……");
        Thread.sleep(random.nextInt(5000));
        System.out.println(staff + "到达公司");
    }
    
    public void step2Task() throws InterruptedException {
        // 第二阶段：出发去公园
        String staff = "员工【" + Thread.currentThread().getName() + "】";
        System.out.println(staff + "出发去公园玩");
        Thread.sleep(random.nextInt(5000));
        System.out.println(staff + "到达公园门口集合");
    
    }
    
    public void step3Task() throws InterruptedException {
        // 第三阶段：去餐厅
        String staff = "员工【" + Thread.currentThread().getName() + "】";
        System.out.println(staff + "出发去餐厅");
        Thread.sleep(random.nextInt(5000));
        System.out.println(staff + "到达餐厅");
    
    }
    
    public void step4Task() throws InterruptedException {
        // 第四阶段：就餐
        String staff = "员工【" + Thread.currentThread().getName() + "】";
        System.out.println(staff + "开始用餐");
        Thread.sleep(random.nextInt(5000));
        System.out.println(staff + "用餐结束，回家");
    }
}
```
还是用随机数，模拟不同参与者的耗时。

重点是下面的 main 方法：
```java
public static void main(String[] args) {

    final Phaser phaser = new Phaser() {
        @Override
        protected boolean onAdvance(int phase, int registeredParties) {
            // 参与者数量，去除主线程
            int staffs = registeredParties - 1;
            switch (phase) {
                case 0:
                    System.out.println("大家都到公司了，出发去公园，人数：" + staffs);
                    break;
                case 1:
                    System.out.println("大家都到公司门口了，出发去餐厅，人数：" + staffs);
                    break;
                case 2:
                    System.out.println("大家都到餐厅了，开始用餐，人数：" + staffs);
                    break;

            }

            // 判断是否只剩下主线程（一个参与者），如果是，则返回true，代表终止
            return registeredParties == 1;
        }
    };

    // 注册主线程 ———— 让主线程全程参与
    phaser.register();
    final StaffTask staffTask = new StaffTask();

    // 3个全程参与TB的员工
    for (int i = 0; i < 3; i++) {
        // 添加任务数
        phaser.register();
        new Thread(() -> {
            try {
                staffTask.step1Task();
                phaser.arriveAndAwaitAdvance();

                staffTask.step2Task();
                phaser.arriveAndAwaitAdvance();

                staffTask.step3Task();
                phaser.arriveAndAwaitAdvance();

                staffTask.step4Task();
                // 完成了，注销离开
                phaser.arriveAndDeregister();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // 两个不聚餐的员工加入
    for (int i = 0; i < 2; i++) {
        phaser.register();
        new Thread(() -> {
            try {
                staffTask.step1Task();
                phaser.arriveAndAwaitAdvance();

                staffTask.step2Task();
                System.out.println("员工【" + Thread.currentThread().getName() + "】回家了");
                // 完成了，注销离开
                phaser.arriveAndDeregister();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    while (!phaser.isTerminated()) {
        int phase = phaser.arriveAndAwaitAdvance();
        if (phase == 2) {
            // 到了去餐厅的阶段，又新增4人，参加晚上的聚餐
            for (int i = 0; i < 4; i++) {
                phaser.register();
                new Thread(() -> {
                    try {
                        staffTask.step3Task();
                        phaser.arriveAndAwaitAdvance();

                        staffTask.step4Task();
                        // 完成了，注销离开
                        phaser.arriveAndDeregister();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    }
}
```
先给出运行结果，直观感受下：
```
员工【Thread-0】从家出发了……
员工【Thread-2】从家出发了……
员工【Thread-1】从家出发了……
员工【Thread-3】从家出发了……
员工【Thread-4】从家出发了……
员工【Thread-4】到达公司
员工【Thread-0】到达公司
员工【Thread-1】到达公司
员工【Thread-3】到达公司
员工【Thread-2】到达公司
大家都到公司了，出发去公园，人数：5
员工【Thread-2】出发去公园玩
员工【Thread-1】出发去公园玩
员工【Thread-4】出发去公园玩
员工【Thread-3】出发去公园玩
员工【Thread-0】出发去公园玩
员工【Thread-1】到达公园门口集合
员工【Thread-2】到达公园门口集合
员工【Thread-0】到达公园门口集合
员工【Thread-3】到达公园门口集合
员工【Thread-3】回家了
员工【Thread-4】到达公园门口集合
员工【Thread-4】回家了
大家都到公司门口了，出发去餐厅，人数：3
员工【Thread-2】出发去餐厅
员工【Thread-0】出发去餐厅
员工【Thread-1】出发去餐厅
员工【Thread-5】出发去餐厅
员工【Thread-6】出发去餐厅
员工【Thread-7】出发去餐厅
员工【Thread-8】出发去餐厅
员工【Thread-8】到达餐厅
员工【Thread-7】到达餐厅
员工【Thread-1】到达餐厅
员工【Thread-5】到达餐厅
员工【Thread-2】到达餐厅
员工【Thread-6】到达餐厅
员工【Thread-0】到达餐厅
大家都到餐厅了，开始用餐，人数：7
员工【Thread-0】开始用餐
员工【Thread-8】开始用餐
员工【Thread-7】开始用餐
员工【Thread-1】开始用餐
员工【Thread-5】开始用餐
员工【Thread-2】开始用餐
员工【Thread-6】开始用餐
员工【Thread-5】用餐结束，回家
员工【Thread-2】用餐结束，回家
员工【Thread-7】用餐结束，回家
员工【Thread-1】用餐结束，回家
员工【Thread-6】用餐结束，回家
员工【Thread-8】用餐结束，回家
员工【Thread-0】用餐结束，回家
```
怎么样，各个阶段有各的任务，并且各个阶段参与者数量也不同。

###### 代码分析
1、Phaser 的创建
```java
final Phaser phaser = new Phaser() {
    @Override
    protected boolean onAdvance(int phase, int registeredParties) {
        // 参与者数量，去除主线程
        int staffs = registeredParties - 1;
        switch (phase) {
            case 0:
                System.out.println("大家都到公司了，出发去公园，人数：" + staffs);
                break;
            case 1:
                System.out.println("大家都到公司门口了，出发去餐厅，人数：" + staffs);
                break;
            case 2:
                System.out.println("大家都到餐厅了，开始用餐，人数：" + staffs);
                break;

        }

        // 判断是否只剩下主线程（一个参与者），如果是，则返回true，代表终止
        return registeredParties == 1;
    }
};
```
创建 Phaser 时，重写了 onAdvance() 方法。这个方法类似于 [CyclicBarrier多任务协同的利器](https://mp.weixin.qq.com/s/ABwOGa9yPq9cF7jKe84w5Q) 文中所讲的CyclicBarrier的回调函数，在每个阶段结束后，处理一些收尾工作。
不同的是，onAdvance() 方法更高级，方法入参直接告诉我们了当前阶段，和该阶段结束时的参与者数量；onAdvance() 方法签名如下：
```java
protected boolean onAdvance(int phase, int registeredParties) 
```
因此，重写 onAdvance() 方法后，我们可以直接使用 phase 拿到当前阶段，registeredParties 为该阶段结束时的参与者数量。

为了不让主进程结束，在创建完 phaser 对象后，立即注册了参与者，该参与者是主线程，也就是让主线程全程参与。
```java
// 注册主线程 ———— 让主线程全程参与
phaser.register();
```

2、多阶段任务协同
随后，我们创建了3个线程，代表3个全程参与团建的员工；
```java
for (int i = 0; i < 3; i++) {
    // 添加任务数
    phaser.register();
    new Thread(() -> {
        try {
            staffTask.step1Task();
            phaser.arriveAndAwaitAdvance();

            staffTask.step2Task();
            phaser.arriveAndAwaitAdvance();

            staffTask.step3Task();
            phaser.arriveAndAwaitAdvance();

            staffTask.step4Task();
            // 完成了，注销离开
            phaser.arriveAndDeregister();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }).start();
}
```
在每次创建线程前，使用 phaser.register(); 添加参与者数量；

在参与者完成每个阶段时，调用 phaser.arriveAndAwaitAdvance(); 进行协同等待，等所有参与者 都到达同步点后，再进入下一阶段。
arriveAndAwaitAdvance() 从方法名也能看出，就是报告自己到达了同步点，并且协同、等待 onAdvance() 方法的执行。

在最后一个阶段任务完成时，调用 phaser.arriveAndDeregister(); 代表：等这次协作完成后，我就离开。

接着，创建了2个线程，代表不聚餐的员工，线程的工作内容仅仅是前两个阶段的任务。

3、在第二阶段，加入新的参与者
最后，用了一个 while 判断，检查 phaser 的任务阶段，在第二阶段，新增了四个参与者，继续参加后续任务的协作。
```java
while (!phaser.isTerminated()) {
    int phase = phaser.arriveAndAwaitAdvance();
    if (phase == 2) {
        // 到了去餐厅的阶段，又新增4人，参加晚上的聚餐
        for (int i = 0; i < 4; i++) {
            phaser.register();
            new Thread(() -> {
                try {
                    staffTask.step3Task();
                    phaser.arriveAndAwaitAdvance();

                    staffTask.step4Task();
                    // 完成了，注销离开
                    phaser.arriveAndDeregister();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
```

#### Phaser 核心方法
从上面示例的代码，可以看到频繁使用的的也就几个方法：
- arriveAndAwaitAdvance()：类似于CyclicBarrier的await()方法，等待其它线程都到屏障点之后，再继续执行。
- arriveAndDeregister()：把执行到此的线程从Phaser中注销掉。
- isTerminated()：判断 Phaser 是否终止。
- register()：将一个新的参与者注册到 Phaser 中，这个新的参与者会被当成`没有执行本阶段的线程`。
- forceTermination()：强制 Phaser 进入终止态。

多阶段协同，示意图如下：



#### Phaser 的父子层级
Phaser 支持层级，根root Phaser、父Phaser把每个子的 Phaser 当作父Phaser的一个parties，相当于把子 Phaser 内的一组参与者当初父Phaser的成员；这个子 Phaser 的内部有多少个parties线程，有多少阶段，均可自定义。

父Phaser等待所有的parties都到达父的阶段屏障，
即子Phaser的所有阶段都执行完，也就是子Phaser都到达父的阶段屏障，父Phaser才会进入下一阶段：唤醒所有的子Phaser的parties线程继续执行下一阶段。

CyclicBarrier 也可以作为阶段屏障使用，每个线程重复做为CyclicBarrier的parties，但是没办法像Phaser那样支持层级。
例如比赛,一个比赛分为3个阶段(phase): 初赛、复赛和决赛，规定所有运动员都完成上一个阶段的比赛才可以进行下一阶段的比赛，并且比赛的过程中存在晋级、允许退赛(deregister)，晋级成功且未退赛的才能进入下一阶段，这个场景就很适合Phaser。

###### 总结
JUC包下的CyclicBarrier、CountDownLatch、Phaser 三个都是线程同步辅助工具类，同步辅助三剑客。
CountDownLatch不能重用，CyclicBarrier、Phaser都可以重用，并且Phaser
更加灵活可以在运行期间随时加入（register）新的parties，也可以在运行期间随时退出（deregister）。

关于 CyclicBarrier、CountDownLatch 可阅读 [CountDownLatch的两种常用场景](https://mp.weixin.qq.com/s/RhK_BrYrooGGYbN5OvyXow) 、[CyclicBarrier多任务协同的利器](https://mp.weixin.qq.com/s/ABwOGa9yPq9cF7jKe84w5Q)。





                