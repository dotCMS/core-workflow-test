/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.x509;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEROctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.crypto.Digest;
import com.dotcms.enterprise.license.bouncycastle.crypto.digests.SHA1Digest;

import java.math.BigInteger;
import java.util.Enumeration;

/**
 * The AuthorityKeyIdentifier object.
 * <pre>
 * id-ce-authorityKeyIdentifier OBJECT IDENTIFIER ::=  { id-ce 35 }
 *
 *   AuthorityKeyIdentifier ::= SEQUENCE {
 *      keyIdentifier             [0] IMPLICIT KeyIdentifier           OPTIONAL,
 *      authorityCertIssuer       [1] IMPLICIT GeneralNames            OPTIONAL,
 *      authorityCertSerialNumber [2] IMPLICIT CertificateSerialNumber OPTIONAL  }
 *
 *   KeyIdentifier ::= OCTET STRING
 * </pre>
 *
 */
public class AuthorityKeyIdentifier
    extends ASN1Encodable
{
    ASN1OctetString keyidentifier=null;
    GeneralNames certissuer=null;
    DERInteger certserno=null;

    public static AuthorityKeyIdentifier getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static AuthorityKeyIdentifier getInstance(
        Object  obj)
    {
        if (obj instanceof AuthorityKeyIdentifier)
        {
            return (AuthorityKeyIdentifier)obj;
        }
        if (obj instanceof ASN1Sequence)
        {
            return new AuthorityKeyIdentifier((ASN1Sequence)obj);
        }
        if (obj instanceof X509Extension)
        {
            return getInstance(X509Extension.convertValueToObject((X509Extension)obj));
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public AuthorityKeyIdentifier(
        ASN1Sequence   seq)
    {
        Enumeration     e = seq.getObjects();

        while (e.hasMoreElements())
        {
            ASN1TaggedObject o = DERTaggedObject.getInstance(e.nextElement());

            switch (o.getTagNo())
            {
            case 0:
                this.keyidentifier = ASN1OctetString.getInstance(o, false);
                break;
            case 1:
                this.certissuer = GeneralNames.getInstance(o, false);
                break;
            case 2:
                this.certserno = DERInteger.getInstance(o, false);
                break;
            default:
                throw new IllegalArgumentException("illegal tag");
            }
        }
    }

    /**
     *
     * Calulates the keyidentifier using a SHA1 hash over the BIT STRING
     * from SubjectPublicKeyInfo as defined in RFC2459.
     *
     * Example of making a AuthorityKeyIdentifier:
     * <pre>
     *   SubjectPublicKeyInfo apki = new SubjectPublicKeyInfo((ASN1Sequence)new ASN1InputStream(
     *       publicKey.getEncoded()).readObject());
     *   AuthorityKeyIdentifier aki = new AuthorityKeyIdentifier(apki);
     * </pre>
     *
     **/
    public AuthorityKeyIdentifier(
        SubjectPublicKeyInfo    spki)
    {
        Digest  digest = new SHA1Digest();
        byte[]  resBuf = new byte[digest.getDigestSize()];

        byte[] bytes = spki.getPublicKeyData().getBytes();
        digest.update(bytes, 0, bytes.length);
        digest.doFinal(resBuf, 0);
        this.keyidentifier = new DEROctetString(resBuf);
    }

    /**
     * create an AuthorityKeyIdentifier with the GeneralNames tag and
     * the serial number provided as well.
     */
    public AuthorityKeyIdentifier(
        SubjectPublicKeyInfo    spki,
        GeneralNames            name,
        BigInteger              serialNumber)
    {
        Digest  digest = new SHA1Digest();
        byte[]  resBuf = new byte[digest.getDigestSize()];

        byte[] bytes = spki.getPublicKeyData().getBytes();
        digest.update(bytes, 0, bytes.length);
        digest.doFinal(resBuf, 0);

        this.keyidentifier = new DEROctetString(resBuf);
        this.certissuer = GeneralNames.getInstance(name.toASN1Object());
        this.certserno = new DERInteger(serialNumber);
    }

    /**
     * create an AuthorityKeyIdentifier with the GeneralNames tag and
     * the serial number provided.
     */
    public AuthorityKeyIdentifier(
        GeneralNames            name,
        BigInteger              serialNumber)
    {
        this.keyidentifier = null;
        this.certissuer = GeneralNames.getInstance(name.toASN1Object());
        this.certserno = new DERInteger(serialNumber);
    }

    /**
      * create an AuthorityKeyIdentifier with a precomupted key identifier
      */
     public AuthorityKeyIdentifier(
         byte[]                  keyIdentifier)
     {
         this.keyidentifier = new DEROctetString(keyIdentifier);
         this.certissuer = null;
         this.certserno = null;
     }

    /**
     * create an AuthorityKeyIdentifier with a precomupted key identifier
     * and the GeneralNames tag and the serial number provided as well.
     */
    public AuthorityKeyIdentifier(
        byte[]                  keyIdentifier,
        GeneralNames            name,
        BigInteger              serialNumber)
    {
        this.keyidentifier = new DEROctetString(keyIdentifier);
        this.certissuer = GeneralNames.getInstance(name.toASN1Object());
        this.certserno = new DERInteger(serialNumber);
    }
    
    public byte[] getKeyIdentifier()
    {
        if (keyidentifier != null)
        {
            return keyidentifier.getOctets();
        }

        return null;
    }

    public GeneralNames getAuthorityCertIssuer()
    {
        return certissuer;
    }
    
    public BigInteger getAuthorityCertSerialNumber()
    {
        if (certserno != null)
        {
            return certserno.getValue();
        }
        
        return null;
    }
    
    /**
     * Produce an object suitable for an ASN1OutputStream.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        if (keyidentifier != null)
        {
            v.add(new DERTaggedObject(false, 0, keyidentifier));
        }

        if (certissuer != null)
        {
            v.add(new DERTaggedObject(false, 1, certissuer));
        }

        if (certserno != null)
        {
            v.add(new DERTaggedObject(false, 2, certserno));
        }


        return new DERSequence(v);
    }

    public String toString()
    {
        return ("AuthorityKeyIdentifier: KeyID(" + this.keyidentifier.getOctets() + ")");
    }
}
