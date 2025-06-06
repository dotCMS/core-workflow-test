/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.cmp;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;

import java.util.Enumeration;

public class KeyRecRepContent
    extends ASN1Encodable
{
    private PKIStatusInfo status;
    private CMPCertificate newSigCert;
    private ASN1Sequence caCerts;
    private ASN1Sequence keyPairHist;

    private KeyRecRepContent(ASN1Sequence seq)
    {
        Enumeration en = seq.getObjects();

        status = PKIStatusInfo.getInstance(en.nextElement());

        while (en.hasMoreElements())
        {
            ASN1TaggedObject tObj = ASN1TaggedObject.getInstance(en.nextElement());

            switch (tObj.getTagNo())
            {
            case 0:
                newSigCert = CMPCertificate.getInstance(tObj.getObject());
                break;
            case 1:
                caCerts = ASN1Sequence.getInstance(tObj.getObject());
                break;
            case 2:
                keyPairHist = ASN1Sequence.getInstance(tObj.getObject());
                break;
            default:
                throw new IllegalArgumentException("unknown tag number: " + tObj.getTagNo());
            }
        }
    }

    public static KeyRecRepContent getInstance(Object o)
    {
        if (o instanceof KeyRecRepContent)
        {
            return (KeyRecRepContent)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new KeyRecRepContent((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }


    public PKIStatusInfo getStatus()
    {
        return status;
    }

    public CMPCertificate getNewSigCert()
    {
        return newSigCert;
    }

    public CMPCertificate[] getCaCerts()
    {
        if (caCerts == null)
        {
            return null;
        }

        CMPCertificate[] results = new CMPCertificate[caCerts.size()];

        for (int i = 0; i != results.length; i++)
        {
            results[i] = CMPCertificate.getInstance(caCerts.getObjectAt(i));
        }

        return results;
    }

    public CertifiedKeyPair[] getKeyPairHist()
    {
        if (keyPairHist == null)
        {
            return null;
        }

        CertifiedKeyPair[] results = new CertifiedKeyPair[keyPairHist.size()];

        for (int i = 0; i != results.length; i++)
        {
            results[i] = CertifiedKeyPair.getInstance(keyPairHist.getObjectAt(i));
        }

        return results;
    }

    /**
     * <pre>
     * KeyRecRepContent ::= SEQUENCE {
     *                         status                  PKIStatusInfo,
     *                         newSigCert          [0] CMPCertificate OPTIONAL,
     *                         caCerts             [1] SEQUENCE SIZE (1..MAX) OF
     *                                                           CMPCertificate OPTIONAL,
     *                         keyPairHist         [2] SEQUENCE SIZE (1..MAX) OF
     *                                                           CertifiedKeyPair OPTIONAL
     *              }
     * </pre> 
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(status);

        addOptional(v, 0, newSigCert);
        addOptional(v, 1, caCerts);
        addOptional(v, 2, keyPairHist);

        return new DERSequence(v);
    }

    private void addOptional(ASN1EncodableVector v, int tagNo, ASN1Encodable obj)
    {
        if (obj != null)
        {
            v.add(new DERTaggedObject(true, tagNo, obj));
        }
    }
}
