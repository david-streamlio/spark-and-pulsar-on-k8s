package io.streamnative.demos.connectors.spark;

import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.streaming.Trigger;

public class WordCount {

    private static final Pattern SPACE = Pattern.compile(" ");

    public static void main(String[] args) throws TimeoutException, StreamingQueryException {

        SparkSession spark = SparkSession
                .builder()
                .appName("Word Count")
                .getOrCreate();

        spark.conf().set("spark.sql.streaming.checkpointLocation", "/tmp");

        Dataset<Row> lines = spark
                .readStream()
                .format("pulsar")
                .option("service.url", "pulsar://192.168.1.120:6650")
                .option("topic", "persistent://public/default/tweet-input-topic")
                .option("startingOffsets", "earliest")
                .option("predefinedSubscription", "test")
                .load();

        Dataset<Row> words = lines.selectExpr("explode(split(value, ' ')) as word");

        // Perform word count
        Dataset<Row> wordCounts = words
                .groupBy("word")
                .count();

        // Define your query to process the streaming data
        StreamingQuery query = wordCounts
                .writeStream()
                .outputMode("update") // Change output mode as per your requirement
                .format("pulsar")
                .option("service.url", "pulsar://192.168.1.120:6650")
                .option("topic", "persistent://public/default/tweet-word-count")
                .trigger(Trigger.ProcessingTime(30000))
                .start();

        // Start the streaming query
        query.awaitTermination();
    }
}
