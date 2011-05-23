package info.pppc.base.system.security.ecc;

import info.pppc.base.system.security.IPrivateKey;
import info.pppc.base.system.security.StaticSecurity;

import java.math.BigInteger;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.ECDSASigner;

/**
 * This class implements an elliptic curve cryptography private key, which can be used for creating signatures.
 * The standard ASN1 encoding scheme is used for signatures.
 * @author WA
 */
public class ECCPrivateKey implements IPrivateKey {

	private final ECPrivateKeyParameters privateKey;
	
	/**
	 * Creates a new ECC private key.
	 * @param privateKey The private key parameters which are used for signing. This ECPrivateKeyParameters-object is created using the Bouncycastle library.
	 */
	public ECCPrivateKey(ECPrivateKeyParameters privateKey)
	{
		this.privateKey=privateKey;	
	}

	/*
	 * (non-Javadoc)
	 * @see edu.uni.bonn.snpc.wa.certs.IPrivateKey#createSignature(byte[])
	 */
	public byte[] createSignature(byte[] message)
	{
		SHA1Digest digest=new SHA1Digest();
		ECDSASigner signer=new ECDSASigner();
		ParametersWithRandom signerParameter=new ParametersWithRandom(privateKey, StaticSecurity.getSecureRandom());
		signer.init(true, signerParameter);
		
		//Creating SHA1-Hash of the byte array
		digest.update(message, 0, message.length);
		byte[] sha1Hash=new byte[digest.getDigestSize()];
		digest.doFinal(sha1Hash, 0);
		
		//Signing the SHA1-Hash
		BigInteger[] signature=signer.generateSignature(sha1Hash);
		
		ASN1EncodableVector signatureVector=new ASN1EncodableVector();
		signatureVector.add(new DERInteger(signature[0])); //Adding r to the ASN1Sequence
		signatureVector.add(new DERInteger(signature[1])); //Adding s to the ASN1Sequence
		DERSequence resultingSequence=new DERSequence(signatureVector);
		return(resultingSequence.getDEREncoded());
	}
	
	/*
	 * (non-Javadoc)
	 * @see edu.uni.bonn.snpc.wa.certs.IPrivateKey#decryptMessage(byte[])
	 */
	public byte[] decryptMessage(byte[] message) {
		// Decryption is not supported
		// Use DH/ECDH and AES instead
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj)
	{
		if(obj==this) return true;
		if(obj==null || (!(obj instanceof ECCPrivateKey))) return false;
		ECPrivateKeyParameters compare=((ECCPrivateKey)obj).privateKey;
		if(!privateKey.getD().equals(compare.getD())) return false;
		ECDomainParameters compareParameters=compare.getParameters();
		ECDomainParameters myParams=privateKey.getParameters();
		if((!myParams.getG().equals(compareParameters.getG())) || !myParams.getH().equals(compareParameters.getH()) || !myParams.getN().equals(compareParameters.getN())) return false;
		return true;
	}
	
	/**
	 * Creates the local secret that is used in the first step of the ECDH. This secret is contained in the returned ECDHBasicAgreement.
	 * @return An ECDHBasicAgreement that contains the local secret as private value.
	 */
	public ECDHBasicAgreement createLocalSecret()
	{
		ECDHBasicAgreement agreement=new ECDHBasicAgreement();
		agreement.init(privateKey);
		return agreement;
	}
	
}
