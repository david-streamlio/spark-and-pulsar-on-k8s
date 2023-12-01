# Development Guide

If you want to use your own class rather than one of the example classes, then you will have to build an image that includes
the artifact on the image, e.g.

```
FROM apache/spark:3.5.0
WORKDIR /app
COPY /target/test-application.jar ./app.jar
CMD [“java”, “--jar”, “app.jar”]
```

-----------
References
-----------

1. https://spark.apache.org/examples.html
2. https://github.com/apache/spark/tree/master/examples/src/main/java/org/apache/spark/examples/streaming