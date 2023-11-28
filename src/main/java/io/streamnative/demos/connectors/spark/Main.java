package io.streamnative.demos.connectors.spark;

import java.util.concurrent.TimeoutException;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.Trigger;

public class Main {

    public static void main(String[] args) throws TimeoutException {
        SparkSession spark = SparkSession
                .builder()
                .master("k8s://https://127.0.0.1:16443")
                .appName("word-count")
                .getOrCreate();

        spark.conf().set("spark.sql.streaming.checkpointLocation", "/tmp/");

        Dataset<Row> df = spark
                .readStream()
                .format("pulsar")
                .option("service.url", "pulsar://192.168.1.120:6650")
                .option("topic", "persistent://public/default/tweet-input-topic")
                .option("startingOffsets", "earliest")
                .option("predefinedSubscription", "test")
                .load();

        df.writeStream()
                .format("pulsar")
                .option("service.url", "pulsar://192.168.1.120:6650")
                .option("topic", "persistent://public/default/spark-test-out")
               // .trigger(Trigger.ProcessingTime("30 seconds"))
                .start();
    }
}
