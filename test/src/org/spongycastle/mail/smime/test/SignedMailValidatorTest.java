package org.spongycastle.mail.smime.test;

import java.io.InputStream;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.CertPath;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.x509.X509Extensions;
import org.spongycastle.cms.SignerInformation;
import org.spongycastle.cms.test.CMSTestUtil;
import org.spongycastle.i18n.ErrorBundle;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.mail.smime.SMIMESignedGenerator;
import org.spongycastle.mail.smime.validator.SignedMailValidator;
import org.spongycastle.x509.PKIXCertPathReviewer;
import org.spongycastle.x509.extension.X509ExtensionUtil;

public class SignedMailValidatorTest extends TestCase
{
    static String TEST_TRUST_ACHOR = "validator.root.crt";

    public void testShortKey() throws Exception
    {
        String message = "validator.shortKey.eml";
        PKIXParameters params = createDefaultParams();
        SignedMailValidator.ValidationResult result = doTest(message, params);

        assertTrue(result.isValidSignature());
        assertContainsMessage(result.getNotifications(),
                "SignedMailValidator.shortSigningKey",
                "Warning: The signing key is only 512 bits long.");
    }

    public void testKeyUsage() throws Exception
    {
        String message = "validator.keyUsage.eml";
        PKIXParameters params = createDefaultParams();
        SignedMailValidator.ValidationResult result = doTest(message, params);

        assertTrue(result.isVerifiedSignature());
        assertTrue(result.getCertPathReview().isValidCertPath());
        assertFalse(result.isValidSignature());

        assertContainsMessage(
                result.getErrors(),
                "SignedMailValidator.signingNotPermitted",
                "The key usage extension of signer certificate does not permit using the key for email signatures.");
    }

    public void testExtKeyUsage() throws Exception
    {
        String message = "validator.extKeyUsage.eml";
        PKIXParameters params = createDefaultParams();
        SignedMailValidator.ValidationResult result = doTest(message, params);

        assertTrue(result.isVerifiedSignature());
        assertTrue(result.getCertPathReview().isValidCertPath());
        assertFalse(result.isValidSignature());

        assertContainsMessage(
                result.getErrors(),
                "SignedMailValidator.extKeyUsageNotPermitted",
                "The extended key usage extension of the signer certificate does not permit using the key for email signatures.");
    }

    public void testNoEmail() throws Exception
    {
        String message = "validator.noEmail.eml";
        PKIXParameters params = createDefaultParams();
        SignedMailValidator.ValidationResult result = doTest(message, params);

        assertTrue(result.isVerifiedSignature());
        assertTrue(result.getCertPathReview().isValidCertPath());
        assertFalse(result.isValidSignature());
        
        assertContainsMessage(
                result.getErrors(),
                "SignedMailValidator.noEmailInCert",
                "The signer certificate is not usable for email signatures: it contains no email address.");
    }

    public void testNotYetValid() throws Exception
    {
        String message = "validator.notYetValid.eml";
        PKIXParameters params = createDefaultParams();
        SignedMailValidator.ValidationResult result = doTest(message, params);

        assertTrue(result.isVerifiedSignature());
        assertFalse(result.isValidSignature());
        assertContainsMessage(result.getErrors(),
                "SignedMailValidator.certNotYetValid",
                "The message was signed at Aug 28, 2006 3:04:01 PM GMT. But the certificate is not valid before Dec 28, 2006 2:19:31 PM GMT.");
        
        PKIXCertPathReviewer review = result.getCertPathReview();
        assertFalse(review.isValidCertPath());
        assertContainsMessage(
                review.getErrors(0),
                "CertPathReviewer.certificateNotYetValid",
                "Could not validate the certificate. Certificate is not valid until Dec 28, 2006 2:19:31 PM GMT.");
    }
    
