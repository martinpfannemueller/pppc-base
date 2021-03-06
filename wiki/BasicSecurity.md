#Security

#Introduction

The following sections contain some documentation on the secure middleware. Specifically, it describes how to generate certificates and how to use them in your programs.

##Security in BASE

Using the Bouncycastle library, BASE is able to secure communication thereby providing authenticity, integrity and secrecy. Following its micro-broker architecture, BASE does this via a minimal security core which can be found in the *base-core-security* project and an extensible set of plug-ins which are contained in the *base-plugin-security* project. The security core manages keys and certificates that are exchanged and used by plug-ins. To automate key exchange, BASE makes use of asymmetric cryptography. This requires each device to be equipped with an asymmetric key pair that is either self-signed or signed by some other key pair. Using certificate hierarchies it is possible to define trust relations between different (sets of) devices. This can be done by configuring trusted certificates for each device using its own key store.

If a device is equipped with a key store and the necessary plug-ins, developers can enforce secure communication at the client- and the server side. To do this in a convenient way, the BASE Eclipse plug-in can generate secure variants of proxies and skeletons. Using a secure skeleton, a service can verify that an incoming call is secure and authentic and that it has been issued from a device with a particular trust level. Similarly, by making a call through a secure proxy, a developer can ensure that the targeted device is authentic and that the call is encrypted.

