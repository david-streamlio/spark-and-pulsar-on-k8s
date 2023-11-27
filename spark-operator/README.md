# Getting Started with Pulsar and Spark on K8s

This guide will demonstrate how to develop an Apache Spark application that consumes data from Apache Pulsar, and 
deploy it inside a Kubernetes environment.

--------------------
Prerequisites
--------------------
- A running Kubernetes cluster at version >= 1.24 with access configured to it using kubectl and helm.

- You must have appropriate permissions to list, create, edit and delete pods in your cluster. You can verify that you 
can list these resources by running: `kubectl auth can-i <list|create|edit|delete> pods`.

- You must have Kubernetes DNS configured in your cluster.

- Please note that the Spark K8s operator requires the [Operator Lifecycle Manager](https://olm.operatorframework.io/) (OLM) to be installed inside your Kubernetes
  environment. If you haven't already installed OLM, please follow the installation instructions for your
  environment before proceeding.

--------------------
Spark K8s Operator Installation Guide
--------------------
This section cover the process of installing the K8s operator for Spark inside your local environment.

## Step 1: Create a K8s ServiceAccount for Spark Applications
By default, spark operator will listen to instructions for Spark applications from all namespaces. However, we don’t 
want to enable that feature to avoid any security issues. It is common in production environment to restrict the 
operator to listen only for instructions coming from a certain namespace.

We need first to create a namespace specific to handle spark operator instructions. Start by creating a K8s namespace 
specifically for Spark applications. This namespace will contain all Spark-related resources and components.

Use the following commands to create the namespace, service account, and cluster binding we will use to run our Spark 
applications.

```
export SPARK_APPS_K8S_NS=spark-apps
kubectl create namespace $SPARK_APPS_K8S_NS
kubectl create serviceaccount spark --namespace=$SPARK_APPS_K8S_NS
kubectl create clusterrolebinding spark-role --clusterrole=edit --serviceaccount=spark-apps:spark --namespace=$SPARK_APPS_K8S_NS
```

## Step 2: Install the Spark Kubernetes Operator
The Kubernetes Operator for Apache Spark aims to make specifying and running Spark applications as easy and idiomatic 
as running other workloads on Kubernetes.

The easiest way to install the Kubernetes Operator for Apache Spark is to use the Helm [chart](https://github.com/GoogleCloudPlatform/spark-on-k8s-operator/blob/master/charts/spark-operator-chart).
The following commands will install the Kubernetes Operator for Apache Spark into the namespace spark-operator
```
export RELEASE_NAME=my-release
export SPARK_OPERATOR_K8S_NS=spark-operator
helm repo add spark-operator https://googlecloudplatform.github.io/spark-on-k8s-operator
helm install $RELEASE_NAME spark-operator/spark-operator --namespace $SPARK_OPERATOR_K8S_NS --create-namespace --set sparkJobNamespace=$SPARK_APPS_K8S_NS --set webhook.enable=true
```

The arguments used in the helm install command are as follows:

- `$RELEASE_NAME`: This is the name given to the release of the Helm chart. It can be any name you choose and is used 
to identify the specific instance of the application being deployed.
    
- `spark-operator/spark-operator`: This specifies the chart repository and the name of the chart to be installed. In 
this case, we are installing the Spark Operator chart from the spark-operator repository.
    
- `--namespace`: This specifies the namespace where the Spark Operator will be deployed. A namespace provides a logical 
boundary for resources and helps isolate different components and applications within a Kubernetes cluster.

- `--set sparkJobNamespace=`: This will force spark operator to look only for instructions for spark-apps namespace.
    
- `--set webhook.enable=true`: This sets the value of a specific parameter in the Helm chart. In this case, it enables 
the webhook feature of the Spark Operator. Webhooks are a mechanism for triggering actions and events based on certain conditions, allowing for more dynamic and automated behavior.

## Step 3: Verify the Installation of the Spark K8s Operator
In order to confirm that the helm chart was deployed properly, you can run the following command. If everything is 
installed correctly, you should see output similar to that shown below:

```
helm status --namespace $SPARK_OPERATOR_K8S_NS $RELEASE_NAME

NAME: my-release
LAST DEPLOYED: Sun Nov 26 11:38:19 2023
NAMESPACE: spark-operator
STATUS: deployed
REVISION: 1
TEST SUITE: None
```

You can also confirm that the K8s operator itself was deployed by checking the status of the components in the K8s 
namespace using the following command: `kubectl -n $SPARK_OPERATOR_K8S_NS get all`

The output should look similar to this:
```
NAME                                               READY   STATUS      RESTARTS        AGE
pod/my-release-spark-operator-webhook-init-khgnt   0/1     Completed   0               6m38s
pod/my-release-spark-operator-64486b7797-76nxl     1/1     Running     3 (6m19s ago)   6m34s

NAME                                        TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)   AGE
service/my-release-spark-operator-webhook   ClusterIP   10.152.183.110   <none>        443/TCP   6m34s

NAME                                        READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/my-release-spark-operator   1/1     1            1           6m34s

NAME                                                   DESIRED   CURRENT   READY   AGE
replicaset.apps/my-release-spark-operator-64486b7797   1         1         1       6m34s

NAME                                               COMPLETIONS   DURATION   AGE
job.batch/my-release-spark-operator-webhook-init   1/1           4s         6m38s
```



-------------------
References
-------------------

1. https://spark.apache.org/docs/latest/running-on-kubernetes.html
2. https://github.com/GoogleCloudPlatform/spark-on-k8s-operator/blob/master/docs/user-guide.md
3. https://medium.com/@SaphE/deploying-apache-spark-on-a-local-kubernetes-cluster-a-comprehensive-guide-d4a59c6b1204
4. https://medium.com/@erkansirin/spark-on-kubernetes-cdeee9462315