    public void testExpired() throws Exception
    {
        String message = "validator.expired.eml";
        PKIXParameters params = createDefaultParams();
        SignedMailValidator.ValidationResult result = doTest(message, params);
        
        assertTrue(result.isVerifiedSignature());
        assertFalse(result.isValidSignature());
        assertContainsMessage(result.getErrors(),
                "SignedMailValidator.certExpired",
                "The message was signed at Sep 1, 2006 9:08:35 AM GMT. But the certificate expired at Sep 1, 2006 8:39:20 AM GMT.");
        
        PKIXCertPathReviewer review = result.getCertPathReview();
        assertFalse(review.isValidCertPath());
        assertContainsMessage(
                review.getErrors(0),
                "CertPathReviewer.certificateExpired",
                "Could not validate the certificate. Certificate expired on Sep 1, 2006 8:39:20 AM GMT.");
    }
    
    public void testRevoked() throws Exception
    {
        String message = "validator.revoked.eml";
        PKIXParameters params = createDefaultParams();
        List crlList = new ArrayList();
        crlList.add(loadCRL("validator.revoked.crl"));
        CertStore crls = CertStore.getInstance("Collection",new CollectionCertStoreParameters(crlList));
        params.addCertStore(crls);
        params.setRevocationEnabled(true);
        
        SignedMailValidator.ValidationResult result = doTest(message, params);
        
        assertTrue(result.isVerifiedSignature());
        assertFalse(result.isValidSignature());
        
        PKIXCertPathReviewer review = result.getCertPathReview();
        assertFalse(review.isValidCertPath());
        assertContainsMessage(
                review.getErrors(0),
                "CertPathReviewer.certRevoked",
                "The certificate was revoked at Sep 1, 2006 9:30:00 AM GMT. Reason: Key Compromise.");
    }
    
    public void testLongValidity() throws Exception
    {
        String message = "validator.longValidity.eml";
        PKIXParameters params = createDefaultParams();
        
        SignedMailValidator.ValidationResult result = doTest(message, params);
        
        assertTrue(result.isVerifiedSignature());
        assertTrue(result.isValidSignature());
        
        assertContainsMessage(result.getNotifications(),
                "SignedMailValidator.longValidity",
                "Warning: The signing certificate has a very long validity period: from Sep 1, 2006 11:00:00 AM GMT until Aug 8, 2106 11:00:00 AM GMT.");
    }

    public void testSelfSignedCert()
        throws Exception
    {
        MimeBodyPart baseMsg = SMIMETestUtil.makeMimeBodyPart("Hello world!\n");
        String signDN = "CN=Eric H. Echidna, E=eric@spongycastle.org, O=Bouncy Castle, C=AU";
        KeyPair signKP = CMSTestUtil.makeKeyPair();
        X509Certificate signCert = CMSTestUtil.makeV1Certificate(signKP, signDN, signKP, signDN);

        // check basic path validation
        Set trustanchors = new HashSet();
        TrustAnchor ta = new TrustAnchor(signCert, null);
        trustanchors.add(ta);

        X509Certificate rootCert = ta.getTrustedCert();

        // init cert stores
        List certStores = new ArrayList();
        List certList = new ArrayList();
        certList.add(rootCert);
        CertStore store = CertStore.getInstance("Collection", new CollectionCertStoreParameters(certList));
        certStores.add(store);

        // first path
        CertPath path = SignedMailValidator.createCertPath(rootCert, trustanchors, certStores);

        assertTrue("path size is not 1", path.getCertificates().size() == 1);

        // check message validation
        certList = new ArrayList();

        certList.add(signCert);

        CertStore certs = CertStore.getInstance("Collection",
				new CollectionCertStoreParameters(certList), BouncyCastleProvider.PROVIDER_NAME);

        SMIMESignedGenerator gen = new SMIMESignedGenerator();

        gen.addSigner(signKP.getPrivate(), signCert, SMIMESignedGenerator.DIGEST_SHA1);
        gen.addCertificatesAndCRLs(certs);

        MimeMultipart signedMsg = gen.generate(baseMsg, BouncyCastleProvider.PROVIDER_NAME);

        Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props, null);

        // read message
        MimeMessage msg = new MimeMessage(session);

        Address fromUser = new InternetAddress("\"Eric H. Echidna\"<eric@spongycastle.org>");
        Address toUser = new InternetAddress("example@spongycastle.org");
         
