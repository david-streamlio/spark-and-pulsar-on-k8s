package io.streamnative.demos.connectors.spark;


public class WordCount {

    public static void main(String[] args) throws InterruptedException {
//        SparkConf sparkConf = new SparkConf().setAppName("Pulsar WordCount")
//                .setMaster("k8s://https://127.0.0.1:16443");
//
//        JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, Durations.seconds(30));
//
//        ConsumerConfigurationData<byte[]> pulsarConf = new ConsumerConfigurationData();
//        Set<String> set = new HashSet();
//        set.add("persistent://public/default/tweet-input-topic");
//        pulsarConf.setTopicNames(set);
//        pulsarConf.setSubscriptionName("my-sub");
//
//        SparkStreamingPulsarReceiver pulsarReceiver = new SparkStreamingPulsarReceiver(
//                "pulsar://192.168.1.120:6650",
//                pulsarConf,
//                new AuthenticationDisabled());
//
//        JavaReceiverInputDStream<byte[]> lineDStream = jssc.receiverStream(pulsarReceiver);
//        JavaPairDStream<String, Integer> result = lineDStream.flatMap(x -> {
//                    String line = new String(x, StandardCharsets.UTF_8);
//                    List<String> list = Arrays.asList(line.split(" "));
//                    return list.iterator();
//                })
//                .mapToPair(x -> new Tuple2<String, Integer>(x, 1))
//                .reduceByKey((x, y) -> x + y);
//
//        result.print();
//
//        jssc.start();
//        jssc.awaitTermination();
    }
}
