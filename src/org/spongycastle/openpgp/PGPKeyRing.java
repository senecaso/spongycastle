package org.spongycastle.openpgp;

import org.spongycastle.bcpg.BCPGInputStream;
import org.spongycastle.bcpg.Packet;
import org.spongycastle.bcpg.PacketTags;
import org.spongycastle.bcpg.SignaturePacket;
import org.spongycastle.bcpg.TrustPacket;
import org.spongycastle.bcpg.UserAttributePacket;
import org.spongycastle.bcpg.UserIDPacket;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class PGPKeyRing
{
    PGPKeyRing()
    {
    }

    static BCPGInputStream wrap(InputStream in)
    {
        if (in instanceof BCPGInputStream)
        {
            return (BCPGInputStream)in;
        }

        return new BCPGInputStream(in);
    }

    static TrustPacket readOptionalTrustPacket(
        BCPGInputStream pIn)
        throws IOException
    {
        return (pIn.nextPacketTag() == PacketTags.TRUST)
            ?   (TrustPacket) pIn.readPacket()
            :   null;
    }

    static List readSignaturesAndTrust(
        BCPGInputStream pIn)
        throws IOException
    {
        try
        {
            List sigList = new ArrayList();

            while (pIn.nextPacketTag() == PacketTags.SIGNATURE)
            {
                SignaturePacket signaturePacket = (SignaturePacket)pIn.readPacket();
                TrustPacket trustPacket = readOptionalTrustPacket(pIn);

                sigList.add(new PGPSignature(signaturePacket, trustPacket));
            }

            return sigList;
        }
        catch (PGPException e)
        {
            throw new IOException("can't create signature object: " + e.getMessage()
                + ", cause: " + e.getUnderlyingException().toString());
        }
    }

    static void readUserIDs(
        BCPGInputStream pIn,
        List ids,
        List idTrusts,
        List idSigs)
        throws IOException
    {
        while (pIn.nextPacketTag() == PacketTags.USER_ID
            || pIn.nextPacketTag() == PacketTags.USER_ATTRIBUTE)
        {
            Packet obj = pIn.readPacket();
            if (obj instanceof UserIDPacket)
            {
                UserIDPacket id = (UserIDPacket)obj;
                ids.add(id.getID());
            }
            else
            {
                UserAttributePacket user = (UserAttributePacket)obj;
                ids.add(new PGPUserAttributeSubpacketVector(user.getSubpackets()));
            }

            idTrusts.add(readOptionalTrustPacket(pIn));
            idSigs.add(readSignaturesAndTrust(pIn));
        }
    }
}
