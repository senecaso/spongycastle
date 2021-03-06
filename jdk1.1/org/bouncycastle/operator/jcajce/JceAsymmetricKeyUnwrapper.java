package org.spongycastle.operator.jcajce;

import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.ProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.jcajce.DefaultJcaJceHelper;
import org.spongycastle.jcajce.NamedJcaJceHelper;
import org.spongycastle.jcajce.ProviderJcaJceHelper;
import org.spongycastle.operator.AsymmetricKeyUnwrapper;
import org.spongycastle.operator.GenericKey;
import org.spongycastle.operator.OperatorException;

public class JceAsymmetricKeyUnwrapper
    extends AsymmetricKeyUnwrapper
{
    private OperatorHelper helper = new OperatorHelper(new DefaultJcaJceHelper());
    private PrivateKey privKey;

    public JceAsymmetricKeyUnwrapper(AlgorithmIdentifier algorithmIdentifier, PrivateKey privKey)
    {
        super(algorithmIdentifier);

        this.privKey = privKey;
    }

    public JceAsymmetricKeyUnwrapper setProvider(Provider provider)
    {
        this.helper = new OperatorHelper(new ProviderJcaJceHelper(provider));

        return this;
    }

    public JceAsymmetricKeyUnwrapper setProvider(String providerName)
    {
        this.helper = new OperatorHelper(new NamedJcaJceHelper(providerName));

        return this;
    }

    public GenericKey generateUnwrappedKey(AlgorithmIdentifier encryptedKeyAlgorithm, byte[] encryptedKey)
        throws OperatorException
    {
        try
        {
            Key sKey = null;

            Cipher keyCipher = helper.createAsymmetricWrapper(this.getAlgorithmIdentifier().getAlgorithm());

            try
            {
                keyCipher.init(Cipher.UNWRAP_MODE, privKey);
                sKey = keyCipher.unwrap(encryptedKey, encryptedKeyAlgorithm.getAlgorithm().getId(), Cipher.SECRET_KEY);
            }
            catch (InvalidKeyException e)
            {
            }
            catch (NoSuchAlgorithmException e)
            {
            }
            catch (IllegalStateException e)
            {
            }
            catch (UnsupportedOperationException e)
            {
            }
            catch (ProviderException e)
            {
            }

            // some providers do not support UNWRAP (this appears to be only for asymmetric algorithms)
            if (sKey == null)
            {
                keyCipher.init(Cipher.DECRYPT_MODE, privKey);
                sKey = new SecretKeySpec(keyCipher.doFinal(encryptedKey), encryptedKeyAlgorithm.getAlgorithm().getId());
            }

            return new GenericKey(sKey);
        }
        catch (InvalidKeyException e)
        {
            throw new OperatorException("key invalid: " + e.getMessage(), e);
        }
        catch (IllegalBlockSizeException e)
        {
            throw new OperatorException("illegal blocksize: " + e.getMessage(), e);
        }
        catch (BadPaddingException e)
        {
            throw new OperatorException("bad padding: " + e.getMessage(), e);
        }
    }
}
