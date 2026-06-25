#!/usr/local/bin/perl  -w 


# 1.1 minimal required uses
use strict;
use warnings;
use File::Basename;
use File::Find;
use File::Slurp;
use File::Path qw(rmtree);
use File::Copy qw(move);
#use Recursive 'dircopy';
#use XML::Twig;
#use File::Copy::Recursive;
use File::Copy;
use File::Spec;
use File::Copy::Recursive qw(fcopy rcopy dircopy fmove rmove dirmove);


#This will copy including folder itself
$File::Copy::Recursive::CPRFComp = 1;

#-------------------------------------------------------------------------------
# 3. Sub functions
#-------------------------------------------------------------------------------
#-------------------------------------------------------------------------------
# 3.1 Resolve the workspace
#-------------------------------------------------------------------------------

my	$projectDirName;
my $basedirname = "C:\\ProgramData\\IBM\\iib_workdir\\PatternAppln\\";
my $integrationType; 
my $subSysCode;
my $appName;
my $funcName;
my $connType1;
my $connType2;
my $publisherType;
my $patternname;
my $applnname;
my $appldir;
my $configProjName ;
my $workspace; 
my $dlm = "\\";
my $new_path;
my $old_path;
my $reponame;
my $repository_url;
my $username = $ENV{USERNAME} ;
my $find_word;
my $replace_word;
my $MqType;
my $ndmnm;
my $ndmused;


#arrays
my @projects;
my @xmlFiles;

sub trim($)
{
	no warnings 'uninitialized';
	my $string = shift;
	$string =~ s/^\s+//;
	$string =~ s/\s+$//;
	return $string;
}

sub getUserInputs
{
	print "Enter the Integration Type (PTP/PUB/SUB/S2P/SVC)  :\n";
		$integrationType = <STDIN>;  
	print "Enter the Subsytemcode :\n";
	 $subSysCode = <STDIN>;
	print "Enter the Application code :\n";
	 $appName = <STDIN>;
	print "Enter the Functionality :\n";
	 $funcName = <STDIN>; 
	print "Enter the connectivitytype1 :\n";
	 $connType1 = <STDIN>; 
	print "Enter the connectivitytype2 (Press ENTER if no additional connectivity) :\n";
	 $connType2 = <STDIN>;

	if (uc(trim($integrationType)) eq 'SUB')
  {
	 #GET the publisher type ; its a batch or online 
		print "Enter the Publisher Type (TYPE 'PUBONLINE' OR 'PUBBATCH') :\n";
		$publisherType = <STDIN>;	
		
	#GET NDM NAME 
		print "NDM USED: (TYPE 'Y' ELSE Enter) :\n";
		$ndmused = <STDIN>;
		if (uc(trim($ndmused)) eq 'Y')
		{
			print "Enter the NDM name (REFPGN/INFUID)  :\n";
			$ndmnm = <STDIN>;
		}
				
  }
	if (uc(trim($connType1)) eq 'MQS' || uc(trim($connType2)) eq 'MQS')
		{
			 #GET the MQ type ; its a normal output queue or to mainframe  
				print "Enter the MQ Type (TYPE 'Y'  for MOM  else 'N') :\n";
				$MqType = <STDIN>;
	  }


#TRIM all user inputs
{
no warnings 'uninitialized';
$integrationType = uc(trim($integrationType));  
 $subSysCode = uc(trim($subSysCode));
 $appName = trim($appName);
 $funcName = trim($funcName); 
 $connType1 = uc(trim($connType1));
 $connType2 = uc(trim($connType2)); 
 $publisherType = uc(trim($publisherType));
 $MqType = uc(trim($MqType));
 $ndmnm = uc(trim($ndmnm));
 }
 	
# 	$integrationType = "SUB";  
# $subSysCode = "XAJ";
# $appName = "TLMTF";
# $funcName = "FINANCING"; 
# $connType1 = "MQS";
# $connType2 = "NA"; 
# 	$publisherType = "PUBONLINE";
}

