/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.pkcs;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Set;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;

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
        attrType = (DERObjectIdentifier)seq.getObjectAt(0);
        attrValues = (ASN1Set)seq.getObjectAt(1);
    }

    public Attribute(
        DERObjectIdentifier attrType,
        ASN1Set             attrValues)
    {
        this.attrType = attrType;
        this.attrValues = attrValues;
    }

    public DERObjectIdentifier getAttrType()
    {
        return attrType;
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
