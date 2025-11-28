# 实验步骤

### 集群与运行环境初始化

**1.切换用户并启动Hadoop集群与YARN**

root用户不能直接启动运行Hadoop集群，需要先切换用户

```
su - hadoop
```

在master上执行

```
start-dfs.sh
start-yarn.sh
```

用于启动NameNode、DataNode、ResoureManager、NodeManager等核心服务

**2.检查各节点进程是否正常**

在各个节点可执行

```
jps
```

确认是否出现各个节点的相应进程

### 上传数据和代码

**1.准备好数据并上传至HDFS**

以本地生成数据的python脚本为例，先上传本地的.py文件，在服务器文件生成数据

**2.上传代码**

本地写好Java代码后将java文件上传到节点

```
#在节点创建目录
mkdir -p ~/mr_avg ##mr_avg文件名自己定 
cd ~/mr_avg

##打开Xftp输入命令
sftp hadoop@ip ##ip换成自己节点的公网ip

##进入创建好的文件
cd mr_avg

##依次把.java拖进去

##上传完检查 
ls -l
输出类似：
-rw-rw-r--    1 hadoop   hadoop       3726 Nov 26 16:02 AverageWithCombiner.java
权限必须是hadoop
```

并使用Hadoop的class编译

```
##依次执行，把所有的java编译
javac -classpath `hadoop classpath` -d . AverageNoCombiner.java##AverageNoCombiner.java是你的java文件名
```

**3.在Linux上打包成jar**

```
##依次打包
jar -cvf avg_no_combiner.jar AverageNoCombiner*.class
```

### 进行实验

**1.每个节点分别执行任务，依次运行在数据集上有/无combiner的实验**

运行之前需要确认自己的输出目录为空

```
##修改你需要的数据集名字，修改你的输出目录
hadoop jar avg_no_combiner.jar AverageNoCombiner /data/uniform_100MB.txt /output_LN_noC
```
运行结束后，可在控制台确认作业是否成功，如图出现
- `map 100% reduce 100%`
- `Job job_xxx completed successfully`
即为成功

![项目截图](https://github.com/LornaLiang/DasebigdataAssignmentGroup32/blob/15e862c22e1701975e751523d81eb98b85aa6bd3/image/yqr_img/real2.png)

**2.实验成功后在Web UI中查看作业详情并记录数据**

Web Ui界面中YARN ResourceManager UI可总览所有YARN任务

![项目截图](https://github.com/LornaLiang/DasebigdataAssignmentGroup32/blob/cdf132d76716b60df7ef34130b9a047e301f4262/image/yqr_img/application.png)

MapReduce Job History UI能够看到已经完成的 MapReduce Job，并且能够获取更细致的情况

![项目截图](https://github.com/LornaLiang/DasebigdataAssignmentGroup32/blob/cdf132d76716b60df7ef34130b9a047e301f4262/image/yqr_img/jobhistory.png)

如图所示，在MapReduce Job的Overview界面查看MapReduce的执行时间

![项目截图](https://github.com/LornaLiang/DasebigdataAssignmentGroup32/blob/15e862c22e1701975e751523d81eb98b85aa6bd3/image/yqr_img/uniform_noc/1.png)

如图所示，以Map的counters为例，可以记录本次实验的关键指标，如：
- Map Output Records
- Map Output Bytes
- Map Output Materialized Bytes
  等

Reduce的数据同理

![项目截图](https://github.com/LornaLiang/DasebigdataAssignmentGroup32/blob/15e862c22e1701975e751523d81eb98b85aa6bd3/image/yqr_img/uniform_noc/mapcounters1.png)

### 实验数据整理与导出

各组员将数据汇集到表格中，方便进行后续的图表绘制与数据分析
