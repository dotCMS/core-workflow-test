/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1;

import java.io.IOException;

public abstract  class ASN1Object
    extends DERObject
{
    /**
     * Create a base ASN.1 object from a byte stream.
     *
     * @param data the byte stream to parse.
     * @return the base ASN.1 object represented by the byte stream.
     * @exception IOException if there is a problem parsing the data.
     */
    public static ASN1Object fromByteArray(byte[] data)
        throws IOException
    {
        ASN1InputStream aIn = new ASN1InputStream(data);

        return (ASN1Object)aIn.readObject();
    }

    public final boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        
        return (o instanceof DEREncodable) && asn1Equals(((DEREncodable)o).getDERObject());
    }

    public abstract int hashCode();

    abstract void encode(DEROutputStream out) throws IOException;

    abstract boolean asn1Equals(DERObject o);
}