An example on how this works can be found in the repository. Just have a look at the *base-tutorial-security* project. As usual, security complicates application development since it is necessary to perform some initial configuration. Specifically, it is necessary to generate and distribute certificates according to goals of the application. The generation of certificates and keys can be done using [http://www.openssl.org/ OpenSSL] which results in the following overall approach.

##Overall Approach

 * In order to use authentication and encryption you need to configure the middleware with a certificate. To do this:
  * Generate an asymmetric key pair using OpenSSL and sign it with some key to create a certificate for the public key.
  * As a first call in your program, create a certificate provider and pass it to the initialize method of the key store. 
 * Furthermore, you must configure each instance of the middleware with the appropriate set of plugins. To do this:
  * Create an instance of the exchange semantic plug-in and pass it to the plug-in manager. Using the certificates, the exchange semantic is able to automatically create a session key when needed.
  * Create an instance of the secure modifier plug-in and pass it to the plug-in manager. Using the key established by the exchange semantic, the secure modifier is able to encrypt and authenticate data streams.
  * Note: If you miss one of the steps above, the middleware will not be able to establish a secure connection. This will result in "could not connect" messages when another semantic tries to open a connection.
 * In addition to automatic key exchange, you can also configure devices with pair-wise keys. To do this, call the appropriate set key methods on the key store. When setting the keys, you need to ensure that each device is equipped with the same key for a particular pair of devices. Failure to do so will result in "could not connect" messages. By setting keys manually, you can avoid the cost of automatic key establishment at the price of a higher complexity with respect to configuration. However, you will not be able to avoid the higher latency introduced by encryption performed by the secure modifier. If you use manual key distribution for all relevant device pairs, you can omit the installation of the key exchange semantic.

##Key Generation

To generate keys, you can use [http://www.openssl.org/ OpenSSL]. In the following, we outline the overall process and provide configurations and scripts to simplify it. The scripts use a specific ECC curve due to the fact that the SunSPOT SDK provides an extremely efficient implementation for it. This implementation can be activated using the *base-extension-fastecc* project. However, one of the libraries required for this to work is licensed under GPL V2 - so if you decide to use it, you are bound to its license terms. 

###Certificate Authority

 * You can use OpenSSL to create a certificate authority (we tested the current version, 0.9.8o)
 * To create a root certification authority certificate.
  * Copy the openssl.cfg file below and edit it.
   * Especially focus on the [ req_distinguished_name ] section (create your own defaults, this eases certificate creation).
   * You can also edit other attributes like the "default_days".
  * Use the createca.cmd file to create an ECC certificate authority.
   * This will create a self-signed CA certificate.
   * We currently use the ECC curve secp160r1 (this is the one supported by the fastecc extensions, but you can also use other ECC curves or RSA certificates - however, they are much slower).
  * Only distribute the certification authority certificate to the devices, do not put the private key there.
  * To create certificates for the devices itself, please read the following section.

###Device Certificates
 * You can use OpenSSL to create a certificate (we tested the current version, 0.9.8o)
 * First generate the certificate request
  * openssl req -nodes -new -x509 -keyout client-req.pem -out client-req.pem -days 365 -config openssl.cfg -newkey ec:ec_param.pem
   * you can change the validity by changing the -days parameter
  * openssl x509 -x509toreq -in client-req.pem -signkey client-req.pem -out client-tmp.pem
 * Then sign the created request with your root certification authority certificate (note that this requires the authority's password)
  * openssl ca -config openssl.cfg -out client-cert.pem -infiles client-tmp.pem
 * Do some format conversion
  * openssl pkcs12 -export -in client-cert.pem -out client-cert.p12 -inkey client-req.pem -descert
   * Creates the certificate in the PKC12 format (useful for Windows; Optional step)
  * openssl x509 -in client-cert.pem -inform PEM -out client-cert.der -outform DER
   * Changes the certificate format from PEM to DER (needed for BASE)
  * openssl ec -in client-req.pem -inform PEM -out client-priv.der -outform DER
   * Changes the format of the private key from PEM to DER (needed for BASE)
 * Tidy up
  * del client-req.pem
  * del client-tmp.pem
  * copy client-req.pem.priv private\client.priv
 * You can also use the createdevice.cmd file below which automates these steps.
   
###Createca.cmd 
```
@echo off
echo This script creates a new certificate authority
echo including a root CA certificate in the current directory.
echo.
echo Please put a edited ^"openssl.cfg^" in this directory
echo before running this script!
echo.
echo Never run this script in a directory where a CA is already
echo established!
echo.
set createCA=
SET /P createCA="Do you really want to create a new certificate authority [Y/N]?"
:REM Windows Vista and 7 would allow to use "choice" (again), Windows XP does not allow this!
if /i not %createCA%==Y goto ende

mkdir certs private newcerts crl
type nul > index.txt
:REM creates the file index.txt (similar to touch, but sets it to 0 bytes)
echo 01 > serial
echo.
echo Creating EC parameters, this is a one-time process.
echo.
openssl ecparam -out ec_param.pem -name secp160r1
echo.
echo Trying to create the CA certificate, you then have to choose a
echo password for your CA according to your CA policy
echo (configured in the file openssl.cfg).
echo This password will later be used to sign certificate requests,
echo so please remember it!
echo.
openssl req -new -x509 -keyout private\cakey.pem -out cacert.pem -days 3650 -config openssl.cfg -newkey ec:ec_param.pem
openssl pkcs12 -export -in cacert.pem -inkey private\cakey.pem -out caroot.p12 -cacerts -descert
openssl x509 -in cacert.pem -inform PEM -out certs\cacert.der -outform DER
:ende
```

###Createdevice.cmd
```
@echo off
if "%~1"=="" goto parameter_wrong
if [%1]==[help] goto parameter_wrong
openssl req -nodes -new -x509 -keyout client%1-req.pem -out client%1-req.pem -days 1095 -config openssl.cfg -newkey ec:ec_param.pem

openssl x509 -x509toreq -in client%1-req.pem -signkey client%1-req.pem -out client%1-tmp.pem

openssl ca -config openssl.cfg -out client%1-cert.pem -infiles client%1-tmp.pem

openssl pkcs12 -export -in client%1-cert.pem -out client%1-cert.p12 -inkey client%1-req.pem -descert

openssl x509 -in client%1-cert.pem -inform PEM -out client%1-cert.der -outform DER

openssl ec -in client%1-req.pem -inform PEM -out client%1-priv.der -outform DER

del client%1-req.pem
del client%1-tmp.pem
copy client%1-priv.der private\%1.priv
copy client%1-cert.der newcerts\%1.der

echo Created a certificate for %1
echo File names are:
echo Certificate: client%1-cert.der
echo Private Key: client%1-priv.der

goto ende
:parameter_wrong
echo Syntax: createClient ^<client name or number^>
echo Example: "createClient 2"
goto ende
:ende
```

###Openssl.cfg

```
#
# OpenSSL example configuration file.
# This is mostly being used for generation of certificate requests.
#

# This definition stops the following lines choking if HOME isn't
# defined.
HOME			= .
RANDFILE		= $ENV::HOME/.rnd

# Extra OBJECT IDENTIFIER info:
#oid_file		= $ENV::HOME/.oid
oid_section		= new_oids

# To use this configuration file with the "-extfile" option of the
# "openssl x509" utility, name here the section containing the
# X.509v3 extensions to use:
# extensions		= 
# (Alternatively, use a configuration file that has only
# X.509v3 extensions in its main [= default] section.)

[ new_oids ]

# We can add new OIDs in here for use by 'ca' and 'req'.
# Add a simple OID like this:
# testoid1=1.2.3.4
# Or use config file substitution like this:
# testoid2=${testoid1}.5.6

####################################################################
[ ca ]
default_ca	= CA_default		# The default ca section

####################################################################
[ CA_default ]

dir		= ./			# Where everything is kept
certs		= $dir/certs		# Where the issued certs are kept
crl_dir		= $dir/crl		# Where the issued crl are kept
database	= $dir/index.txt	# database index file.
#unique_subject	= no			# Set to 'no' to allow creation of
					# several ctificates with same subject.
new_certs_dir	= $dir/newcerts		# default place for new certs.

certificate	= $dir/cacert.pem 	# The CA certificate
serial		= $dir/serial 		# The current serial number
crlnumber	= $dir/crlnumber	# the current crl number
					# must be commented out to leave a V1 CRL
crl		= $dir/crl.pem 		# The current CRL
private_key	= $dir/private/cakey.pem# The private key
RANDFILE	= $dir/private/.rand	# private random number file

x509_extensions	= usr_cert		# The extentions to add to the cert

# Comment out the following two lines for the "traditional"
# (and highly broken) format.
name_opt 	= ca_default		# Subject Name options
cert_opt 	= ca_default		# Certificate field options

# Extension copying option: use with caution.
# copy_extensions = copy

# Extensions to add to a CRL. Note: Netscape communicator chokes on V2 CRLs
# so this is commented out by default to leave a V1 CRL.
# crlnumber must also be commented out to leave a V1 CRL.
# crl_extensions	= crl_ext

default_days	= 1095			# how long to certify for
default_crl_days= 30			# how long before next CRL
default_md	= sha1			# which md to use.
preserve	= no			# keep passed DN ordering

# A few difference way of specifying how similar the request should look
# For type CA, the listed attributes must be the same, and the optional
# and supplied fields are just that :-)
policy		= policy_match

# For the CA policy
[ policy_match ]
countryName		= match
stateOrProvinceName	= match
organizationName	= match
organizationalUnitName	= optional
commonName		= supplied
emailAddress		= optional

# For the 'anything' policy
# At this point in time, you must list all acceptable 'object'
# types.
[ policy_anything ]
countryName		= optional
stateOrProvinceName	= optional
localityName		= optional
organizationName	= optional
organizationalUnitName	= optional
commonName		= supplied
emailAddress		= optional

####################################################################
[ req ]
default_bits		= 1024
default_keyfile 	= privkey.pem
distinguished_name	= req_distinguished_name
attributes		= req_attributes
x509_extensions	= v3_ca	# The extentions to add to the self signed cert

# Passwords for private keys if not present they will be prompted for
# input_password = secret
# output_password = secret

# This sets a mask for permitted string types. There are several options. 
# default: PrintableString, T61String, BMPString.
# pkix	 : PrintableString, BMPString.
# utf8only: only UTF8Strings.
# nombstr : PrintableString, T61String (no BMPStrings or UTF8Strings).
# MASK:XXXX a literal mask value.
# WARNING: current versions of Netscape crash on BMPStrings or UTF8Strings
# so use this option with caution!
string_mask = nombstr

# req_extensions = v3_req # The extensions to add to a certificate request

[ req_distinguished_name ]
countryName			= Country Name (2 letter code)
countryName_default		= DE
countryName_min			= 2
countryName_max			= 2

stateOrProvinceName		= State or Province Name (full name)
stateOrProvinceName_default	= North-Rhine Westphalia

localityName			= Locality Name (eg, city)
localityName_default		= Duisburg

0.organizationName		= Organization Name (eg, company)
0.organizationName_default	= University of Duisburg-Essen

# we can do this but it is not needed normally :-)
#1.organizationName		= Second Organization Name (eg, company)
#1.organizationName_default	= World Wide Web Pty Ltd

organizationalUnitName		= Organizational Unit Name (eg, section)
organizationalUnitName_default	= yourUnit

commonName			= Common Name (eg, Client 1)
commonName_max			= 64

emailAddress			= Email Address
emailAddress_max		= 64
emailAddress_default		= your_e-mail@example.com

# SET-ex3			= SET extension number 3

[ req_attributes ]
challengePassword		= A challenge password
challengePassword_min		= 4
challengePassword_max		= 20

unstructuredName		= An optional company name

[ usr_cert ]

# These extensions are added when 'ca' signs a request.

# This goes against PKIX guidelines but some CAs do it and some software
# requires this to avoid interpreting an end user certificate as a CA.

basicConstraints=CA:FALSE

# Here are some examples of the usage of nsCertType. If it is omitted
# the certificate can be used for anything *except* object signing.

# This is OK for an SSL server.
# nsCertType			= server

# For an object signing certificate this would be used.
# nsCertType = objsign

# For normal client use this is typical
# nsCertType = client, email

# and for everything including object signing:
# nsCertType = client, email, objsign

# This is typical in keyUsage for a client certificate.
# keyUsage = nonRepudiation, digitalSignature, keyEncipherment

# This will be displayed in Netscape's comment listbox.
nsComment			= "BASE project"

# PKIX recommendations harmless if included in all certificates.
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid,issuer

# This stuff is for subjectAltName and issuerAltname.
# Import the email address.
# subjectAltName=email:copy
# An alternative to produce certificates that aren't
# deprecated according to PKIX.
# subjectAltName=email:move

# Copy subject details
# issuerAltName=issuer:copy

#nsCaRevocationUrl		= http://www.domain.dom/ca-crl.pem
#nsBaseUrl
#nsRevocationUrl
#nsRenewalUrl
#nsCaPolicyUrl
#nsSslServerName

[ v3_req ]

# Extensions to add to a certificate request

basicConstraints = CA:FALSE
keyUsage = nonRepudiation, digitalSignature, keyEncipherment

[ v3_ca ]


# Extensions for a typical CA


# PKIX recommendation.

subjectKeyIdentifier=hash

authorityKeyIdentifier=keyid:always,issuer:always

# This is what PKIX recommends but some broken software chokes on critical
# extensions.
#basicConstraints = critical,CA:true
# So we do this instead.
basicConstraints = CA:true

# Key usage: this is typical for a CA certificate. However since it will
# prevent it being used as an test self-signed certificate it is best
# left out by default.
# keyUsage = cRLSign, keyCertSign

# Some might want this also
# nsCertType = sslCA, emailCA

# Include email address in subject alt name: another PKIX recommendation
# subjectAltName=email:copy
# Copy issuer details
# issuerAltName=issuer:copy

# DER hex encoding of an extension: beware experts only!
# obj=DER:02:03
# Where 'obj' is a standard or added object
# You can even override a supported extension:
# basicConstraints= critical, DER:30:03:01:01:FF

[ crl_ext ]

# CRL extensions.
# Only issuerAltName and authorityKeyIdentifier make any sense in a CRL.

# issuerAltName=issuer:copy
authorityKeyIdentifier=keyid:always,issuer:always

[ proxy_cert_ext ]
# These extensions should be added when creating a proxy certificate

# This goes against PKIX guidelines but some CAs do it and some software
# requires this to avoid interpreting an end user certificate as a CA.

basicConstraints=CA:FALSE

# Here are some examples of the usage of nsCertType. If it is omitted
# the certificate can be used for anything *except* object signing.

# This is OK for an SSL server.
# nsCertType			= server

# For an object signing certificate this would be used.
# nsCertType = objsign

# For normal client use this is typical
# nsCertType = client, email

# and for everything including object signing:
# nsCertType = client, email, objsign

# This is typical in keyUsage for a client certificate.
# keyUsage = nonRepudiation, digitalSignature, keyEncipherment

# This will be displayed in Netscape's comment listbox.
nsComment			= "d Certificate"

# PKIX recommendations harmless if included in all certificates.
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid,issuer:always

# This stuff is for subjectAltName and issuerAltname.
# Import the email address.
# subjectAltName=email:copy
# An alternative to produce certificates that aren't
# deprecated according to PKIX.
# subjectAltName=email:move

# Copy subject details
# issuerAltName=issuer:copy

#nsCaRevocationUrl		= http://www.domain.dom/ca-crl.pem
#nsBaseUrl
#nsRevocationUrl
#nsRenewalUrl
#nsCaPolicyUrl
#nsSslServerName

# This really needs to be in place for it to be a proxy certificate.
proxyCertInfo=critical,language:id-ppl-anyLanguage,pathlen:3,policy:foo

```