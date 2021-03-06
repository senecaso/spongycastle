package org.spongycastle.cavp.jca;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import org.spongycastle.cavp.test.DigestProcessor;
import org.spongycastle.cavp.test.DigestProcessorFactory;
import org.spongycastle.jce.provider.BouncyCastleProvider;

public class JcaDigestProcessorFactory
    implements DigestProcessorFactory
{
    private final MessageDigest digest;

    public JcaDigestProcessorFactory(String algorithm)
        throws GeneralSecurityException
    {
        this.digest = MessageDigest.getInstance(algorithm, BouncyCastleProvider.PROVIDER_NAME);
    }

    public DigestProcessor getProcessor()
    {
        return new DigestProcessor()
        {
            public void update(byte[] msg)
            {
                digest.update(msg);
            }

            public byte[] digest()
            {
                return digest.digest();
            }
        };
    }
}