        msg.setFrom(fromUser);
        msg.setRecipient(Message.RecipientType.TO, toUser);
        msg.setContent(signedMsg, signedMsg.getContentType());

        msg.saveChanges();
        
        PKIXParameters params = new PKIXParameters(trustanchors);
        params.setRevocationEnabled(false);
        
        SignedMailValidator validator = new SignedMailValidator(msg, params);
        SignerInformation signer = (SignerInformation) validator
                .getSignerInformationStore().getSigners().iterator().next();

        SignedMailValidator.ValidationResult res = validator.getValidationResult(signer);

        assertTrue(res.isVerifiedSignature());
        assertTrue(res.isValidSignature());
    }

// TODO: this test needs to be replaced, unfortunately it was working due to a bug in
// trust anchor extension handling
//    public void testCorruptRootStore() throws Exception
//    {
//        String message = "validator.validMail.eml";
//        Set trustanchors = new HashSet();
//        trustanchors.add(getTrustAnchor(TEST_TRUST_ACHOR));
//        trustanchors.add(getTrustAnchor("validator.fakeRoot.crt"));
//        PKIXParameters params = new PKIXParameters(trustanchors);
//        params.setRevocationEnabled(false);
//
//        SignedMailValidator.ValidationResult result = doTest(message, params);
//
//        assertTrue(result.isVerifiedSignature());
//        assertFalse(result.isValidSignature());
//
//        PKIXCertPathReviewer review = result.getCertPathReview();
//
//        assertFalse(review.isValidCertPath());
//        assertContainsMessage(review.getErrors(-1),
//                "CertPathReviewer.conflictingTrustAnchors",
//                "Warning: corrupt trust root store: There are 2 trusted public keys for the CA \"CN=SignedMailValidatorTest Root, C=CH\" - please ensure with CA which is the correct key.");
//    }
    
    public void testCircular() throws Exception
    {
        String message = "circular.eml";
        PKIXParameters params = createDefaultParams();
        SignedMailValidator.ValidationResult result = doTest(message, params);

        assertTrue(result.isVerifiedSignature());
        assertFalse(result.isValidSignature());
        assertFalse(result.getCertPathReview().isValidCertPath());
        assertTrue("cert path size", result.getCertPathReview().getCertPathSize() > 2);
    }
    
    public void testExtendedReviewer() throws Exception
    {
        try
        {
            // Get a Session object with the default properties.
            Properties props = System.getProperties();
            Session session = Session.getDefaultInstance(props, null);

            // read message
            MimeMessage msg = new MimeMessage(session, getClass().getResourceAsStream("validator.shortKey.eml"));

            SignedMailValidator validator = new SignedMailValidator(msg, createDefaultParams(), String.class);
            fail();
        } 
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().startsWith("certPathReviewerClass is not a subclass of"));
        }
        
        // Get a Session object with the default properties.
        Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props, null);

        // read message
        MimeMessage msg = new MimeMessage(session, getClass().getResourceAsStream("validator.shortKey.eml"));

        SignedMailValidator validator = new SignedMailValidator(msg, createDefaultParams(), DummyCertPathReviewer.class);
        SignerInformation sInfo = (SignerInformation) validator.getSignerInformationStore().getSigners().iterator().next();
        SignedMailValidator.ValidationResult result = validator.getValidationResult(sInfo);

        assertTrue(result.isValidSignature());
        assertContainsMessage(result.getNotifications(),
                "SignedMailValidator.shortSigningKey",
                "Warning: The signing key is only 512 bits long.");
    }
    
    public void testCreateCertPath() throws Exception
    {
        // load trust anchor
        Set trustanchors = new HashSet();
        TrustAnchor ta = getTrustAnchor("certpath_root.crt");
        trustanchors.add(ta);
        
        X509Certificate rootCert = ta.getTrustedCert();
        X509Certificate interCert1 = loadCert("certpath_inter1.crt");
        X509Certificate interCert2 = loadCert("certpath_inter2.crt");
        X509Certificate endCert1 = loadCert("certpath_end1.crt");
        X509Certificate endCert2 = loadCert("certpath_end2.crt");
        
        // init cert stores
        List certStores = new ArrayList();
        List certList = new ArrayList();
        certList.add(interCert1);
        certList.add(interCert2);
        CertStore store = CertStore.getInstance("Collection", new CollectionCertStoreParameters(certList));
        certStores.add(store);
        
        // first path
        CertPath path = SignedMailValidator.createCertPath(endCert1, trustanchors, certStores);
        assertTrue("path size is not 3", path.getCertificates().size() == 3);
        assertEquals("different end certificate", path.getCertificates().get(0), endCert1);
        assertEquals("different intermediate certificate", path.getCertificates().get(1), interCert1);
        assertEquals("different root certificate", path.getCertificates().get(2), rootCert);
        
        // second path
        path = SignedMailValidator.createCertPath(endCert2, trustanchors, certStores);
        assertTrue("path size is not 3", path.getCertificates().size() == 3);
        assertEquals("different end certificate", path.getCertificates().get(0), endCert2);
        assertEquals("different intermediate certificate", path.getCertificates().get(1), interCert2);
        assertEquals("different root certificate", path.getCertificates().get(2), rootCert);
    }
    
    private SignedMailValidator.ValidationResult doTest(String message,
            PKIXParameters params) throws Exception
    {
        // Get a Session object with the default properties.
        Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props, null);

        // read message
        MimeMessage msg = new MimeMessage(session, getClass().getResourceAsStream(message));

        SignedMailValidator validator = new SignedMailValidator(msg, params);
        SignerInformation signer = (SignerInformation) validator
                .getSignerInformationStore().getSigners().iterator().next();
        return validator.getValidationResult(signer);
    }

    private void assertContainsMessage(List msgList, String messageId,
            String text) throws Exception
    {
        Iterator it = msgList.iterator();
        boolean found = false;
        while (it.hasNext())
        {
            ErrorBundle message = (ErrorBundle) it.next();
            if (message.getId().equals(messageId))
            {
                found = true;
                assertEquals(text, message.getText(Locale.ENGLISH, TimeZone
                        .getTimeZone("GMT")));
                break;
            }
        }
        assertTrue("Expected message not found!", found);
    }

    private PKIXParameters createDefaultParams() throws Exception
    {
        Set trustanchors = new HashSet();
        trustanchors.add(getTrustAnchor(TEST_TRUST_ACHOR));
        PKIXParameters defParams = new PKIXParameters(trustanchors);
        defParams.setRevocationEnabled(false);

        return defParams;
    }

    private TrustAnchor getTrustAnchor(String trustcert) throws Exception
    {
        X509Certificate cert = loadCert(trustcert);
        if (cert != null)
        {
            byte[] ncBytes = cert
                    .getExtensionValue(X509Extensions.NameConstraints.getId());

            if (ncBytes != null)
            {
                ASN1Encodable extValue = X509ExtensionUtil
                        .fromExtensionValue(ncBytes);
                return new TrustAnchor(cert, extValue.getDEREncoded());
            }
            return new TrustAnchor(cert, null);
        }
        return null;
    }

    private X509Certificate loadCert(String certfile) throws Exception
    {
        X509Certificate cert = null;
        InputStream in = getClass().getResourceAsStream(certfile);

        CertificateFactory cf = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
        cert = (X509Certificate) cf.generateCertificate(in);
        return cert;
    }
    
    private X509CRL loadCRL(String crlfile) throws Exception
    {
        X509CRL crl = null;
        InputStream in = this.getClass().getResourceAsStream(crlfile);
        
        CertificateFactory cf = CertificateFactory.getInstance("x.509", BouncyCastleProvider.PROVIDER_NAME);
        crl = (X509CRL) cf.generateCRL(in);
        return crl;
    }

    public void setUp()
    {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
        {
            Security
                    .addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
        }
    }
    
    public static void main(String[] args) throws Exception
    {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite("SignedMailValidator Tests");

        suite.addTestSuite(SignedMailValidatorTest.class);

        return suite;
    }

}
