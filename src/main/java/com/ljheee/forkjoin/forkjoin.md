#### Fork-Joink框架
* RecursiveAction 无返回值的任务，如排序
* RecursiveTask 有返回值的任务，如累计求和
* CountedCompleter任务完成之后，回调onComplete() 触发其他任务;


##### 任务的拆分
```
// 方式一
left.fork();
right.fork();
invokeAll(left,right);
return left.join() + right.join();


// 方式二
left.fork();
right.fork();
// 把两个小任务累加的结果合并起来
return left.join() + right.join();


//优化
left.fork();
return left.join() + right.compute();//只fork一边,和上面3行代码等效
```


总结
- ForkJoinPool 默认线程池大小=CPU核心数
    并非设置越大越好，IO密集型任务 受限于磁盘IO。
- fork-join和ThreadPool，各自有各自的应用场景，二者是并存互补的关系。
- 递归任务，很适合用fork-join。如分治任务：分而治之，父任务，依赖于子任务的完成。
