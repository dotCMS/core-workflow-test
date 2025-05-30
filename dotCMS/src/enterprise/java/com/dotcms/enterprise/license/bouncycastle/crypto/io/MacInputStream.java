/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.dotcms.enterprise.license.bouncycastle.crypto.Mac;

public class MacInputStream
    extends FilterInputStream
{
    protected Mac mac;

    public MacInputStream(
        InputStream stream,
        Mac         mac)
    {
        super(stream);
        this.mac = mac;
    }

    public int read()
        throws IOException
    {
        int b = in.read();

        if (b >= 0)
        {
            mac.update((byte)b);
        }
        return b;
    }

    public int read(
        byte[] b,
        int off,
        int len)
        throws IOException
    {
        int n = in.read(b, off, len);
        if (n >= 0)
        {
            mac.update(b, off, n);
        }
        return n;
    }

    public Mac getMac()
    {
        return mac;
    }
}
