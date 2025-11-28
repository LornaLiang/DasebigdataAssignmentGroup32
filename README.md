# 📘 **实验报告：MapReduce Combiner 机制分析**

### **研究目的：探究MapReduce中Combiner对作业性能的影响**

------

# **1. 实验环境**

## **1.1 硬件环境（集群配置）**

本次实验基于阿里云 ECS 自建 Hadoop 集群完成，共包含 **4 个节点**：1 个 Master 节点与 3 个 Worker 节点。集群采用 Hadoop **3.4.2** 版本，统一使用 **Linux (Ubuntu 20.04)** 操作系统，并通过 YARN 作为资源管理与任务调度框架。

| 节点    | 角色                       | vCPU | 内存 | 存储类型 | 带宽     |
| ------- | -------------------------- | ---- | ---- | -------- | -------- |
| master  | NameNode + ResourceManager | 2 核 | 2GB  | SSD      | 100 Mbps |
| worker1 | DataNode + NodeManager     | 2 核 | 2GB  | SSD      | 100 Mbps |
| worker2 | DataNode + NodeManager     | 2 核 | 2GB  | SSD      | 100 Mbps |
| worker3 | DataNode + NodeManager     | 2 核 | 2 GB | SSD      | 100 Mbps |

## **1.2 软件环境**

| 组件   | 版本              |
| ------ | ----------------- |
| OS     | Ubuntu 22.04 64位 |
| JDK    | JDK 1.8.0_472     |
| Hadoop | Hadoop 3.4.2      |
| SSH    | Xshell 8          |

------

## 1.3 Hadoop服务进程

(1)master节点
![](image/comfiguration/jps/master_jps.png)


(2)worker1
![](image/comfiguration/jps/worker1_jps.png)


(3)worker2
![](image/comfiguration/jps/worker2_jps.png)



(4)worker3
![](image/comfiguration/jps/worker3_jps.png)






## 1.4 集群端口

| 服务                 | 端口  | 作用                        |
| -------------------- | ----- | --------------------------- |
| HDFS NameNode        | 9870  | 查看 HDFS 文件系统状态      |
| YARN ResourceManager | 8088  | 查看 Application 执行情况   |
| JobHistory Server    | 19888 | 查看 MapReduce 详细历史信息 |

(1)Web UI 9870端口
![](image/comfiguration/9870.png)
(2)Web UI 8088端口
![](image/comfiguration/8088.png)
## 1.5 HDFS存储结构
![](image/comfiguration/jps/hdfs_data.png)


## 1.6 集群部署方式

- 采用 阿里云 ECS + VPC 内网 部署

- 配置 SSH 免密通信，实现自动启动 Hadoop 集群

- 所有配置（core-site.xml, hdfs-site.xml, yarn-site.xml, mapred-site.xml）保持一致

#  2.实验负载

## 2.1数据集

|     数据集名称      | 不均衡程度 | 大小  |
| :-----------------: | :--------: | :---: |
|  uniform_100MB.txt  |     0%     | 100MB |
|  uniform_50MB.txt   |     0%     | 50MB  |
|  uniform_25MB.txt   |     0%     | 25MB  |
|  skew60_100MB.txt   |    60%     | 100MB |
|   skew60_50MB.txt   |    60%     | 50MB  |
|  skew_60_25MB.txt   |    60%     | 25MB  |
| extreme90_100MB.txt |    90%     | 100MB |
| extreme90_200MB.txt |    90%     | 200MB |
|      avg_50MB       |            | 50MB  |

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

## 2.2 任务描述

### 任务一：key-value求和（有无Combiner）

任务目标是对输入数据中的所有记录，按照key进行value的求和，输出每个key的总和结果。

在Map阶段，对于输入的文本行“key value”，会解析出字符串key和整数value然后输出中间键值对（key,value)。在Combiner阶段，与Reducer一样，会在Map任务的输出阶段对相同key的value进行局部求和，以此来介绍发往下游的中间键值对的数量，从而减少网络传输和Reduce端的负载。在Reduce极端，Reducer会接收来自多个Map/Combiner的(key,valueSum)对，完成全局求和。

实验设计上，我们对每个数据集都进行两次实验。当关闭Combiner时，Map的输出会直接进入Shuffle阶段；当开启Combiner时，会在Map节点进行局部聚合。通过在不同倾斜度的数据集以及不同大小的数据集上进行实验，来探究有无Combiner对MapReduce作业性能的影响。

### 任务二：按key求平均值

任务目标是对50MB的均衡数据集key计算value的平均值。

直接在Combiner中对value求平均值，然后再在Reducer中对这些“局部平均值”再求平均会得出错误的结果，因为“平均值”本身不满足简单的结合律。所以在求平均值时，需要改变MapReduce的平均值计算模式：将平均值拆分为sum和count两个部分。这两个部分可以进行可结合的加法操作，得出正确的结果。

在Map阶段对于输入的每一条记录，输出(sum,value)和（count,1)，输入Combiner阶段，会对sum和count进行分别求和，这时Combiner只做了加法，满足了结合律和交换律，不会影响最终的结果，同时也大幅度减少了传输到Reducer的中间键值对数量。Reducer最后对多个Map/Combiner输出的值进行累加，得到全局的(sum,count)并求出平均值。

该任务只在avg_50MB一个数据集上进行了实验，通过对比在不使用Combiner、使用错误的模式以及使用正确的支持Combiner的模式这三种情况下的结果，探究Combiner适合使用的任务场景，设计出适合Combiner的聚合逻辑。

__通过上述两类工作负载与多组数据集的结合，本实验可以系统回答以下问题：__

1.Combiner是否能够有效减少Shuffle阶段的数据量？

2.在不同倾斜度的数据集上，Combiner的性能提升有何差异？

3.是否所有场景都适合使用Combiner？

> [!IMPORTANT]
>
> ### 为什么key-value 求和任务适合用 combiner
>
> 因为Combiner本质上是一个在Map端的“迷你Reducer”，它必须能够以任意顺序、任意分组方式处理Map输出的中间结果，而不影响最终结果的正确定性，而结合律和交换律正是保证这种“任意性”不会导致错误的关键属性。
>
> Combiner的作用
>
> 　　在MapReduce中，Combiner的作用是在Map任务完成后，在本地先对输出的<key, value>对进行一次合并，然后再将结果通过网络发送给Reducer。
>
> 　　目的：减少Map和Reduce任务之间的网络传输数据量，提升整体作业性能。
>
> 　　运行位置：它在每个Map任务节点上运行，处理该Map任务自己产生的中间结果。
>
> 因为加法满足结合律和交换律，这两个性质保证了无论数据是如何分布的，只要最终的操作是加法，结果将始终是一样的。这正是使用combiner时的核心优势。
>
> 在 MapReduce 中，数据从 Map 阶段传输到 Reduce 阶段时，传输的中间数据量可能会非常大。通过使用 combiner，我们可以在Map 任务的节点上对相同的 key 进行局部求和，然后将求和结果传递给 Reducer。这样可以显著减少需要传输到Reducer 的数据量。
>
> 由于加法的结合律和交换律，局部合并的顺序不会影响最终结果。即使不同的 Map 任务在不同的节点上并行运行，Reducer 仍然能够得到正确的总和，因为各个节点上的局部结果会被合并成最终的正确结果。这保证了分布式计算中的一致性和正确性。
>
> 使用 combiner 可以减少 Reducer 端的计算负担，因为它收到的数据已经是部分合并过的。例如，Reducer 不需要重复计算每个 key 的所有值的和，而是直接对局部合并的结果进行处理，进一步提高了计算效率。


