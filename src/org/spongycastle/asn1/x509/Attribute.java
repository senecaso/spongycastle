package org.spongycastle.asn1.x509;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.ASN1Set;
import org.spongycastle.asn1.DERObject;
import org.spongycastle.asn1.DERObjectIdentifier;
import org.spongycastle.asn1.DERSequence;

public class Attribute
    extends ASN1Encodable
{
    private DERObjectIdentifier attrType;
    private ASN1Set             attrValues;

    /**
     * return an Attribute object from the given object.
     *
     * @param o the object we want converted.
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static Attribute getInstance(
        Object o)
    {
        if (o == null || o instanceof Attribute)
        {
            return (Attribute)o;
        }
        
        if (o instanceof ASN1Sequence)
        {
            return new Attribute((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("unknown object in factory: " + o.getClass().getName());
    }
    
    public Attribute(
        ASN1Sequence seq)
    {
        if (seq.size() != 2)
        {
            throw new IllegalArgumentException("Bad sequence size: " + seq.size());
        }

        attrType = DERObjectIdentifier.getInstance(seq.getObjectAt(0));
        attrValues = ASN1Set.getInstance(seq.getObjectAt(1));
    }

    public Attribute(
        DERObjectIdentifier attrType,
        ASN1Set             attrValues)
    {
        this.attrType = attrType;
        this.attrValues = attrValues;
    }

    public ASN1ObjectIdentifier getAttrType()
    {
        return new ASN1ObjectIdentifier(attrType.getId());
    }

    public ASN1Encodable[] getAttributeValues()
    {
        return attrValues.toArray();
    }

    public ASN1Set getAttrValues()
    {
        return attrValues;
    }

    /** 
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * Attribute ::= SEQUENCE {
     *     attrType OBJECT IDENTIFIER,
     *     attrValues SET OF AttributeValue
     * }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(attrType);
        v.add(attrValues);

        return new DERSequence(v);
    }
}
