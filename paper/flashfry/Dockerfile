FROM centos:7

RUN mkdir opencl
RUN su - && yum -y install sudo deltarpm
RUN sudo yum -y update
RUN sudo yum -y install wget gcc redhat-lsb-core numactl-devel curl-devel libxml2-devel emacs
RUN sudo yum -y install dkms "kernel-devel-uname-r == 3.10.0-693.21.1.el7.x86_64" java-1.8.0-openjdk

RUN sudo rpm --import "http://keyserver.ubuntu.com/pks/lookup?op=get&search=0x3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF"
RUN su -c 'curl https://download.mono-project.com/repo/centos7-stable.repo | tee /etc/yum.repos.d/mono-centos7-stable.repo'
RUN sudo yum install -y mono-devel

RUN cd opencl && wget http://registrationcenter-download.intel.com/akdlm/irc_nas/12556/opencl_runtime_16.1.2_x64_rh_6.4.0.37.tgz
RUN cd opencl && tar xf opencl_runtime_16.1.2_x64_rh_6.4.0.37.tgz
RUN cd opencl/opencl_runtime_16.1.2_x64_rh_6.4.0.37 && wget https://gist.githubusercontent.com/aaronmck/b4bef23cce0c20390567a7774c475312/raw/eddfb35c857a354f2c1a851acfa567fd9331bea4/silent_install.cfg


RUN cd opencl/opencl_runtime_16.1.2_x64_rh_6.4.0.37 && bash install.sh -s silent_install.cfg
RUN cd opencl && wget http://registrationcenter-download.intel.com/akdlm/irc_nas/vcp/12526/intel_sdk_for_opencl_2017_7.0.0.2568_x64.gz
RUN cd opencl && tar xf intel_sdk_for_opencl_2017_7.0.0.2568_x64.gz
RUN cd opencl/intel_sdk_for_opencl_2017_7.0.0.2568_x64 && wget https://gist.githubusercontent.com/aaronmck/b4bef23cce0c20390567a7774c475312/raw/eddfb35c857a354f2c1a851acfa567fd9331bea4/silent_install.cfg

RUN cd opencl/intel_sdk_for_opencl_2017_7.0.0.2568_x64 && sudo bash install.sh -s silent_install.cfg

# get the Cas-off software
RUN sudo mkdir /cas-off && sudo chmod 755 /cas-off
RUN cd /cas-off && sudo wget -O cas-off "https://downloads.sourceforge.net/project/cas-offinder/Binaries/2.4/Linux64/cas-offinder?r=https%3A%2F%2Fsourceforge.net%2Fprojects%2Fcas-offinder%2Ff"
RUN sudo chmod 755 /cas-off/cas-off

# setup aws tools
RUN cd /tmp && wget https://www.python.org/ftp/python/2.7.11/Python-2.7.11.tgz
RUN cd /tmp && tar -zxf Python-2.7.11.tgz
RUN cd /tmp/Python-2.7.11 && sudo ./configure --prefix=/opt/
RUN cd /tmp/Python-2.7.11 && sudo make && sudo make install
RUN cd /tmp/ && sudo wget -O get-pip.py https://bootstrap.pypa.io/get-pip.py
RUN cd /tmp && sudo python get-pip.py
RUN cd /tmp && sudo sudo pip install awscli --upgrade


ENV AWS_ACCESS_KEY_ID=
ENV AWS_SECRET_ACCESS_KEY=


# copy genome information over to the machine
RUN sudo mkdir /genome && sudo chmod 777 /genome
RUN cd /genome && aws s3 cp s3://flashfrydist/hg38.header ./
RUN cd /genome && aws s3 cp s3://flashfrydist/hg38 ./
RUN cd /genome && aws s3 cp s3://flashfrydist/hg38.fa.amb ./
RUN cd /genome && aws s3 cp s3://flashfrydist/hg38.fa.ann ./
RUN cd /genome && aws s3 cp s3://flashfrydist/hg38.fa.bwt ./
RUN cd /genome && aws s3 cp s3://flashfrydist/hg38.fa.pac ./
RUN cd /genome && aws s3 cp s3://flashfrydist/hg38.fa.sa ./
RUN cd /genome && aws s3 cp s3://flashfrydist/hg38.fa ./

# get R and CRISPRseek setup
RUN sudo yum -y install epel-release git openssl-devel mysql-devel
RUN sudo yum update -y
RUN sudo yum -y install R
RUN sudo mkdir /CRISPRseek
RUN cd /CRISPRseek && sudo wget -O setup.R "https://gist.githubusercontent.com/aaronmck/9b1fb9bf2c033fa3c4e3ea63ba5158c8/raw/5282fdc069d3686660ccc6f830aebb8ac5c08402/setup.R"
RUN cd /CRISPRseek && sudo R CMD BATCH setup.R

# setup FlashFry
RUN sudo mkdir /FlashFry
RUN cd /FlashFry && sudo wget -O flashfry.jar "https://github.com/aaronmck/FlashFry/releases/download/1.8.1/FlashFry-assembly-1.8.1.jar"

# add bwa
RUN sudo yum -y install bwa

# now setup docker on the machine
RUN sudo yum install -y docker

# install the CWL runner
RUN sudo pip install cwl-runner

RUN sudo git clone https://github.com/aaronmck/FlashFry.git /FlashFry_GIT

RUN sudo yum install -y nodejs

RUN cd /genome && aws s3 cp s3://flashfrydist/hg38.chromFa.tar.gz ./
RUN cd /genome && tar xf hg38.chromFa.tar.gz
