package org.bouncycastle.asn1.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.test.SimpleTest;


/**
 * X.690 test example
 */
public class OIDTest
    extends SimpleTest
{
    byte[]    req1 = Hex.decode("0603813403");
    byte[]    req2 = Hex.decode("06082A36FFFFFFDD6311");

    public String getName()
    {
        return "OID";
    }
    
    private void recodeCheck(
        String oid, 
        byte[] enc) 
        throws IOException
    {
        ByteArrayInputStream     bIn = new ByteArrayInputStream(enc);
        ASN1InputStream          aIn = new ASN1InputStream(bIn);

        DERObjectIdentifier      o = new DERObjectIdentifier(oid);
        DERObjectIdentifier      encO = (DERObjectIdentifier)aIn.readObject();
        
        if (!o.equals(encO))
        {
            fail("oid ID didn't match", o, encO);
        }
        
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();
        DEROutputStream          dOut = new DEROutputStream(bOut);

        dOut.writeObject(o);

        byte[]                    bytes = bOut.toByteArray();

        if (bytes.length != enc.length)
        {
            fail("failed length test");
        }

        for (int i = 0; i != enc.length; i++)
        {
            if (bytes[i] != enc[i])
            {
                fail("failed comparison test", new String(Hex.encode(enc)), new String(Hex.encode(bytes)));
            }
        }
    }
    
    private void valueCheck(
        String  oid)
        throws IOException
    {
        DERObjectIdentifier     o = new DERObjectIdentifier(oid);
        ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
        ASN1OutputStream        aOut = new ASN1OutputStream(bOut);
        
        aOut.writeObject(o);
        
        ByteArrayInputStream    bIn = new ByteArrayInputStream(bOut.toByteArray());
        ASN1InputStream         aIn = new ASN1InputStream(bIn);
        
        o = (DERObjectIdentifier)aIn.readObject();
        
        if (!o.getId().equals(oid))
        {
            fail("failed oid check for " + oid);
        }
    }
    
    public void performTest()
        throws IOException
    {
        recodeCheck("2.100.3", req1);
        recodeCheck("1.2.54.34359733987.17", req2);
        
        valueCheck(PKCSObjectIdentifiers.pkcs_9_at_contentType.getId());
        valueCheck("1.1.127.32512.8323072.2130706432.545460846592.139637976727552.35747322042253312.9151314442816847872");
        valueCheck("1.2.123.12345678901.1.1.1");
        valueCheck("2.25.196556539987194312349856245628873852187.1");
    }

    public static void main(
        String[]    args)
    {
        runTest(new OIDTest());
    }
}
