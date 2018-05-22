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

In the marathon interface, create an app that looks like this:

'''
ID
/effechecka-api
Command
cd effechecka-master && chmod u+x run-hdfs.sh && ./run-hdfs.sh
Constraints
hostname:CLUSTER:mesos03.acis.ufl.edu
Dependencies
Unspecified
Labels
Unspecified
Resource Roles
Unspecified
Container
Unspecified
CPUs
1
Environment
HADOOP_PREFIX=/usr/lib/hadoop
HADOOP_HOME=/usr/lib/hadoop
HADOOP_CONF_DIR=/etc/hadoop/conf
HADOOP_USER_NAME=hdfs
HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec
Executor
Unspecified
Health Checks
[
  {
    "path": "/checklist?taxonSelector=Aves%2CInsecta&wktString=POLYGON%20((-72.147216796875%2041.492120839687786%2C%20-72.147216796875%2043.11702412135048%2C%20-69.949951171875%2043.11702412135048%2C%20-69.949951171875%2041.492120839687786%2C%20-72.147216796875%2041.492120839687786))&limit=20",
    "protocol": "HTTP",
    "gracePeriodSeconds": 300,
    "intervalSeconds": 60,
    "timeoutSeconds": 20,
    "maxConsecutiveFailures": 3,
    "ignoreHttp1xx": false,
    "port": 32000
  }
]
Instances
1
IP Address
Unspecified
Memory
2048 MiB
Disk Space
0 MiB
Ports
32000
Backoff Factor
1.15
Backoff
1 seconds
Max Launch Delay
3600 seconds
URIs
https://github.com/bio-guoda/effechecka/archive/master.zip
https://github.com/sbt/sbt/releases/download/v0.13.15/sbt-0.13.15.zip
User
Unspecified
Version
2018-05-22T16:25:38.751Z
'''



