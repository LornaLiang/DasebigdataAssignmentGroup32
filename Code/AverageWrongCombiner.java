import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class AverageWrongCombiner {

    // Mapper：解析 "key value"
    public static class AvgMapper
            extends Mapper<LongWritable, Text, Text, LongWritable> {

        private final static Text ONEKEY = new Text("avg");

        @Override
        public void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {

            String[] parts = value.toString().split("\\s+");
            if (parts.length != 2) return;

            long num = Long.parseLong(parts[1]);
            context.write(ONEKEY, new LongWritable(num));
        }
    }

    // ❌ 错误 Combiner：计算局部平均值 → 极其错误！
    public static class WrongCombiner
            extends Reducer<Text, LongWritable, Text, LongWritable> {

        @Override
        public void reduce(Text key, Iterable<LongWritable> values, Context context)
                throws IOException, InterruptedException {

            long sum = 0, count = 0;
            for (LongWritable v : values) {
                sum += v.get();
                count++;
            }
            long localAvg = sum / count;   // ❌ 计算局部平均
            context.write(key, new LongWritable(localAvg));
        }
    }

    // Reducer：再对局部平均求平均（更错）
    public static class WrongReducer
            extends Reducer<Text, LongWritable, Text, DoubleWritable> {

        @Override
        protected void reduce(Text key, Iterable<LongWritable> values, Context context)
                throws IOException, InterruptedException {

            long sum = 0, count = 0;

            for (LongWritable v : values) {
                sum += v.get();
                count++;
            }

            double wrongFinalAvg = (double) sum / count;
            context.write(new Text("WrongAverage"), new DoubleWritable(wrongFinalAvg));
        }
    }

    // Driver
    public static void main(String[] args) throws Exception {

        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "average_wrong_combiner");

        job.setJarByClass(AverageWrongCombiner.class);
        job.setMapperClass(AvgMapper.class);
        job.setCombinerClass(WrongCombiner.class);   // ❌ 错误 Combiner
        job.setReducerClass(WrongReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
