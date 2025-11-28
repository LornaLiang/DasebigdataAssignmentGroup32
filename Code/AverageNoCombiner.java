import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class AverageNoCombiner {

    // Mapper：解析 key value 格式，并输出 sum 和 count
    public static class AvgMapper 
            extends Mapper<LongWritable, Text, Text, LongWritable> {

        private final static Text SUM = new Text("sum");
        private final static Text COUNT = new Text("count");

        @Override
        public void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {

            // 输入格式为 "1 39"
            String[] parts = value.toString().trim().split("\\s+");
            if (parts.length != 2) return;  // 防止脏数据

            long num = Long.parseLong(parts[1]);

            context.write(SUM, new LongWritable(num));   // 数值
            context.write(COUNT, new LongWritable(1));   // 个数+1
        }
    }

    // Reducer：最终计算平均值
    public static class AvgReducer 
            extends Reducer<Text, LongWritable, Text, DoubleWritable> {

        long sum = 0;
        long count = 0;

        @Override
        public void reduce(Text key, Iterable<LongWritable> values, Context context)
                throws IOException, InterruptedException {

            if (key.toString().equals("sum")) {
                for (LongWritable val : values) sum += val.get();
            } else { // count
                for (LongWritable val : values) count += val.get();
            }
        }

        @Override
        protected void cleanup(Context context)
                throws IOException, InterruptedException {
            double avg = (double) sum / count;
            context.write(new Text("Average"), new DoubleWritable(avg));
        }
    }

    // Driver
    public static void main(String[] args) throws Exception {

        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "average_no_combiner");

        job.setJarByClass(AverageNoCombiner.class);
        job.setMapperClass(AvgMapper.class);
        job.setReducerClass(AvgReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
