package OrthoMCLModel::MapProteomeService::ProteomeJobMgr;

use strict;
use FgpUtil::Util::SshComputeCluster;
use DJob::DistribJob::DistribJobUtils;
use POSIX qw(strftime);
use Carp;

sub new {
  my ($class, $serverConfigFile, $clusterConfigFile) = @_;

  my $self = {};
  bless($self,$class);
  $self->{serverConfigFile} = $serverConfigFile;
  $self->{clusterConfigFile} = $clusterConfigFile;
  return $self;
}

sub getConfig {
  my ($self, $tag) = @_;

  if (!$self->{config}) {
    $self->readConfigFile($self->{serverConfigFile});
    $self->readConfigFile($self->{clusterConfigFile});
  }
  $self->error("Config files do not have a property '$tag'") unless defined($self->{config}->{$tag});
  return $self->{config}->{$tag};
}

sub readConfigFile {
  my ($self, $file) = @_;

  open(F, $file) || $self->error("Can't open config file '$file'\n");

  while (<F>) {
    chomp;
    s/\s+$//;
    next if /^\#/ or /^$/;
    /^(\w+)\=(.+)/ || die "illegal line '$_' in config file '$file'\n";
    my $key=$1;
    my $val=$2;
    $self->{config}->{$key} = $val;
  }

}

sub getTransferServer {
    my ($self) = @_;

    if (!$self->{transferServer}) {
      my $transferServer = $self->getConfig('transferServer');
      my $clusterUser = $self->getConfig('clusterUserName');
      $self->{transferServer} = FgpUtil::Util::SshComputeCluster->new($transferServer,
							       $clusterUser,
							       $self);
    }
    return $self->{transferServer};
}

sub getNodeClass {
  my ($self) = @_;

  if (!$self->{nodeClass}) {
    # use node object static method to find the cmd to submit to a node (eg "bsub -Is")
    my $nodeClass = $self->getConfig('nodeClass');
    my $nodePath = $nodeClass;
    $nodePath =~ s/::/\//g;       # work around perl 'require' weirdness
    require "$nodePath.pm";
    $self->{nodeClass} = $nodeClass;
  }
  return $self->{nodeClass};
}


#include unused $testmode arg for compatibility w/ workflow code (SshCluster)
sub runCmd {
    my ($self, $testmode, $cmd, $allowFailure) = @_;

    $self->log("Running cmd:\n$cmd\n");
    my $output = `$cmd`;
    my $status = $? >> 8;
    if ($status) {
      if ($allowFailure) { $self->logErr("WARNING: command failed, but we don't die for this type of failure:\n$cmd\n\n"); }
      else { $self->error("Failed with status $status running: \n$cmd"); }
    }

    return $output;
}

sub retryCmd {
    my ($self, $cmd, $wait) = @_;

      $self->logErr("Failed running: $cmd\nWill retry in $wait seconds.");
      sleep($wait);
      return $self->runCmd(0, $cmd, 1);
}

sub runCmdInBackground {
  my ($self, $cmd) = @_;

  $self->log("Running background cmd:\n$cmd\n");

  my $pid = fork();

  if (not defined $pid) {
    $self->log("Failed fork for background cmd:\n$cmd\n");
  } elsif ($pid == 0) {
    my $status = system("$cmd");
    warn "Failed running background cmd $cmd' with stderr:\n $!" if ($status);
    exit($status);
  } else {
    # parent proceeds without waiting
  }
}


sub error {
    my ($self, $msg) = @_;
    confess($self->formatLocalTime(localtime) . "\t$msg\n\n");
}

sub log {
  my ($self, $msg) = @_;
  print STDERR $self->formatLocalTime(localtime) . "\t$msg\n";
}

# for compatibility with DistribjobUtils
sub logErr {
  my ($self, $msg) = @_;
  $self->log($msg);
}

sub formatLocalTime {
  my ($self, @localtime) = @_;
  return strftime('%Y-%m-%d %H:%M:%S', @localtime);
}

sub runClusterTask {
  my ($self, $user, $rcfile, $submitServer, $transferServer, $jobInfoFile, $logFile, $controllerPropFile, $numNodes, $time, $queue, $ppn, $maxMemoryGigs) = @_;

  my $rcfile = $self->getConfig('rcfile');

  DJob::DistribJob::DistribJobUtils::submitDistribJobToQueue($self, $user, $submitServer, $transferServer, $jobInfoFile, $logFile, $controllerPropFile, $numNodes, $time, $queue, $ppn, $maxMemoryGigs, $rcfile);
}

sub clusterTaskRunning {
  my ($self, $jobInfoFile, $user, $submitServer, $transferServer, $nodeClass) = @_;

  return DJob::DistribJob::DistribJobUtils::distribJobRunning($self, $jobInfoFile, $user, $submitServer, $transferServer, $self->getNodeClass());
}

sub runSshCmdWithRetries {
    my ($self, $test, $cmd, $optionalMsgForErr, $allowFailure, $doNotLog, $user, $server, $redirect) = @_;

    my $sshCmd = "ssh -2 $user\@$server '$cmd' $redirect";

    my ($output, $status) = $self->runCmd($test, $sshCmd, 1);
    ($output, $status) = $self->retryCmd($sshCmd, 30) if ($status);
    ($output, $status) = $self->retryCmd($sshCmd, 60) if ($status);

    if ($status) {
      my $msg = "Retries didn't work.  Giving up.";
      $allowFailure? $self->logErr($msg) : $self->error($msg);
    }
    return $output;
}



1;
