/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.engines;

import com.dotcms.enterprise.license.bouncycastle.crypto.AsymmetricBlockCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.DataLengthException;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithRandom;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.RSAKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.util.BigIntegers;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * this does your basic RSA algorithm with blinding
 */
public class RSABlindedEngine
    implements AsymmetricBlockCipher
{
    private static BigInteger ONE = BigInteger.valueOf(1);

    private RSACoreEngine    core = new RSACoreEngine();
    private RSAKeyParameters key;
    private SecureRandom     random;

    /**
     * initialise the RSA engine.
     *
     * @param forEncryption true if we are encrypting, false otherwise.
     * @param param the necessary RSA key parameters.
     */
    public void init(
        boolean             forEncryption,
        CipherParameters    param)
    {
        core.init(forEncryption, param);

        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom    rParam = (ParametersWithRandom)param;

            key = (RSAKeyParameters)rParam.getParameters();
            random = rParam.getRandom();
        }
        else
        {
            key = (RSAKeyParameters)param;
            random = new SecureRandom();
        }
    }

    /**
     * Return the maximum size for an input block to this engine.
     * For RSA this is always one byte less than the key size on
     * encryption, and the same length as the key size on decryption.
     *
     * @return maximum size for an input block.
     */
    public int getInputBlockSize()
    {
        return core.getInputBlockSize();
    }

    /**
     * Return the maximum size for an output block to this engine.
     * For RSA this is always one byte less than the key size on
     * decryption, and the same length as the key size on encryption.
     *
     * @return maximum size for an output block.
     */
    public int getOutputBlockSize()
    {
        return core.getOutputBlockSize();
    }

    /**
     * Process a single block using the basic RSA algorithm.
     *
     * @param in the input array.
     * @param inOff the offset into the input buffer where the data starts.
     * @param inLen the length of the data to be processed.
     * @return the result of the RSA process.
     * @exception DataLengthException the input block is too large.
     */
    public byte[] processBlock(
        byte[]  in,
        int     inOff,
        int     inLen)
    {
        if (key == null)
        {
            throw new IllegalStateException("RSA engine not initialised");
        }

        BigInteger input = core.convertInput(in, inOff, inLen);

        BigInteger result;
        if (key instanceof RSAPrivateCrtKeyParameters)
        {
            RSAPrivateCrtKeyParameters k = (RSAPrivateCrtKeyParameters)key;

            BigInteger e = k.getPublicExponent();
            if (e != null)   // can't do blinding without a public exponent
            {
                BigInteger m = k.getModulus();
                BigInteger r = BigIntegers.createRandomInRange(ONE, m.subtract(ONE), random);

                BigInteger blindedInput = r.modPow(e, m).multiply(input).mod(m);
                BigInteger blindedResult = core.processBlock(blindedInput);

                BigInteger rInv = r.modInverse(m);
                result = blindedResult.multiply(rInv).mod(m);
            }
            else
            {
                result = core.processBlock(input);
            }
        }
        else
        {
            result = core.processBlock(input);
        }

        return core.convertOutput(result);
    }
}