sub formRepo_Pattern
{
	###
if ($connType2 eq '') 
{
	if ($MqType eq "Y") ##IF MOM MQ
	{
			if  ($integrationType eq 'SUB') {	
			$patternname = 'SUBSYS_'.($integrationType).'_APPNM_FUNCNM_'.($connType1).'_'.($publisherType).'_MOM';
			$applnname = ($subSysCode).'_'.($integrationType).'_'.($appName).'_'.($funcName).'_'.($connType1);
			
		}
		else
		{
			$patternname = 'SUBSYS_'.trim($integrationType).'_APPNM_FUNCNM_'.trim($connType1).'_MOM';
			$applnname = ($subSysCode).'_'.($integrationType).'_'.($appName).'_'.($funcName).'_'.($connType1);			
		}
	}
	else
	{
		if  ($integrationType eq 'SUB') {	
			$patternname = 'SUBSYS_'.($integrationType).'_APPNM_FUNCNM_'.($connType1).'_'.($publisherType);
			$applnname = ($subSysCode).'_'.($integrationType).'_'.($appName).'_'.($funcName).'_'.($connType1);
			
		}
		else
		{
			$patternname = 'SUBSYS_'.trim($integrationType).'_APPNM_FUNCNM_'.trim($connType1);
			$applnname = ($subSysCode).'_'.($integrationType).'_'.($appName).'_'.($funcName).'_'.($connType1);			
		}
		
	}
	
}
else
{
	if ($MqType eq "Y") ##IF MOM MQ
	{
		if  ($integrationType eq 'SUB') {	
			$patternname = 'SUBSYS_'.($integrationType).'_APPNM_FUNCNM_'.($connType1).'_'.($connType2).'_'.($publisherType).'_MOM';
			$applnname = ($subSysCode).'_'.($integrationType).'_'.($appName).'_'.($funcName).'_'.($connType1).'_'.($connType2);
			}	
		else
		{
			$patternname = 'SUBSYS_'.($integrationType).'_APPNM_FUNCNM_'.($connType1).'_'.($connType2).'_MOM';
			$applnname = ($subSysCode).'_'.($integrationType).'_'.($appName).'_'.($funcName).'_'.($connType1).'_'.($connType2);
			
		}
	}
	else
	{
		
		if  ($integrationType eq 'SUB') {	
			$patternname = 'SUBSYS_'.($integrationType).'_APPNM_FUNCNM_'.($connType1).'_'.($connType2).'_'.($publisherType);
			$applnname = ($subSysCode).'_'.($integrationType).'_'.($appName).'_'.($funcName).'_'.($connType1).'_'.($connType2);
			}	
		else
		{
			$patternname = 'SUBSYS_'.($integrationType).'_APPNM_FUNCNM_'.($connType1).'_'.($connType2);
			$applnname = ($subSysCode).'_'.($integrationType).'_'.($appName).'_'.($funcName).'_'.($connType1).'_'.($connType2);
			
		}
	
	}
}


###make directory

			$projectDirName= $basedirname.(lc($patternname));
			mkdir $projectDirName or warn "couldnt mkdir $patternname";				
			$appldir = $basedirname.lc($applnname);
}
sub GITRepo_checkout
	{
	
	my $password = '';  ---> Provide your glow password

	  
	  # Define the repository URL and the branch you want to checkout
	  $repository_url = 'https://'.lc($username).':'.($password).'@agit.kbc.be:6088/scm/ed7/'.lc($patternname).'.git'; #git clone https://agit.kbc.be:6088/scm/ed7/subsys_sub_appnm_FUNCNM_mqs_pubonline.git
	  my $branch = 'master';
	  
	   # Clone the repository into a specific directory
	  
	  my $clonecmd = "git clone $repository_url \n";
	  print("cloning url : $clonecmd ");
			system("git clone $repository_url $projectDirName") == 0
			    or die "Failed to clone repository: $!";
				    
	  
		print "Repository cloned successfully.\n";
	
}	

