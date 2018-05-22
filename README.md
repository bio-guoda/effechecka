[![Build Status](https://travis-ci.org/bio-guoda/effechecka.svg?branch=master)](https://travis-ci.org/bio-guoda/effechecka)

# effechecka
Web API to generate taxonomic checklist, occurrence lists and to help monitor biodiversity data access.

For more information, please visit [our wiki](../../wiki).

For projects/prototypes using this api, please see http://effechecka.org or http://gimmefreshdata.github.io.

# running
## standalone
to run the effechecka webservice:
```sh run-hdfs.sh```

## marathon

In the marathon interface, create an app for the spark dispatcher and effechecka api. You can see the list of configured apps at http://mesos03.acis.ufl.edu:8080/v2/apps. The Effecheck app should look like this:

```
{
id: "/effechecka-api",
cmd: "cd effechecka-master && chmod u+x run-hdfs.sh && ./run-hdfs.sh",
args: null,
user: null,
env: {
HADOOP_PREFIX: "/usr/lib/hadoop",
HADOOP_HOME: "/usr/lib/hadoop",
HADOOP_CONF_DIR: "/etc/hadoop/conf",
HADOOP_USER_NAME: "hdfs",
HADOOP_LIBEXEC_DIR: "/usr/lib/hadoop/libexec"
},
instances: 1,
cpus: 1,
mem: 2048,
disk: 0,
executor: "",
constraints: [
[
"hostname",
"CLUSTER",
"mesos03.acis.ufl.edu"
]
],
uris: [
"https://github.com/bio-guoda/effechecka/archive/master.zip",
"https://github.com/sbt/sbt/releases/download/v0.13.15/sbt-0.13.15.zip"
],
fetch: [
{
uri: "https://github.com/bio-guoda/effechecka/archive/master.zip",
extract: true,
executable: false,
cache: false
},
{
uri: "https://github.com/sbt/sbt/releases/download/v0.13.15/sbt-0.13.15.zip",
extract: true,
executable: false,
cache: false
}
],
storeUrls: [ ],
ports: [
32000
],
requirePorts: false,
backoffSeconds: 1,
backoffFactor: 1.15,
maxLaunchDelaySeconds: 3600,
container: null,
healthChecks: [
{
path: "/checklist?taxonSelector=Aves%2CInsecta&wktString=POLYGON%20((-72.147216796875%2041.492120839687786%2C%20-72.147216796875%2043.11702412135048%2C%20-69.949951171875%2043.11702412135048%2C%20-69.949951171875%2041.492120839687786%2C%20-72.147216796875%2041.492120839687786))&limit=20",
protocol: "HTTP",
gracePeriodSeconds: 300,
intervalSeconds: 60,
timeoutSeconds: 20,
maxConsecutiveFailures: 3,
ignoreHttp1xx: false,
port: 32000
}
],
dependencies: [ ],
upgradeStrategy: {
minimumHealthCapacity: 1,
maximumOverCapacity: 1
},
labels: { },
acceptedResourceRoles: null,
ipAddress: null,
version: "2018-05-22T16:25:38.751Z",
versionInfo: {
lastScalingAt: "2018-05-22T16:25:38.751Z",
lastConfigChangeAt: "2018-05-22T16:23:00.487Z"
},
tasksStaged: 0,
tasksRunning: 1,
tasksHealthy: 1,
tasksUnhealthy: 0,
deployments: [ ]
}
```


and the spark dispatcher should look like:

````
{
id: "/spark-mesos-cluster-dispatcher",
cmd: "cd spark-2.2.0-bin-hadoop2.7 && chmod u+x bin/spark-class && echo spark.mesos.maxDrivers=1 > conf/spark-defaults.conf && echo "spark.mesos.driverEnv.HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec" >> conf/spark-defaults.conf && bin/spark-class org.apache.spark.deploy.mesos.MesosClusterDispatcher --master mesos://zk://mesos02:2181,mesos01:2181,mesos03:2181/mesos",
args: null,
user: null,
env: { },
instances: 1,
cpus: 1,
mem: 1024,
disk: 0,
executor: "",
constraints: [
[
"hostname",
"CLUSTER",
"mesos07.acis.ufl.edu"
]
],
uris: [
"http://mirror.reverse.net/pub/apache/spark/spark-2.2.0/spark-2.2.0-bin-hadoop2.7.tgz"
],
fetch: [
{
uri: "http://mirror.reverse.net/pub/apache/spark/spark-2.2.0/spark-2.2.0-bin-hadoop2.7.tgz",
extract: true,
executable: false,
cache: false
}
],
storeUrls: [ ],
ports: [
10000
],
requirePorts: false,
backoffSeconds: 1,
backoffFactor: 1.15,
maxLaunchDelaySeconds: 3600,
container: null,
healthChecks: [ ],
dependencies: [ ],
upgradeStrategy: {
minimumHealthCapacity: 1,
maximumOverCapacity: 1
},
labels: { },
acceptedResourceRoles: null,
ipAddress: null,
version: "2017-11-09T16:08:25.487Z",
versionInfo: {
lastScalingAt: "2017-11-09T16:08:25.487Z",
lastConfigChangeAt: "2017-11-09T16:08:25.487Z"
},
tasksStaged: 0,
tasksRunning: 1,
tasksHealthy: 0,
tasksUnhealthy: 0,
deployments: [ ]
}
```
