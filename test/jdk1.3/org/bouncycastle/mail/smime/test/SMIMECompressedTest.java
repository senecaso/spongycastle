package org.spongycastle.mail.smime.test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyPair;
import org.spongycastle.jce.cert.CertStore;
import org.spongycastle.jce.cert.CollectionCertStoreParameters;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.cms.AttributeTable;
import org.spongycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.spongycastle.asn1.smime.SMIMECapability;
import org.spongycastle.asn1.smime.SMIMECapabilityVector;
import org.spongycastle.cms.SignerInformation;
import org.spongycastle.cms.SignerInformationStore;
import org.spongycastle.cms.test.CMSTestUtil;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.mail.smime.SMIMECompressed;
import org.spongycastle.mail.smime.SMIMECompressedGenerator;
import org.spongycastle.mail.smime.SMIMECompressedParser;
import org.spongycastle.mail.smime.SMIMESigned;
import org.spongycastle.mail.smime.SMIMESignedGenerator;
import org.spongycastle.mail.smime.SMIMEUtil;
import org.spongycastle.util.Arrays;

public class SMIMECompressedTest
    extends TestCase
{
    private static final String COMPRESSED_CONTENT_TYPE = "application/pkcs7-mime; name=\"smime.p7z\"; smime-type=compressed-data";

    boolean DEBUG = true;

    MimeBodyPart    msg;

    String          signDN;
    KeyPair         signKP;
    X509Certificate signCert;

    String          origDN;
    KeyPair         origKP;
    X509Certificate origCert;

    String          reciDN;
    KeyPair         reciKP;
    X509Certificate reciCert;

    KeyPair         dsaSignKP;
    X509Certificate dsaSignCert;

    KeyPair         dsaOrigKP;
    X509Certificate dsaOrigCert;

    /*
     *
     *  INFRASTRUCTURE
     *
     */

    public SMIMECompressedTest(
         String name)
        throws Exception
    {
        super(name);
        
        msg      = SMIMETestUtil.makeMimeBodyPart("Hello world!");

        signDN   = "O=Bouncy Castle, C=AU";
        signKP   = CMSTestUtil.makeKeyPair();
        signCert = CMSTestUtil.makeCertificate(signKP, signDN, signKP, signDN);

        origDN   = "CN=Eric H. Echidna, E=eric@spongycastle.org, O=Bouncy Castle, C=AU";
        origKP   = CMSTestUtil.makeKeyPair();
        origCert = CMSTestUtil.makeCertificate(origKP, origDN, signKP, signDN);
    }

    public static void main(String args[]) 
    {
        junit.textui.TestRunner.run(SMIMECompressedTest.class);
    }

    public static Test suite() 
    {
        return new SMIMETestSetup(new TestSuite(SMIMECompressedTest.class));
    }

    public void testHeaders()
        throws Exception
    {
        SMIMECompressedGenerator    cgen = new SMIMECompressedGenerator();

        MimeBodyPart cbp = cgen.generate(msg, SMIMECompressedGenerator.ZLIB);
        
        assertEquals(COMPRESSED_CONTENT_TYPE, cbp.getHeader("Content-Type")[0]);
        assertEquals("attachment; filename=\"smime.p7z\"", cbp.getHeader("Content-Disposition")[0]);
        assertEquals("S/MIME Compressed Message", cbp.getHeader("Content-Description")[0]);
    }

    public void testBasic()
        throws Exception
    {
        SMIMECompressedGenerator    cgen = new SMIMECompressedGenerator();
        ByteArrayOutputStream       bOut = new ByteArrayOutputStream();
        MimeBodyPart cbp = cgen.generate(msg, SMIMECompressedGenerator.ZLIB);
        
        SMIMECompressed sc = new SMIMECompressed(cbp);
        
        msg.writeTo(bOut);

        assertTrue(Arrays.areEqual(bOut.toByteArray(), sc.getContent()));
    }
    
    public void testParser()
        throws Exception
    {
        SMIMECompressedGenerator    cgen = new SMIMECompressedGenerator();
        ByteArrayOutputStream       bOut1 = new ByteArrayOutputStream();
        ByteArrayOutputStream       bOut2 = new ByteArrayOutputStream();
        MimeBodyPart                cbp = cgen.generate(msg, SMIMECompressedGenerator.ZLIB);
        SMIMECompressedParser       sc = new SMIMECompressedParser(cbp);
        
        msg.writeTo(bOut1);
    
        InputStream in = sc.getContent().getContentStream();
        int ch;
        
        while ((ch = in.read()) >= 0)
        {
            bOut2.write(ch);
        }
        
        assertTrue(Arrays.areEqual(bOut1.toByteArray(), bOut2.toByteArray()));
    }
    
    /*
     * test compressing and uncompressing of a multipart-signed message.
     */
    public void testCompressedSHA1WithRSA()
        throws Exception
    {
        List           certList = new ArrayList();

        certList.add(origCert);
        certList.add(signCert);

        CertStore      certs = CertStore.getInstance("Collection",
				new CollectionCertStoreParameters(certList), BouncyCastleProvider.PROVIDER_NAME);

        ASN1EncodableVector         signedAttrs = new ASN1EncodableVector();
        SMIMECapabilityVector       caps = new SMIMECapabilityVector();

        caps.addCapability(SMIMECapability.dES_EDE3_CBC);
        caps.addCapability(SMIMECapability.rC2_CBC, 128);
        caps.addCapability(SMIMECapability.dES_CBC);

        signedAttrs.add(new SMIMECapabilitiesAttribute(caps));

        SMIMESignedGenerator gen = new SMIMESignedGenerator();

        gen.addSigner(origKP.getPrivate(), origCert, SMIMESignedGenerator.DIGEST_SHA1, new AttributeTable(signedAttrs), null);

        gen.addCertificatesAndCRLs(certs);

        MimeMultipart smp = gen.generate(msg, BouncyCastleProvider.PROVIDER_NAME);

        MimeMessage bp2 = new MimeMessage((Session)null);                          

        bp2.setContent(smp);

        bp2.saveChanges();

        SMIMECompressedGenerator    cgen = new SMIMECompressedGenerator();

        MimeBodyPart cbp = cgen.generate(bp2, SMIMECompressedGenerator.ZLIB);

        SMIMECompressed cm = new SMIMECompressed(cbp);

        MimeMultipart mm = (MimeMultipart)SMIMEUtil.toMimeBodyPart(cm.getContent()).getContent();
        
        SMIMESigned s = new SMIMESigned(mm);

        ByteArrayOutputStream _baos = new ByteArrayOutputStream();
        msg.writeTo(_baos);
        _baos.close();
        byte[] _msgBytes = _baos.toByteArray();
        _baos = new ByteArrayOutputStream();
        s.getContent().writeTo(_baos);
        _baos.close();
        byte[] _resBytes = _baos.toByteArray();
        
        assertEquals(true, Arrays.areEqual(_msgBytes, _resBytes));

        certs = s.getCertificatesAndCRLs("Collection", BouncyCastleProvider.PROVIDER_NAME);

        SignerInformationStore  signers = s.getSignerInfos();
        Collection              c = signers.getSigners();
        Iterator                it = c.iterator();

        while (it.hasNext())
        {
            SignerInformation   signer = (SignerInformation)it.next();
            Collection          certCollection = certs.getCertificates(signer.getSID());

            Iterator            certIt = certCollection.iterator();
            X509Certificate     cert = (X509Certificate)certIt.next();

            assertEquals(true, signer.verify(cert, BouncyCastleProvider.PROVIDER_NAME));
        }
    }
}
