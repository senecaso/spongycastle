package org.spongycastle.asn1.x509;

import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.ASN1TaggedObject;
import org.spongycastle.asn1.DERBitString;
import org.spongycastle.asn1.DEREncodable;
import org.spongycastle.asn1.DERObject;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.asn1.DERUTCTime;

/**
 * PKIX RFC-2459
 *
 * The X.509 v2 CRL syntax is as follows.  For signature calculation,
 * the data that is to be signed is ASN.1 DER encoded.
 *
 * <pre>
 * CertificateList  ::=  SEQUENCE  {
 *      tbsCertList          TBSCertList,
 *      signatureAlgorithm   AlgorithmIdentifier,
 *      signatureValue       BIT STRING  }
 * </pre>
 */
public class CertificateList
    implements DEREncodable
{
    TBSCertList            tbsCertList;
    AlgorithmIdentifier    sigAlgId;
    DERBitString           sig;

    public static CertificateList getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static CertificateList getInstance(
        Object  obj)
    {
        if (obj instanceof CertificateList)
        {
            return (CertificateList)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new CertificateList((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory");
    }

    public CertificateList(
        ASN1Sequence seq)
    {
        if ( seq.getObjectAt(0) instanceof TBSCertList )
        {
            tbsCertList = (TBSCertList)seq.getObjectAt(0);
        }
        else
        {
            tbsCertList = new TBSCertList((ASN1Sequence)seq.getObjectAt(0));
        }

        sigAlgId = AlgorithmIdentifier.getInstance(seq.getObjectAt(1));
        sig = (DERBitString)seq.getObjectAt(2);
    }

    public TBSCertList getTBSCertList()
    {
        return tbsCertList;
    }

    public CRLEntry[] getRevokedCertificates()
    {
        return tbsCertList.getRevokedCertificates();
    }

    public AlgorithmIdentifier getSignatureAlgorithm()
    {
        return sigAlgId;
    }

    public DERBitString getSignature()
    {
        return sig;
    }

    public int getVersion()
    {
        return tbsCertList.getVersion();
    }

    public X509Name getIssuer()
    {
        return tbsCertList.getIssuer();
    }

    public DERUTCTime getThisUpdate()
    {
        return tbsCertList.getThisUpdate();
    }

    public DERUTCTime getNextUpdate()
    {
        return tbsCertList.getNextUpdate();
    }

    public DERObject getDERObject()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(tbsCertList);
        v.add(sigAlgId);
        v.add(sig);

        return new DERSequence(v);
    }
}
