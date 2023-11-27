# Getting Started with Pulsar and Spark on Kubernetes

This repository contains a collection of instructions the guide you through the process of installing Spark inside 
your K8s environment, developing applications that use both Apache Spark, and Apache Pulsar, and deploying the 
applications inside a K8s environment.

------------
Requirements
------------
- kubectl (v1.16 or higher), compatible with your cluster (+/- 1 minor release from your cluster).
- Helm (v3.0.2 or higher).
- Kubernetes cluster (v1.24 or higher).

Ensure you have allocated enough resources to Kubernetes: at least 16Gb.

---
Application Development Guides
---
- Spark on K8s installation [guide](spark-operator/README.md).
- Spark & Pulsar application development [guide](development/README.md).
- Application deployment [guide](deployment/README.md).