package org.spongycastle.asn1.cmp;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.DERObject;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.asn1.crmf.CertTemplate;
import org.spongycastle.asn1.x509.X509Extensions;

public class RevDetails
    extends ASN1Encodable
{
    private CertTemplate certDetails;
    private X509Extensions crlEntryDetails;

    private RevDetails(ASN1Sequence seq)
    {
        certDetails = CertTemplate.getInstance(seq.getObjectAt(0));
        if  (seq.size() > 1)
        {
            crlEntryDetails = X509Extensions.getInstance(seq.getObjectAt(1));
        }
    }

    public static RevDetails getInstance(Object o)
    {
        if (o instanceof RevDetails)
        {
            return (RevDetails)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new RevDetails((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public RevDetails(CertTemplate certDetails)
    {
        this.certDetails = certDetails;
    }

    public RevDetails(CertTemplate certDetails, X509Extensions crlEntryDetails)
    {
        this.crlEntryDetails = crlEntryDetails;
    }

    public CertTemplate getCertDetails()
    {
        return certDetails;
    }

    public X509Extensions getCrlEntryDetails()
    {
        return crlEntryDetails;
    }

    /**
     * <pre>
     * RevDetails ::= SEQUENCE {
     *                  certDetails         CertTemplate,
     *                   -- allows requester to specify as much as they can about
     *                   -- the cert. for which revocation is requested
     *                   -- (e.g., for cases in which serialNumber is not available)
     *                   crlEntryDetails     Extensions       OPTIONAL
     *                   -- requested crlEntryExtensions
     *             }
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(certDetails);

        if (crlEntryDetails != null)
        {
            v.add(crlEntryDetails);
        }

        return new DERSequence(v);
    }
}