sub ReplaceFuncNm 
{
  if (-d $_ && $_ eq 'FUNCNM')
       { 
       		    $old_path = $File::Find::name;       		    
				$new_path = 	$old_path;		       		
				$new_path = rename('FUNCNM',trim($funcName));				
				post_rename_action ($old_path,$new_path);	
		}
}
sub  ReplaceAppNm  
{ 
	if (-d $_ && $_ eq 'APPNM')
			{
				$old_path = $File::Find::name;		
				$new_path = 	$old_path;		       		
				$new_path = rename('APPNM',trim($appName));				
				post_rename_action ($old_path,$new_path);
			}  
}		       
sub  ReplaceSubsysNm  
{ 					
	if (-d $_ && $_ eq 'SUBSYS')
			{
				$old_path = $File::Find::name;		
				$new_path = 	$old_path;		       		
				$new_path = rename('SUBSYS',trim($subSysCode));				
				post_rename_action ($old_path,$new_path);					
       		}		
}		
sub post_rename_action 
{	my ($old_path, $new_path) = @_;	
}

sub process_file
{
	return unless -f;
	my $file = $File::Find::name;	
	#Read File content
	my $content = read_file($file);
	#Repalce the word
	my $new_content = $content =~ s/\Q$find_word\E/$replace_word/gr;
	#write new content back to the  file if it was modified
	if ($content ne $new_content)
	{
		write_file($file,$new_content);
	}
	
}


sub rename_files
{
	
	 my $file= $_;
	 $old_path = $File::Find::name;		
	#skip directories#	
	#check if the  File name contains the word
	if ($file =~ /\Q$find_word\E/)
	{	
		
		my $new_file = $file;
		$new_file =~ s/\Q$find_word\E/$replace_word/g;
		$new_path = $File::Find::dir.$dlm.$new_file;	
	
	#Rename the File
	move ($old_path,$new_path) or warn "Could not rename $old_path to $new_path: $! \n";
	#print "Renamed $old_path to $new_path: $! \n";
	}
}



#-------------------------------------------------------------------------------
# 5. Main program
#-------------------------------------------------------------------------------
main:
$| = 1;
##Collect details from User
getUserInputs();

 	
# Form THE appln Pattern and make directory
formRepo_Pattern();

##CHECKOUT GIT REPO AND CLONE
GITRepo_checkout();

## REMOVE THE >GIT
rmtree ($projectDirName.$dlm.'.git');

###RENAME REPO NAME
rename ($projectDirName,$appldir)	or die ("Support Pattern $patternname is not checked out \n\n ");

###RENAME APPLICAION NAME
my $patternnameis =  $appldir.$dlm.trim($patternname);
my $patternnametobe =  $appldir.$dlm.trim($applnname);

my $patternnameis_con =  $appldir.$dlm.trim($patternname).'_Configs';
my $patternnametobe_con =  $appldir.$dlm.trim($applnname).'_Configs';
rename($patternnameis , $patternnametobe) or die ("Couldnt rename $patternnameis \n\n ");

###RENAME APPLICAION_CONFIG NAME
rename($patternnameis_con,$patternnametobe_con) or die ("Couldnt rename $patternnametobe_con \n\n");

###RENAME FOLDER NAMES
{
no warnings;
find(\&ReplaceFuncNm,$patternnametobe);
find(\&ReplaceAppNm,$patternnametobe) ;
find(\&ReplaceSubsysNm,$patternnametobe)  ;
}

#rename SUBSYS in filenames
#$find_word = 'SUBSYS';
#$replace_word = $subSysCode;
#find(\&rename_files, $appldir);
#
## rename APPNAME inside files
#$find_word = 'APPNM';
#$replace_word = $appName;
#find(\&rename_files, $appldir);
#
## rename FUNCNAME in files
#$find_word = 'FUNCNM';
#$replace_word = $funcName;
#find(\&rename_files, $appldir);

## rename SUBSYS inside files
$find_word = 'SUBSYS';
$replace_word = $subSysCode;
find(\&process_file,$appldir);

## rename APPNAME inside files
$find_word = 'APPNM';
$replace_word = $appName;
find(\&process_file,$appldir);

## rename FUNCNAME inside files
$find_word = 'FUNCNM';
$replace_word = $funcName;
find(\&process_file,$appldir);

## rename NDMNAME inside files
$find_word = 'NDMNM';
$replace_word = $ndmnm;
find(\&process_file,$appldir);


print ("\nDone !!!!!!\n");
print ("\nImport the application into toolkit from dir \n $appldir \n");
 #git config --global credential.helper store // if you want to store the credentials for ever (considered unsafe);
 #print "\nDone !!!!!!\n";
 