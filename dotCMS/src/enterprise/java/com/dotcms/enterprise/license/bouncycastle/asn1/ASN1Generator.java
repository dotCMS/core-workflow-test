/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included 
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1;

import java.io.OutputStream;

public abstract class ASN1Generator
{
    protected OutputStream _out;
    
    public ASN1Generator(OutputStream out)
    {
        _out = out;
    }
    
    public abstract OutputStream getRawOutputStream();
}
