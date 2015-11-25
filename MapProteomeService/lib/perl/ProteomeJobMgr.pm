package OrthoMCLShared::MapProteomeService::ProteomeJobMgr;

use strict;
use CBIL::Util::SshCluster;
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

sub getCluster {
    my ($self) = @_;

    if (!$self->{cluster}) {
    my $clusterServer = $self->getConfig('clusterServer');
    my $clusterUser = $self->getConfig('clusterUserName');
    $self->{cluster} = CBIL::Util::SshCluster->new($clusterServer,
                              $clusterUser,
                              $self);
    }
    return $self->{cluster};
}

#include unused $testmode arg for compatibility w/ workflow code (SshCluster)
sub runCmd {
    my ($self, $testmode, $cmd) = @_;

    $self->log("Running cmd:\n$cmd\n");
    my $output = `$cmd`;
    my $status = $? >> 8;
    $self->error("Failed with status $status running: \n$cmd") if ($status);
    return $output;
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

sub formatLocalTime {
  my ($self, @localtime) = @_;
  return strftime('%Y-%m-%d %H:%M:%S', @localtime);
}

sub runClusterTask {
    my ($self, $user, $rcfile, $server, $processIdFile, $logFile, $controllerPropFile, $numNodes, $time, $queue, $ppn, $maxMem) = @_;

    # if not already started, start it up (otherwise the local process was restarted)
    if (!$self->clusterTaskRunning($processIdFile, $user, $server)) {
        my $cmd = "workflowRunDistribJob $controllerPropFile $logFile $processIdFile $numNodes $time $queue $ppn $maxMem";
        my $sshCmd = "ssh -q -2 $user\@$server '/bin/bash -rcfile $rcfile -i -c \"$cmd\"'";
        $self->runCmdInBackground($sshCmd);
    }
}

sub clusterTaskRunning {
    my ($self, $processIdFile, $user, $server) = @_;

    my $processId = `ssh -q -2 $user\@$server 'if [ -a $processIdFile ];then cat $processIdFile; fi'`;

    chomp $processId;

    my $status = 0;
    if ($processId) {
      system("ssh -q -2 $user\@$server 'ps -p $processId > /dev/null'");
      $status = $? >> 8;
      $status = !$status;
    }
    return $status;
}

1;
