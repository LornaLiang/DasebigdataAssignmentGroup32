# 实验负载

## 1.数据集

| 数据集名称 | 不均衡程度 | 大小 |
| :---: | :---: | :---: |
| uniform_100MB.txt | 0% | 100MB |
| uniform_50MB.txt | 0% | 50MB |
| uniform_25MB.txt | 0% | 25MB |
| skew60_100MB.txt | 60% | 100MB |
| skew60_50MB.txt | 60% | 50MB |
| skew_60_25MB.txt | 60% | 25MB |
| extreme90_100MB.txt | 90% | 100MB |
| extreme90_200MB.txt | 90% | 200MB |
| avg_50MB |  | 50MB |

本实验一共构造了9个数据集。其中，8个数据集用于研究在不同规模和key分布下Combiner的效果：

- **均衡数据集**（key 均匀分布）：
    - 25MB、50MB、100MB 三个规模。
    - 各个 key 出现次数大致相同，没有明显热点 key。
- **倾斜数据集（60%）**：
    - 25MB、50MB、100MB 三个规模。
    - 约 60% 的记录集中在少数热点 key 上，其余记录分布在其他 key 上。
- **倾斜数据集（90%）**：
    - 100MB、200MB 两个规模。
    - 约 90% 的记录集中在更少数的热点 key 上，数据倾斜更严重。

这8个数据集用于求和任务，每条数据的记录格式为：key value，其中key为int型，value值均为1。

avg_50MB数据集用于求平均值的任务，key值均为1，value值为int型。

## 2.任务描述

### 任务一：key-value求和（有无Combiner）

任务目标是对输入数据中的所有记录，按照key进行value的求和，输出每个key的总和结果。

在Map阶段，对于输入的文本行“key value”，会解析出字符串key和整数value然后输出中间键值对（key,value)。在Combiner阶段，与Reducer一样，会在Map任务的输出阶段对相同key的value进行局部求和，以此来介绍发往下游的中间键值对的数量，从而减少网络传输和Reduce端的负载。在Reduce极端，Reducer会接收来自多个Map/Combiner的(key,valueSum)对，完成全局求和。

实验设计上，我们对每个数据集都进行两次实验。当关闭Combiner时，Map的输出会直接进入Shuffle阶段；当开启Combiner时，会在Map节点进行局部聚合。通过在不同倾斜度的数据集以及不同大小的数据集上进行实验，来探究有无Combiner对MapReduce作业性能的影响。

### 任务二：按key求平均值

任务目标是对50MB的均衡数据集key计算value的平均值。

直接在Combiner中对value求平均值会得出错误的结果，因为“平均值”本身不满足简单的结合律。所以在求平均值时，需要改变MapReduce的平均值计算模式：将平均值拆分为（sum,count).(sum,count)可以进行可结合的加法操作，得出正确的结果。

在Map阶段输出key的局部value和计数值(value,1)后，输入Combiner阶段，会输出同一key合并后的(sum,count)组，这时Combiner只做了加法，满足了结合律和交换律，能够得出正确的平均值结果，同时也大幅度减少了传输到Reducer的中间键值对数量。Reducer最后对多个Map/Combiner输出的值进行累加，得到全局的(sum,count)并求出平均值。

该任务只在avg_50MB一个数据集上进行了实验，通过对比在不使用Combiner、使用错误的模式以及使用正确的支持Combiner的模式这三种情况下的结果，探究Combiner适合使用的任务场景，设计出适合Combiner的聚合逻辑。

__通过上述两类工作负载与多组数据集的结合，本实验可以系统回答以下问题：__

1.Combiner是否能够有效减少Shuffle阶段的数据量？

2.在不同倾斜度的数据集上，Combiner的性能提升有何差异？

3.是否所有场景都适合使用Combiner？
