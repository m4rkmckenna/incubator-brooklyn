Instructions for running examples
=================================

The commands below assume that the `brooklyn` script is already on your $PATH, and you are in the `examples` directory:

    cd simple-nosql-cluster
    export BROOKLYN_CLASSPATH=$(pwd)/target/classes
    
    # Launches a Redis cluster on AWS EC2
    brooklyn -v launch --app org.apache.brooklyn.demo.SimpleRedisCluster --location aws-ec2:eu-west-1
    
    # Launches a Cassandra cluster on AWS EC2
    brooklyn -v launch --app org.apache.brooklyn.demo.SimpleCassandraCluster --location aws-ec2:eu-west-1
    
    # Launches a CouchDB cluster on AWS EC2
    brooklyn -v launch --app org.apache.brooklyn.demo.SimpleCouchDBCluster --location aws-ec2:eu-west-1
    
    # Launches a CumulusRDF application backed by a cassandra cluster on AWS EC2
    brooklyn -v launch --app org.apache.brooklyn.demo.CumulusRDFApplication --location aws-ec2:eu-west-1

--------

For more information please visit https://brooklyn.incubator.apache.org/.

----
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.