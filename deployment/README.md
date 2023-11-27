# Submitting Applications to Kubernetes

Spark can run on clusters managed by Kubernetes. This feature makes use of native Kubernetes scheduler that has been 
added to Spark. In order to this, you must first download the Apache Spark distribution.

TODO: curl, tar -xvf

## How it Works
The `spark-submit` command can be directly used to submit a Spark application to a Kubernetes cluster. The submission 
mechanism works as follows:

- Spark creates a Spark driver running within a Kubernetes pod. 
- The driver creates executors which are also running within Kubernetes pods and connects to them, and executes 
application code. 
- When the application completes, the executor pods terminate and are cleaned up, but the driver pod persists logs and 
remains in “completed” state in the Kubernetes API until it’s eventually garbage collected or manually cleaned up.

![k8s-cluster-mode.png](images%2Fk8s-cluster-mode.png)

Note that in the completed state, the driver pod does not use any computational or memory resources.

The driver and executor pod scheduling is handled by Kubernetes. Communication to the Kubernetes API is done via fabric8. 
It is possible to schedule the driver and executor pods on a subset of available nodes through a node selector using the 
configuration property for it.

## Docker Images
Kubernetes requires users to supply images that can be deployed into containers within pods. The images are built to be 
run in a container runtime environment that Kubernetes supports. Starting with version 2.3, Spark ships with a 
Dockerfile that can be customized to match an individual application’s needs. Instructions for building a Docker image 
from the Spark distribution is covered [here](https://spark.apache.org/docs/latest/running-on-kubernetes.html#docker-images).

Alternatively, you can also use the Apache Spark Docker images (such as `apache/spark:<version>`) directly. This is the approach
taken throughout the remainder of the guide.


## Deployment via the Command Line
You can use the `spark-submit` command (included in the Spark distribution), to launch a Spark application in cluster-mode.
The following command will run the example Spark application that calculates the value of Pi.


```
$SPARK_HOME/bin/spark-submit \
    --master k8s://https://<k8s-apiserver-host>:<k8s-apiserver-port> \
    --deploy-mode cluster \
    --name spark-pi \
    --class org.apache.spark.examples.SparkPi \
    --conf spark.executor.instances=5 \
    --conf spark.kubernetes.container.image=apache/spark:3.5.0 \
    --conf spark.kubernetes.authenticate.driver.serviceAccountName=spark \
    local:///opt/spark/examples/jars/spark-examples_2.12-3.5.0.jar
```

Let’s explain the arguments used in greater detail:

- `-— master`: Kubernetes cluster’s entrypoint, which serves as the control plane for the cluster. You can find this 
information by running the command `kubectl cluster-info`. This must be a URL with the format `k8s://<api_server_host>:<k8s-apiserver-port>`.

- `-- deploy-mode=cluster`: This mode allows the Spark application to be distributed across multiple worker nodes within
the Kubernetes cluster. The other deployment-mode option is `client`

- `--name spark-pi`: In Kubernetes mode, the Spark application name that is specified by this property is used 
to name the Kubernetes resources created. So all the K8s pods associated with this application will include this name. 

- `--class org.apache.spark.examples.SparkPi`: The fully qualified class name of the application to run. This class must 
exist on the classpath of the Spark executors, and is typically found in the artifact passed at the end of this command,
e.g. `local:///path/to/examples.jar`

We also need to configure additional properties using the “ — conf” parameter.

- `-— conf spark.executor.instances=5`: specifies the number of executor instances to be used for the application. 
- `-— confspark.kubernetes.container.image=<docker-image>` specifies the Docker image to be used for the Spark application.
- `-— conf spark.kubernetes.authenticate.driver.serviceAccountName=<k8s service account>` sets the service account name 
to authenticate the Spark driver.

The last bit of information you need to submit a Spark job from the command line is the location of the artifact that
contains the application code/class. You have to pass this in the `spark-submit` command as a local file within the Docker
image you are using. In this case `local:///opt/spark/examples/jars/spark-examples_2.12-3.5.0.jar` works because the examples
jar is included in the `apache:spark:3.5.0` docker image we are using.

NOTE: You can also pass the jar location that is stored in S3 or HDFS and add [configurations](https://spark.apache.org/docs/latest/running-on-kubernetes.html#dependency-management) 
required for the driver pod to download the jar.


## Deployment via kubectl
The Kubernetes Operator for Apache Spark aims to make specifying and running Spark applications as easy and idiomatic as 
running other workloads on Kubernetes. It uses Kubernetes custom resources for specifying, running, and surfacing status
of Spark applications. Among these CRDs, is the `SparkApplication` which allows you to manage Spark applications using
`kubectl`


### Using a SparkApplication
The operator runs Spark applications specified in Kubernetes objects of the `SparkApplication` custom resource type. 
The most common way of using a `SparkApplication` is store the SparkApplication specification in a YAML file and use 
the `kubectl` command. The operator automatically submits the application as configured in a SparkApplication to run on 
the Kubernetes cluster, effectively performing the `spark-submit` command on our behalf.

### Configuring a SparkApplication (Mandatory)
A SparkApplication also needs a .spec section. This section contains fields for specifying various aspects of an 
application including its type (Scala, Java, Python, or R), deployment mode (cluster or client), main application 
resource URI (e.g., the URI of the application jar), main class, arguments, etc.

Below is an example showing part of a SparkApplication specification:

```
apiVersion: sparkoperator.k8s.io/v1beta2
kind: SparkApplication
metadata:
  name: spark-pi
  # Use the namespace of the K8s ServiceAccount for Spark Applications
  namespace: spark-apps
spec:
  type: Scala
  mode: cluster
  image: "apache/spark:3.5.0"
  mainClass: org.apache.spark.examples.SparkPi
  mainApplicationFile: "local:///opt/spark/examples/jars/spark-examples_2.12-3.5.0.jar"
```

### Specifying the Spark Driver configuration (Mandatory)
The driver pod by default uses the default service account in the namespace it is running in to talk to the Kubernetes 
API server. The default service account, however, typically does not have sufficient permissions to create executor pods
and the headless service used by the executors to connect to the driver. It was for this reason that we created a special
K8s service account for the `spark-apps` namespace and granted it `edit` permissions when we installed the Spark K8s
operator.

Therefore, we must now specify the name of the custom service account to use in the `SparkApplication` specification as 
shown here:

```
spec:
  driver:
    cores: 1
    coreLimit: "1200m"
    memory: "512m"
    labels:
      version: 3.5.0
    serviceAccount: spark
```

### Specifying Application Dependencies (Optional)
Often Spark applications need additional files additionally to the main application resource to run. Such application 
dependencies can include for example jars and data files the application needs at runtime. To support specification of 
application dependencies, a SparkApplication uses an optional field `.spec.deps` that supports specifying jars and files.

The following is an example specification with both container-local (i.e., within the container) and remote dependencies:

```
spec:
  deps:
    jars:
      - local:///opt/spark-jars/gcs-connector.jar
    files:
      - gs://spark-data/data-file-1.txt
      - gs://spark-data/data-file-2.txt
```

This approach allows you to utilize dependencies built within a docker image, as well as resources hosted in public cloud
storage. It's also possible to specify additional jars to obtain from a remote Maven repository by adding maven coordinates
to `.spec.deps.packages`

This technique is useful for bringing in dependencies without having to create and manage an entirely separate Docker
build file and image. For instance, this can be used to bring in [Apache Pulsar's Spark connector](https://docs.streamnative.io/hub/data-processing-pulsar-spark-3.2).

```
spec:
  deps:
    repositories:
      - https://dl.bintray.com/streamnative/maven
    packages:
      - io.streamnative.connectors:pulsar-spark-connector_{{SCALA_BINARY_VERSION}}:3.2.0
```


-----------------
References
-----------------
1. https://spark.apache.org/docs/latest/running-on-kubernetes.html#submitting-applications-to-kubernetes
