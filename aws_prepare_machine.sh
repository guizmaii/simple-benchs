curl -s "https://get.sdkman.io" | bash
source "/home/ec2-user/.sdkman/bin/sdkman-init.sh"
sdk install java 8.0.232.hs-adpt
sdk install sbt
sdk install maven

# -------------------------- #
# BEGIN ---- Schema Registry #
# -------------------------- #
# https://docs.confluent.io/current/installation/installing_cp/rhel-centos.html#get-the-software
# https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/add-repositories.html

sudo rpm --import https://packages.confluent.io/rpm/5.3/archive.key

cat <<EOF > confluent.repo
[Confluent.dist]
name=Confluent repository (dist)
baseurl=https://packages.confluent.io/rpm/5.3/7
gpgcheck=1
gpgkey=https://packages.confluent.io/rpm/5.3/archive.key
enabled=1

[Confluent]
name=Confluent repository
baseurl=https://packages.confluent.io/rpm/5.3
gpgcheck=1
gpgkey=https://packages.confluent.io/rpm/5.3/archive.key
enabled=1
EOF

sudo yum-config-manager --add-repo confluent.repo

sudo yum clean all
sudo yum install confluent-platform-2.12 -y

# ------------------------ #
# END ---- Schema Registry #
# ------------------------ #
