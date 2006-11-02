package org.bouncycastle.x509;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.x509.CertificatePair;
import org.bouncycastle.asn1.x509.X509CertificateStructure;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

/**
 * This class contains a cross certificate pair. Cross certificates pairs may
 * contain two cross signed certificates from two CAs. A certificate from the
 * other CA to this CA is contained in the forward certificate, the certificate
 * from this CA to the other CA is contained in the reverse certificate.
 */
public class X509CrossCertificatePair
{
    private X509Certificate forward;
    private X509Certificate reverse;

    /**
     * Constructor.
     *
     * @param forward Certificate from the other CA to this CA.
     * @param reverse Certificate from this CA to the other CA.
     */
    public X509CrossCertificatePair(
        X509Certificate forward,
        X509Certificate reverse)
    {
        this.forward = forward;
        this.reverse = reverse;
    }

    public byte[] getEncoded()
        throws CertificateEncodingException
    {
        X509CertificateStructure f = null;
        X509CertificateStructure r = null;
        try
        {
            if (forward != null)
            {
                f = X509CertificateStructure.getInstance(new ASN1InputStream(
                    forward.getEncoded()).readObject());
            }
            if (reverse != null)
            {
                r = X509CertificateStructure.getInstance(new ASN1InputStream(
                    reverse.getEncoded()).readObject());
            }
            return new CertificatePair(f, r).getDEREncoded();
        }
        catch (IOException e)
        {
            throw new ExtCertificateEncodingException(e.toString(), e);
        }
    }

    /**
     * Returns the certificate from the other CA to this CA.
     *
     * @return Returns the forward certificate.
     */
    public X509Certificate getForward()
    {
        return forward;
    }

    /**
     * Return the certificate from this CA to the other CA.
     *
     * @return Returns the reverse certificate.
     */
    public X509Certificate getReverse()
    {
        return reverse;
    }

    public boolean equals(Object o)
    {
        if (o == null)
        {
            return false;
        }
        if (!(o instanceof X509CrossCertificatePair))
        {
            return false;
        }
        X509CrossCertificatePair pair = (X509CrossCertificatePair)o;
        boolean equalReverse = true;
        boolean equalForward = true;
        if (forward != null)
        {
            equalForward = this.forward.equals(pair.forward);
        }
        else
        {
            if (pair.forward != null)
            {
                equalForward = false;
            }
        }
        if (reverse != null)
        {
            equalReverse = this.reverse.equals(pair.reverse);
        }
        else
        {
            if (pair.reverse != null)
            {
                equalReverse = false;
            }
        }
        return equalForward && equalReverse;
    }

    public int hashCode()
    {
        int hash = 0;
        if (forward != null)
        {
            hash += forward.hashCode();
        }
        if (reverse != null)
        {
            hash += reverse.hashCode();
		}

		return hash;
	}

}