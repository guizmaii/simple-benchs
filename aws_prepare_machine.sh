curl -s "https://get.sdkman.io" | bash
source "/home/ec2-user/.sdkman/bin/sdkman-init.sh"
sdk install java 8.0.232.hs-adpt
sdk install sbt
sdk install maven

sudo yum clean all
sudo yum install confluent-platform-2.12 -y
